package net.create_nomad.item;

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
import net.create_nomad.world.inventory.ToolbeltMenu;

public class ToolbeltItem extends Item {
	public ToolbeltItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResultHolder.pass(stack);
		}
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			int handSlotIndex = player.getInventory().selected;
			serverPlayer.openMenu(new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.translatable("item.create_nomad.toolbelt");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player openingPlayer) {
					return new ToolbeltMenu(id, inventory, handSlotIndex);
				}
			});
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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
