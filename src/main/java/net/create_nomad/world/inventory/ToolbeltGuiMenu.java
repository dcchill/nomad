package net.create_nomad.world.inventory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.create_nomad.init.CreateNomadModMenus;
import net.create_nomad.item.ToolbeltItem;
import net.create_nomad.util.ToolbeltDataUtils;
import net.create_nomad.util.ToolbeltInventoryRules;

public class ToolbeltGuiMenu extends AbstractContainerMenu implements CreateNomadModMenus.MenuAccessor {
	private static final byte MAIN_HAND_SOURCE = 0;
	private static final byte OFF_HAND_SOURCE = 1;
	private static final byte EQUIPPED_SOURCE = 2;

	public final Map<String, Object> menuState = new HashMap<>() {
		@Override
		public Object put(String key, Object value) {
			if (!this.containsKey(key) && this.size() >= 4) {
				return null;
			}
			return super.put(key, value);
		}
	};

	public final Level world;
	public final Player entity;
	public int x;
	public int y;
	public int z;

	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private final ItemStackHandler internal;

	private byte source = EQUIPPED_SOURCE;
	private ItemStack boundStack = ItemStack.EMPTY;
	private int boundPlayerSlot = -1;

	public ToolbeltGuiMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		super(CreateNomadModMenus.TOOLBELT_GUI.get(), id);
		this.entity = inv.player;
		this.world = inv.player.level();
		this.internal = new ItemStackHandler(ToolbeltDataUtils.SLOT_COUNT);

		// Read extra data
		if (extraData != null) {
			var pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			if (extraData.readableBytes() >= 1) {
				this.source = extraData.readByte();
			}
		}

		refreshBoundStack();
		loadFromItem();

		// Toolbelt slots
		for (int slot = 0; slot < ToolbeltDataUtils.SLOT_COUNT; slot++) {
			final int index = slot;
			this.customSlots.put(slot, this.addSlot(new SlotItemHandler(internal, slot, 61 + slot * 18, 35) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return ToolbeltInventoryRules.canStoreInToolbelt(stack);
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}

				@Override
				public void setChanged() {
					super.setChanged();
					ToolbeltDataUtils.sanitizeHandler(ToolbeltGuiMenu.this.internal);

					refreshBoundStack();

					if (!boundStack.isEmpty()
							&& !ToolbeltInventoryRules.canStoreInToolbelt(getItem())
							&& ToolbeltDataUtils.getSelectedSlot(boundStack) == index) {
						ToolbeltDataUtils.setSelectedSlot(boundStack, 0);
					}

					saveToItem();
				}
			}));
		}

		// Player inventory (3 rows)
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(createPlayerSlot(inv, col + (row + 1) * 9, 8 + col * 18, 84 + row * 18));
			}
		}

		// Hotbar
		for (int col = 0; col < 9; ++col) {
			this.addSlot(createPlayerSlot(inv, col, 8 + col * 18, 142));
		}
	}

	private void loadFromItem() {
		if (boundStack.isEmpty() || !(world instanceof ServerLevel serverLevel)) {
			return;
		}

		ItemStackHandler loaded = ToolbeltDataUtils.loadHandler(boundStack, serverLevel.registryAccess());
		for (int slot = 0; slot < internal.getSlots(); slot++) {
			internal.setStackInSlot(slot, loaded.getStackInSlot(slot));
		}
	}

	private void refreshBoundStack() {
		if (source == MAIN_HAND_SOURCE) {
			boundStack = entity.getMainHandItem();
		} else if (source == OFF_HAND_SOURCE) {
			boundStack = entity.getOffhandItem();
		} else {
			boundStack = ToolbeltItem.findEquippedToolbelt(entity);
		}
		resolveBoundPlayerSlot();
	}

	private void resolveBoundPlayerSlot() {
		boundPlayerSlot = -1;
		if (boundStack.isEmpty()) return;

		for (int slot = 0; slot < entity.getInventory().items.size(); slot++) {
			if (entity.getInventory().items.get(slot) == boundStack) {
				boundPlayerSlot = slot;
				return;
			}
		}
	}

	private Slot createPlayerSlot(Inventory inventory, int slotIndex, int xPos, int yPos) {
		return new Slot(inventory, slotIndex, xPos, yPos) {
			@Override
			public boolean mayPickup(Player player) {
				return getContainerSlot() != boundPlayerSlot && super.mayPickup(player);
			}
		};
	}

	private void saveToItem() {
		refreshBoundStack();
		if (boundStack.isEmpty() || !(world instanceof ServerLevel serverLevel)) {
			return;
		}
		ToolbeltDataUtils.saveHandler(boundStack, internal, serverLevel.registryAccess());
	}

	@Override
	public boolean stillValid(Player player) {
		refreshBoundStack();
		return !boundStack.isEmpty();
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);

		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();

			if (index < ToolbeltDataUtils.SLOT_COUNT) {
				if (!this.moveItemStackTo(itemstack1, ToolbeltDataUtils.SLOT_COUNT, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!ToolbeltInventoryRules.canStoreInToolbelt(itemstack1)
					|| !this.moveItemStackTo(itemstack1, 0, ToolbeltDataUtils.SLOT_COUNT, false)) {
				return ItemStack.EMPTY;
			}

			if (itemstack1.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}

		return itemstack;
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
		refreshBoundStack();

		if (slotId >= 0) {
			Slot slot = this.slots.get(slotId);
			if (slot.container == player.getInventory() && slot.getContainerSlot() == boundPlayerSlot) {
				return;
			}
		}

		super.clicked(slotId, dragType, clickType, player);
	}

	@Override
	public void removed(Player playerIn) {
		super.removed(playerIn);
		saveToItem();

		if (playerIn instanceof ServerPlayer serverPlayer
				&& (!serverPlayer.isAlive() || serverPlayer.hasDisconnected())) {
			for (int slot = 0; slot < internal.getSlots(); ++slot) {
				if (internal instanceof IItemHandlerModifiable modifiable) {
					modifiable.setStackInSlot(slot, ItemStack.EMPTY);
				}
			}
		}
	}

	@Override
	public Map<Integer, Slot> getSlots() {
		return Collections.unmodifiableMap(customSlots);
	}

	@Override
	public Map<String, Object> getMenuState() {
		return menuState;
	}
}