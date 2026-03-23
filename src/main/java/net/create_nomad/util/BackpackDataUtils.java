package net.create_nomad.util;

import net.minecraftforge.items.ItemStackHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class BackpackDataUtils {
	private static final String BACKPACK_ID_KEY = "gearbound_backpack_id";

	private BackpackDataUtils() {
	}

	public static void saveContainerToItem(Container container, ItemStack stack, Level level) {
		if (!(level instanceof ServerLevel serverLevel))
			return;

		ItemStackHandler handler = new ItemStackHandler(container.getContainerSize());
		for (int slot = 0; slot < container.getContainerSize(); slot++)
			handler.setStackInSlot(slot, container.getItem(slot).copy());

		saveHandlerToItem(handler, stack, serverLevel);
	}

	public static void loadContainerFromItem(Container container, ItemStack stack, Level level) {
		if (!(level instanceof ServerLevel serverLevel))
			return;

		ItemStackHandler handler = loadHandlerFromItem(stack, serverLevel, container.getContainerSize());
		for (int slot = 0; slot < container.getContainerSize(); slot++)
			container.setItem(slot, handler.getStackInSlot(slot).copy());
	}

	public static ItemStackHandler loadHandlerFromItem(ItemStack stack, ServerLevel level, int slots) {
		ItemStackHandler handler = new ItemStackHandler(slots);
		UUID id = getOrCreateBackpackId(stack);
		CompoundTag inventoryTag = BackpackInventorySavedData.get(level).getInventory(id);
		handler.deserializeNBT(inventoryTag);
		return handler;
	}

	public static void saveHandlerToItem(ItemStackHandler handler, ItemStack stack, ServerLevel level) {
		UUID id = getOrCreateBackpackId(stack);
		BackpackInventorySavedData.get(level).setInventory(id, handler.serializeNBT());
	}

	private static UUID getOrCreateBackpackId(ItemStack stack) {
		CompoundTag customTag = stack.getOrCreateTag();

		UUID id;
		if (customTag.hasUUID(BACKPACK_ID_KEY)) {
			id = customTag.getUUID(BACKPACK_ID_KEY);
		} else {
			id = UUID.randomUUID();
			customTag.putUUID(BACKPACK_ID_KEY, id);
			stack.setTag(customTag);
		}

		return id;
	}
}
