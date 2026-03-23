package net.create_nomad.world.inventory;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.create_nomad.init.CreateNomadModMenus;
import net.create_nomad.util.ToolbeltDataUtils;
import net.create_nomad.util.ToolbeltInventoryRules;

public class ToolbeltMenu extends AbstractContainerMenu {
    private final Player player;
    private final int handSlotIndex;
    private final ItemStack boundStack;
    private final ItemStackHandler internal;

    public ToolbeltMenu(int id, Inventory inventory, int handSlotIndex) {
        super(CreateNomadModMenus.TOOLBELT_MENU.get(), id);
        this.player = inventory.player;
        this.handSlotIndex = handSlotIndex;
        this.boundStack = inventory.getItem(handSlotIndex);
        this.internal = ToolbeltDataUtils.loadHandler(boundStack);

        for (int slot = 0; slot < ToolbeltDataUtils.SLOT_COUNT; slot++) {
            final int index = slot;
            this.addSlot(new SlotItemHandler(internal, slot, 62 + slot * 18, 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return ToolbeltInventoryRules.canStoreInToolbelt(stack);
                }

                @Override
                public boolean isActive() {
                    return true;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    if (!boundStack.isEmpty()) {
                        ToolbeltDataUtils.saveHandler(boundStack, internal);
                        if (ToolbeltDataUtils.getSelectedSlot(boundStack) == index && !ToolbeltInventoryRules.canStoreInToolbelt(getItem())) {
                            ToolbeltDataUtils.setSelectedSlot(boundStack, 0);
                        }
                    }
                }
            });
        }

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 55 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 113) {
                @Override
                public boolean mayPickup(Player player) {
                    return getContainerSlot() != ToolbeltMenu.this.handSlotIndex && super.mayPickup(player);
                }
            });
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !boundStack.isEmpty() && player.getInventory().getItem(handSlotIndex) == boundStack;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < ToolbeltDataUtils.SLOT_COUNT) {
                if (!this.moveItemStackTo(itemstack1, ToolbeltDataUtils.SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!ToolbeltInventoryRules.canStoreInToolbelt(itemstack1) || !this.moveItemStackTo(itemstack1, 0, ToolbeltDataUtils.SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
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
        if (slotId >= 0) {
            Slot slot = this.slots.get(slotId);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == handSlotIndex) {
                return;
            }
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!boundStack.isEmpty()) {
            ToolbeltDataUtils.saveHandler(boundStack, internal);
        }
    }
}
