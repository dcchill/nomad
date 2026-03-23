package net.create_nomad.util;

import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import net.create_nomad.item.ToolbeltItem;

public final class ToolbeltDataUtils {
    public static final int SLOT_COUNT = 3;
    private static final String INVENTORY_KEY = "inventory";
    private static final String SELECTED_SLOT_KEY = "gearbound_toolbelt_selected_slot";
    private static final String ACTIVE_TOOLBELT_KEY = "gearbound_toolbelt_active";
    private static final String RESTORE_SLOT_KEY = "gearbound_toolbelt_restore_slot";
    private static final String RESTORE_STACK_KEY = "gearbound_toolbelt_restore_stack";

    private ToolbeltDataUtils() {
    }

    public static ItemStackHandler loadHandler(ItemStack stack, HolderLookup.Provider lookupProvider) {
        ItemStackHandler handler = new ItemStackHandler(SLOT_COUNT);
        CompoundTag customTag = getCustomTag(stack);
        if (customTag.contains(INVENTORY_KEY, Tag.TAG_COMPOUND)) {
            handler.deserializeNBT(lookupProvider, customTag.getCompound(INVENTORY_KEY));
        }
        sanitizeHandler(handler);
        return handler;
    }

    public static void saveHandler(ItemStack stack, ItemStackHandler handler, HolderLookup.Provider lookupProvider) {
        sanitizeHandler(handler);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(INVENTORY_KEY, handler.serializeNBT(lookupProvider)));
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

    public static ItemStack getSelectedStack(ItemStack stack, HolderLookup.Provider lookupProvider) {
        ItemStackHandler handler = loadHandler(stack, lookupProvider);
        return handler.getStackInSlot(getSelectedSlot(stack));
    }

    public static void setSelectedStack(ItemStack stack, ItemStack selectedStack, HolderLookup.Provider lookupProvider) {
        ItemStackHandler handler = loadHandler(stack, lookupProvider);
        handler.setStackInSlot(getSelectedSlot(stack), selectedStack);
        saveHandler(stack, handler, lookupProvider);
    }

    public static boolean activateSelectedTool(Player player) {
        HolderLookup.Provider registryAccess = player.level().registryAccess();
        Inventory inventory = player.getInventory();
        int restoreSlot = inventory.selected;
        ItemStack toolbelt = ToolbeltItem.findEquippedToolbelt(player);
        if (toolbelt.isEmpty()) {
            return false;
        }

        if (isToolSelected(player)) {
            deactivateSelectedTool(player);
            toolbelt = ToolbeltItem.findEquippedToolbelt(player);
            if (toolbelt.isEmpty()) {
                return false;
            }
        }

        CompoundTag persistentData = player.getPersistentData();
        persistentData.putBoolean(ACTIVE_TOOLBELT_KEY, true);
        persistentData.putInt(RESTORE_SLOT_KEY, restoreSlot);
        persistentData.put(RESTORE_STACK_KEY, inventory.getItem(restoreSlot).saveOptional(registryAccess));

        ItemStack selectedTool = getSelectedStack(toolbelt, registryAccess);
        setSelectedStack(toolbelt, ItemStack.EMPTY, registryAccess);
        inventory.setItem(restoreSlot, selectedTool.copy());
        inventory.setChanged();
        return true;
    }

    public static boolean deactivateSelectedTool(Player player) {
        if (!isToolSelected(player)) {
            clearActiveSelection(player);
            return false;
        }

        HolderLookup.Provider registryAccess = player.level().registryAccess();
        Inventory inventory = player.getInventory();
        CompoundTag persistentData = player.getPersistentData();
        int restoreSlot = getRestoreSlot(persistentData);
        ItemStack toolbelt = ToolbeltItem.findEquippedToolbelt(player);

        if (!toolbelt.isEmpty()) {
            ItemStack activeTool = inventory.getItem(restoreSlot).copy();
            setSelectedStack(toolbelt, activeTool, registryAccess);
        }

        inventory.setItem(restoreSlot, loadStoredStack(persistentData, registryAccess));
        inventory.setChanged();
        clearActiveSelection(player);
        return true;
    }

    public static boolean syncSelectedTool(Player player, int selectedSlot) {
        HolderLookup.Provider registryAccess = player.level().registryAccess();
        ItemStack toolbelt = ToolbeltItem.findEquippedToolbelt(player);
        if (toolbelt.isEmpty()) {
            return false;
        }

        int previousSlot = getSelectedSlot(toolbelt);
        int clampedSelectedSlot = Math.floorMod(selectedSlot, SLOT_COUNT);
        setSelectedSlot(toolbelt, clampedSelectedSlot);
        if (!isToolSelected(player)) {
            return true;
        }

        Inventory inventory = player.getInventory();
        int restoreSlot = getRestoreSlot(player.getPersistentData());
        ItemStack currentMainHand = inventory.getItem(restoreSlot).copy();

        ItemStackHandler handler = loadHandler(toolbelt, registryAccess);
        handler.setStackInSlot(previousSlot, currentMainHand);
        ItemStack nextTool = handler.getStackInSlot(clampedSelectedSlot).copy();
        handler.setStackInSlot(clampedSelectedSlot, ItemStack.EMPTY);
        saveHandler(toolbelt, handler, registryAccess);

        inventory.setItem(restoreSlot, nextTool);
        inventory.setChanged();
        return true;
    }

    public static boolean isToolSelected(Player player) {
        return player.getPersistentData().getBoolean(ACTIVE_TOOLBELT_KEY);
    }

    public static void sanitizeHandler(ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stored = handler.getStackInSlot(slot);
            if (!stored.isEmpty() && !ToolbeltInventoryRules.canStoreInToolbelt(stored)) {
                handler.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private static int getRestoreSlot(CompoundTag persistentData) {
        int restoreSlot = persistentData.getInt(RESTORE_SLOT_KEY);
        return restoreSlot >= 0 && restoreSlot < Inventory.getSelectionSize() ? restoreSlot : 0;
    }

    private static ItemStack loadStoredStack(CompoundTag persistentData, HolderLookup.Provider registryAccess) {
        if (!persistentData.contains(RESTORE_STACK_KEY, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(registryAccess, persistentData.getCompound(RESTORE_STACK_KEY));
    }

    private static void clearActiveSelection(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        persistentData.remove(ACTIVE_TOOLBELT_KEY);
        persistentData.remove(RESTORE_SLOT_KEY);
        persistentData.remove(RESTORE_STACK_KEY);
    }

    private static CompoundTag getCustomTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }
}
