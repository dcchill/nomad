package net.create_nomad.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class BackpackItemAssociations {
	private BackpackItemAssociations() {
	}

	public static boolean isBackpackItem(ItemStack stack) {
		String itemPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		return itemPath.endsWith("_brass_backpack") || itemPath.endsWith("_brass_backpack_item");
	}
}
