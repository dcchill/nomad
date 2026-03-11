package net.create_nomad.client.gui;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import net.create_nomad.world.inventory.BrassBackpackGUIMenu;
import net.create_nomad.init.GearboundModScreens;

import net.create_nomad.init.GearboundModSounds;

import com.mojang.blaze3d.systems.RenderSystem;

public class BrassBackpackGUIScreen extends AbstractContainerScreen<BrassBackpackGUIMenu> implements GearboundModScreens.ScreenAccessor {
	private final Level world;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public BrassBackpackGUIScreen(BrassBackpackGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 188;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		menuStateUpdateActive = false;
	}

	private static final ResourceLocation texture = ResourceLocation.parse("gearbound:textures/screens/brass_backpack_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		guiGraphics.blit(ResourceLocation.parse("gearbound:textures/screens/brass_backpack.png"), this.leftPos + -2, this.topPos + -21, 0, 0, 256, 256, 256, 256);
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
		guiGraphics.drawString(this.font, Component.translatable("gui.gearbound.brass_backpack_gui.label_brass_backpack"), 2, -18, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
		if (this.minecraft != null) {
			this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(GearboundModSounds.BACKPACK_OPEN.get(), 1.0F));
		}
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(GearboundModSounds.BACKPACK_CLOSE.get(), 1.0F));
		}
		super.onClose();
	}
}