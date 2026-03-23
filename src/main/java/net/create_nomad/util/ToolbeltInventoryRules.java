package net.create_nomad.util;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import net.create_nomad.init.CreateNomadModItems;

public final class ToolbeltInventoryRules {
    private ToolbeltInventoryRules() {
    }

    public static boolean canStoreInToolbelt(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() instanceof BlockItem) {
            return false;
        }

        return !stack.is(CreateNomadModItems.TOOLBELT.get());
    }
}
