package net.create_nomad.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class BackpackInventoryRules {
    public static final int STORAGE_SLOT_COUNT = 36;
    public static final int UPGRADE_SLOT_START = STORAGE_SLOT_COUNT;
    public static final int UPGRADE_SLOT_COUNT = 3;
    public static final int TOTAL_SLOT_COUNT = STORAGE_SLOT_COUNT + UPGRADE_SLOT_COUNT;
    public static final TagKey<Item> BACKPACK_UPGRADES_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("create_nomad", "backpack_upgrades"));

    private BackpackInventoryRules() {
    }

    public static boolean canStoreInBackpack(ItemStack stack) {
        return canStoreInBackpack(stack, false);
    }

    public static boolean canStoreInBackpack(ItemStack stack, boolean allowBackpacks) {
        boolean disallowBackpack = !allowBackpacks && BackpackItemAssociations.isBackpackItem(stack);
        return !disallowBackpack && !isShulkerBoxItem(stack);
    }

    public static boolean isUpgradeSlot(int slot) {
        return slot >= UPGRADE_SLOT_START && slot < TOTAL_SLOT_COUNT;
    }

    public static boolean canPlaceInSlot(int slot, ItemStack stack) {
        if (isUpgradeSlot(slot)) {
            return stack.is(BACKPACK_UPGRADES_TAG);
        }

        return canStoreInBackpack(stack);
    }

    private static boolean isShulkerBoxItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }
}
