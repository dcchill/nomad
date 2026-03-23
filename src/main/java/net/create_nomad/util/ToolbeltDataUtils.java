package net.create_nomad.util;

import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ToolbeltDataUtils {
    public static final int SLOT_COUNT = 3;
    private static final String INVENTORY_KEY = "inventory";
    private static final String SELECTED_SLOT_KEY = "gearbound_toolbelt_selected_slot";

    private ToolbeltDataUtils() {
    }

    public static ItemStackHandler loadHandler(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(SLOT_COUNT);
        CompoundTag customTag = getCustomTag(stack);
        if (customTag.contains(INVENTORY_KEY, Tag.TAG_COMPOUND)) {
            handler.deserializeNBT(stack.registryAccess(), customTag.getCompound(INVENTORY_KEY));
        }
        sanitizeHandler(handler);
        return handler;
    }

    public static void saveHandler(ItemStack stack, ItemStackHandler handler) {
        sanitizeHandler(handler);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(INVENTORY_KEY, handler.serializeNBT(stack.registryAccess())));
    }

    public static int getSelectedSlot(ItemStack stack) {
        CompoundTag customTag = getCustomTag(stack);
        int selectedSlot = customTag.getInt(SELECTED_SLOT_KEY);
        if (selectedSlot < 0 || selectedSlot >= SLOT_COUNT) {
            return 0;
        }
        return selectedSlot;
    }

    public static void setSelectedSlot(ItemStack stack, int selectedSlot) {
        int clamped = Math.floorMod(selectedSlot, SLOT_COUNT);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SELECTED_SLOT_KEY, clamped));
    }

    public static ItemStack getSelectedStack(ItemStack stack) {
        ItemStackHandler handler = loadHandler(stack);
        return handler.getStackInSlot(getSelectedSlot(stack));
    }

    public static void setSelectedStack(ItemStack stack, ItemStack selectedStack) {
        ItemStackHandler handler = loadHandler(stack);
        handler.setStackInSlot(getSelectedSlot(stack), selectedStack);
        saveHandler(stack, handler);
    }

    public static void sanitizeHandler(ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stored = handler.getStackInSlot(slot);
            if (!stored.isEmpty() && !ToolbeltInventoryRules.canStoreInToolbelt(stored)) {
                handler.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private static CompoundTag getCustomTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }
}
