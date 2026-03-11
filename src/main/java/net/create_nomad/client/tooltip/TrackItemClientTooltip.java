package net.create_nomad.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import net.create_nomad.item.tooltip.TrackItemTooltip;

public class TrackItemClientTooltip implements ClientTooltipComponent {
    private static final int SLOT_SIZE = 18;
    private static final int MAX_COLUMNS = 4;

    private final ItemStack trackStack;
    private final int storedCount;
    private final int maxStackSize;
    private final int shownStacks;
    private final int columns;
    private final int rows;

    public TrackItemClientTooltip(TrackItemTooltip tooltip) {
        this.trackStack = tooltip.trackStack();
        this.storedCount = Math.max(0, tooltip.storedCount());
        this.maxStackSize = Math.max(1, this.trackStack.getMaxStackSize());
        this.shownStacks = this.storedCount <= 0 ? 0 : Mth.ceil((float) this.storedCount / this.maxStackSize);
        this.columns = this.shownStacks <= 0 ? 1 : Math.min(MAX_COLUMNS, this.shownStacks);
        this.rows = this.shownStacks <= 0 ? 1 : Mth.ceil((float) this.shownStacks / this.columns);
    }

    @Override
    public int getHeight() {
        return this.rows * SLOT_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        return this.columns * SLOT_SIZE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        if (this.shownStacks <= 0) {
            return;
        }

        int remaining = this.storedCount;
        for (int index = 0; index < this.shownStacks; index++) {
            int col = index % this.columns;
            int row = index / this.columns;
            int drawX = x + (col * SLOT_SIZE);
            int drawY = y + (row * SLOT_SIZE);

            int count = Math.min(remaining, this.maxStackSize);
            remaining -= count;

            ItemStack displayStack = this.trackStack.copy();
            displayStack.setCount(count);

            guiGraphics.renderItem(displayStack, drawX + 1, drawY + 1);
            guiGraphics.renderItemDecorations(font, displayStack, drawX + 1, drawY + 1);
        }
    }
}