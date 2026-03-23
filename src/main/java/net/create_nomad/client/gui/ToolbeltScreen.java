package net.create_nomad.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import net.create_nomad.util.ToolbeltDataUtils;
import net.create_nomad.world.inventory.ToolbeltMenu;

public class ToolbeltScreen extends AbstractContainerScreen<ToolbeltMenu> {
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");

    public ToolbeltScreen(ToolbeltMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 137;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xF0101010);
        guiGraphics.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.imageWidth - 4, this.topPos + 48, 0xCC2E3A4E);
        guiGraphics.fill(this.leftPos + 4, this.topPos + 49, this.leftPos + this.imageWidth - 4, this.topPos + this.imageHeight - 4, 0xCC1A1A1A);
        for (int slot = 0; slot < ToolbeltDataUtils.SLOT_COUNT; slot++) {
            int x = this.leftPos + 61 + slot * 18;
            int y = this.topPos + 17;
            guiGraphics.blitSprite(SLOT_SPRITE, x, y, 18, 18);
        }
        RenderSystem.disableBlend();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xE0E0E0, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xE0E0E0, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.create_nomad.toolbelt_gui.label_tools"), 8, 18, 0xE0E0E0, false);
    }
}
