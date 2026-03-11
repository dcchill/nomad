package net.create_nomad.item.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public record TrackItemTooltip(ItemStack trackStack, int storedCount) implements TooltipComponent {
}