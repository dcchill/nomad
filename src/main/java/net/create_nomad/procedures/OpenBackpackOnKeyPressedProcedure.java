package net.create_nomad.procedures;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.items.IItemHandler;

import net.create_nomad.GearboundMod;
import net.create_nomad.util.BackpackItemAssociations;
import net.create_nomad.world.inventory.BrassBackpackGUIMenu;

public class OpenBackpackOnKeyPressedProcedure {
	public static void execute(ServerPlayer serverPlayer) {
		ItemStack stack = findFirstCuriosBackpack(serverPlayer);

		if (stack.isEmpty()) {
			return;
		}

		serverPlayer.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return stack.getDisplayName();
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
				FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
				buf.writeBlockPos(player.blockPosition());
				buf.writeByte(2);
				return new BrassBackpackGUIMenu(id, inventory, buf);
			}
		});
	}

	private static ItemStack findFirstCuriosBackpack(Player player) {
		IItemHandler curiosInventory = GearboundMod.CuriosApiHelper.getCuriosInventory(player);

		if (curiosInventory == null) {
			return ItemStack.EMPTY;
		}

		for (int i = 0; i < curiosInventory.getSlots(); i++) {
			ItemStack stack = curiosInventory.getStackInSlot(i);
			if (BackpackItemAssociations.isBackpackItem(stack)) {
				return stack;
			}
		}

		return ItemStack.EMPTY;
	}
}
