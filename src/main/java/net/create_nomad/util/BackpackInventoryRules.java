package net.create_nomad.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class BackpackInventoryRules {
    private BackpackInventoryRules() {
    }

    public static boolean canStoreInBackpack(ItemStack stack) {
        return !BackpackItemAssociations.isBackpackItem(stack) && !isShulkerBoxItem(stack);
    }

    private static boolean isShulkerBoxItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }
}