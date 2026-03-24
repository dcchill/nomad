package net.create_nomad.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.component.DataComponents;
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
		if (previewStack.isEmpty()) {
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
			return;
		}
		CompoundTag schematicData = previewStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

		guiGraphics.drawString(this.font, Component.literal("Schematic Structure"), 78, 52, -12829636, false);
		renderSchematicNbtLines(guiGraphics, schematicData, 78, 64, -12829636);
	}

	private void renderSchematicNbtLines(GuiGraphics guiGraphics, CompoundTag nbt, int x, int y, int color) {
		if (nbt.isEmpty()) {
			guiGraphics.drawString(this.font, Component.literal("No schematic data"), x, y, color, false);
			return;
		}

		String sizeLine = this.extractSizeLine(nbt);
		if (sizeLine != null)
			guiGraphics.drawString(this.font, Component.literal(sizeLine), x, y, color, false);

		String sourceLine = this.extractSourceLine(nbt);
		if (sourceLine != null)
			guiGraphics.drawString(this.font, Component.literal(sourceLine), x, y + 10, color, false);

		String blocksLine = this.extractBlocksLine(nbt);
		if (blocksLine != null)
			guiGraphics.drawString(this.font, Component.literal(blocksLine), x, y + 20, color, false);
	}

	private String extractSizeLine(CompoundTag nbt) {
		int width = resolveInt(nbt, "width", "Width", "sizeX", "SizeX");
		int height = resolveInt(nbt, "height", "Height", "sizeY", "SizeY");
		int depth = resolveInt(nbt, "length", "Length", "sizeZ", "SizeZ", "depth", "Depth");
		if (width > 0 && height > 0 && depth > 0)
			return "Size: " + width + "x" + height + "x" + depth;
		return null;
	}

	private String extractSourceLine(CompoundTag nbt) {
		for (String key : new String[] {"File", "file", "Schematic", "schematic", "Name", "name"}) {
			if (nbt.contains(key)) {
				String value = nbt.getString(key);
				if (!value.isBlank())
					return "Source: " + value;
			}
		}
		return null;
	}

	private String extractBlocksLine(CompoundTag nbt) {
		for (String key : new String[] {"blocks", "Blocks", "palette", "Palette"}) {
			if (nbt.contains(key) && nbt.get(key) instanceof ListTag listTag)
				return "Entries: " + listTag.size();
		}
		return null;
	}

	private int resolveInt(CompoundTag tag, String... keys) {
		for (String key : keys) {
			if (tag.contains(key))
				return tag.getInt(key);
		}
		return 0;
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
