package net.create_nomad.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public class ToolUpgradeItem extends Item {
	public ToolUpgradeItem() {
		super(new Item.Properties());
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);

		if (Screen.hasShiftDown()) {
			tooltip.add(Component.literal("Allows using tools directly from the backpack").withStyle(ChatFormatting.GOLD));
			tooltip.add(Component.literal("Tools in the backpack can be used without").withStyle(ChatFormatting.GOLD));
			tooltip.add(Component.literal("moving them to your inventory.").withStyle(ChatFormatting.GOLD));
		} else {
			tooltip.add(Component.translatable("tooltip.create_nomad.shift_for_info",
					Component.translatable("key.keyboard.left.shift").withStyle(ChatFormatting.YELLOW))
					.withStyle(ChatFormatting.GRAY));
		}
	}
}