package net.create_nomad.item;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.items.IItemHandler;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.world.inventory.ToolbeltGuiMenu;

public class ToolbeltItem extends Item {
	private static final byte MAIN_HAND_SOURCE = 0;
	private static final byte OFF_HAND_SOURCE = 1;
	private static final byte EQUIPPED_SOURCE = 2;

	public ToolbeltItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			openToolbelt(serverPlayer, hand == InteractionHand.MAIN_HAND ? MAIN_HAND_SOURCE : OFF_HAND_SOURCE, stack.getDisplayName());
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	public static void openPreferredToolbelt(ServerPlayer serverPlayer) {
		ItemStack mainHand = serverPlayer.getMainHandItem();
		if (mainHand.getItem() instanceof ToolbeltItem) {
			openToolbelt(serverPlayer, MAIN_HAND_SOURCE, mainHand.getDisplayName());
			return;
		}

		ItemStack offHand = serverPlayer.getOffhandItem();
		if (offHand.getItem() instanceof ToolbeltItem) {
			openToolbelt(serverPlayer, OFF_HAND_SOURCE, offHand.getDisplayName());
			return;
		}

		ItemStack equipped = findEquippedToolbelt(serverPlayer);
		if (!equipped.isEmpty()) {
			openToolbelt(serverPlayer, EQUIPPED_SOURCE, equipped.getDisplayName());
		}
	}

	private static void openToolbelt(ServerPlayer serverPlayer, byte source, Component displayName) {
		serverPlayer.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return displayName;
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player openingPlayer) {
				FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
				buf.writeBlockPos(openingPlayer.blockPosition());
				buf.writeByte(source);
				return new ToolbeltGuiMenu(id, inventory, buf);
			}
		});
	}

	public static ItemStack findEquippedToolbelt(Player player) {
		IItemHandler curiosInventory = CreateNomadMod.CuriosApiHelper.getCuriosInventory(player);
		if (curiosInventory != null) {
			for (int i = 0; i < curiosInventory.getSlots(); i++) {
				ItemStack stack = curiosInventory.getStackInSlot(i);
				if (stack.getItem() instanceof ToolbeltItem) {
					return stack;
				}
			}
			return ItemStack.EMPTY;
		}

		for (ItemStack stack : player.getInventory().items) {
			if (stack.getItem() instanceof ToolbeltItem) {
				return stack;
			}
		}

		return ItemStack.EMPTY;
	}
}
