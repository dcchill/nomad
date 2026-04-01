package net.create_nomad.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public class FallUpgradeItem extends Item {
	public FallUpgradeItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);

		if (Screen.hasShiftDown()) {
			tooltip.add(Component.literal("Automatically places water to break your fall").withStyle(ChatFormatting.GOLD));
			tooltip.add(Component.literal("when falling from a dangerous height.").withStyle(ChatFormatting.GOLD));
			tooltip.add(Component.literal("Hold Shift while falling to activate.").withStyle(ChatFormatting.GOLD));
		} else {
			tooltip.add(Component.translatable("tooltip.create_nomad.shift_for_info",
					Component.translatable("key.keyboard.left.shift").withStyle(ChatFormatting.YELLOW))
					.withStyle(ChatFormatting.GRAY));
		}
	}
}