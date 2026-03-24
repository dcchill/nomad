package net.create_nomad.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import net.create_nomad.init.CreateNomadModScreens;
import net.create_nomad.world.inventory.FilingCabinetGuiMenu;

import com.mojang.blaze3d.systems.RenderSystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilingCabinetGuiScreen extends AbstractContainerScreen<FilingCabinetGuiMenu> implements CreateNomadModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	private String cachedSchematicFile = "";
	private SchematicPreviewData cachedPreview = SchematicPreviewData.empty("Hover a schematic to preview");

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
			this.cachedSchematicFile = "";
			this.cachedPreview = SchematicPreviewData.empty("Hover a schematic to preview");
			return;
		}

		CompoundTag data = previewStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		String schematicFile = data.getString("create:schematic_file");
		if (!schematicFile.equals(this.cachedSchematicFile)) {
			this.cachedSchematicFile = schematicFile;
			this.cachedPreview = loadSchematicPreview(schematicFile);
		}

		renderSchematicPreview(guiGraphics, this.cachedPreview, 78, 52, 90, 74);
	}

	private void renderSchematicPreview(GuiGraphics guiGraphics, SchematicPreviewData preview, int x, int y, int width, int height) {
		guiGraphics.drawString(this.font, Component.literal("Schematic Structure"), x, y, -12829636, false);
		int mapY = y + 10;
		int mapHeight = height - 24;
		guiGraphics.fill(x, mapY, x + width, mapY + mapHeight, 0xFF1F1F1F);

		if (preview.isEmpty()) {
			guiGraphics.drawString(this.font, Component.literal(preview.message()), x + 4, mapY + mapHeight / 2 - 4, 0xFFAAAAAA, false);
			return;
		}

		int[][] colors = preview.topDownColors();
		int w = colors.length;
		int d = colors[0].length;
		int pixel = Math.max(1, Math.min((width - 2) / w, (mapHeight - 2) / d));
		int drawW = w * pixel;
		int drawD = d * pixel;
		int startX = x + (width - drawW) / 2;
		int startY = mapY + (mapHeight - drawD) / 2;

		for (int sx = 0; sx < w; sx++) {
			for (int sz = 0; sz < d; sz++) {
				int color = colors[sx][sz];
				guiGraphics.fill(startX + sx * pixel, startY + sz * pixel, startX + (sx + 1) * pixel, startY + (sz + 1) * pixel, color);
			}
		}

		guiGraphics.drawString(this.font, Component.literal(preview.sizeText()), x, y + height - 10, -12829636, false);
	}

	private SchematicPreviewData loadSchematicPreview(String schematicFileRaw) {
		if (schematicFileRaw == null || schematicFileRaw.isBlank())
			return SchematicPreviewData.empty("Missing create:schematic_file");

		String schematicFile = schematicFileRaw.endsWith(".nbt") ? schematicFileRaw : schematicFileRaw + ".nbt";
		Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("schematics").resolve(schematicFile);
		if (!Files.exists(path))
			return SchematicPreviewData.empty("Not found: " + schematicFile);

		try (InputStream stream = Files.newInputStream(path)) {
			CompoundTag root = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
			ListTag size = root.getList("size", Tag.TAG_INT);
			if (size.size() < 3)
				return SchematicPreviewData.empty("Invalid size in " + schematicFile);

			int width = size.getInt(0);
			int height = size.getInt(1);
			int depth = size.getInt(2);
			if (width <= 0 || depth <= 0)
				return SchematicPreviewData.empty("Empty structure in " + schematicFile);

			ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
			ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
			int[][] topY = new int[width][depth];
			int[][] colors = new int[width][depth];
			for (int sx = 0; sx < width; sx++)
				for (int sz = 0; sz < depth; sz++) {
					topY[sx][sz] = Integer.MIN_VALUE;
					colors[sx][sz] = 0xFF2A2A2A;
				}

			for (int i = 0; i < blocks.size(); i++) {
				CompoundTag blockTag = blocks.getCompound(i);
				ListTag pos = blockTag.getList("pos", Tag.TAG_INT);
				if (pos.size() < 3)
					continue;
				int bx = pos.getInt(0);
				int by = pos.getInt(1);
				int bz = pos.getInt(2);
				if (bx < 0 || bz < 0 || bx >= width || bz >= depth)
					continue;
				if (by < topY[bx][bz])
					continue;
				topY[bx][bz] = by;

				int stateIndex = blockTag.getInt("state");
				String stateName = "minecraft:air";
				if (stateIndex >= 0 && stateIndex < palette.size())
					stateName = palette.getCompound(stateIndex).getString("Name");
				colors[bx][bz] = colorFromStateName(stateName);
			}

			return new SchematicPreviewData(colors, "Size: " + width + "x" + height + "x" + depth, "");
		} catch (IOException exception) {
			return SchematicPreviewData.empty("Failed to read " + schematicFile);
		}
	}

	private int colorFromStateName(String stateName) {
		if (stateName == null || stateName.isBlank() || "minecraft:air".equals(stateName))
			return 0xFF2A2A2A;
		int hash = stateName.hashCode();
		int r = 64 + (hash & 0x7F);
		int g = 64 + ((hash >> 7) & 0x7F);
		int b = 64 + ((hash >> 14) & 0x7F);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
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

	private record SchematicPreviewData(int[][] topDownColors, String sizeText, String message) {
		private static SchematicPreviewData empty(String message) {
			return new SchematicPreviewData(new int[0][0], "", message);
		}

		private boolean isEmpty() {
			return topDownColors.length == 0;
		}
	}
}
