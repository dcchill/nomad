package net.create_nomad.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import net.create_nomad.world.inventory.FilingCabinetGuiMenu;
import net.create_nomad.init.CreateNomadModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class FilingCabinetGuiScreen extends AbstractContainerScreen<FilingCabinetGuiMenu> implements CreateNomadModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public FilingCabinetGuiScreen(FilingCabinetGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		menuStateUpdateActive = false;
	}

	private static final ResourceLocation texture = ResourceLocation.parse("create_nomad:textures/screens/filing_cabinet_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		ItemStack previewStack = this.getHoveredCabinetSchematic();
		if (!previewStack.isEmpty()) {
			guiGraphics.renderTooltip(this.font, previewStack, mouseX, mouseY);
		} else {
			this.renderTooltip(guiGraphics, mouseX, mouseY);
		}
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		ItemStack previewStack = this.getHoveredCabinetSchematic();
		if (previewStack.isEmpty()) {
			guiGraphics.drawString(this.font, Component.translatable("gui.create_nomad.filing_cabinet_gui.label_schematic_placeholder"), 78, 70, -12829636, false);
			return;
		}

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(116, 48, 0);
		guiGraphics.pose().scale(2.75f, 2.75f, 1f);
		guiGraphics.renderItem(previewStack, 0, 0);
		guiGraphics.pose().popPose();
		guiGraphics.drawString(this.font, previewStack.getHoverName(), 78, 70, -12829636, false);
	}

	private ItemStack getHoveredCabinetSchematic() {
		Slot slot = this.hoveredSlot;
		if (slot == null || !slot.hasItem() || slot.index < 0 || slot.index >= 16)
			return ItemStack.EMPTY;
		return slot.getItem();
	}

	@Override
	public void init() {
		super.init();
	}
}