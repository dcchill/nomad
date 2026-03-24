package net.create_nomad.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.create_nomad.init.CreateNomadModScreens;
import net.create_nomad.world.inventory.FilingCabinetGuiMenu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilingCabinetGuiScreen extends AbstractContainerScreen<FilingCabinetGuiMenu> implements CreateNomadModScreens.ScreenAccessor {

	private final Level world;
	private final int x, y, z;
	private final Player entity;

	private static final ResourceLocation texture = ResourceLocation.parse("create_nomad:textures/screens/filing_cabinet_gui.png");

	public FilingCabinetGuiScreen(FilingCabinetGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;

		this.imageWidth = 188;
		this.imageHeight = 182;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {}

	// =========================
	// RENDER
	// =========================

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);

		ItemStack stack = getHoveredCabinetSchematic();

		// render inside texture box
		renderPreviewInBox(guiGraphics, stack, partialTicks);

		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);

		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
	}

	// =========================
	// PREVIEW BOX
	// =========================

	private void renderPreviewInBox(GuiGraphics guiGraphics, ItemStack stack, float partialTicks) {

		int boxX = this.leftPos + 14;
		int boxY = this.topPos + 19;

		int boxW = 72;
		int boxH = 72;

		if (stack.isEmpty()) return;

		render3DSchematic(guiGraphics, stack, boxX, boxY, boxW, boxH, partialTicks);
	}
	//render labels
	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {

    // Filing Cabinet title
    guiGraphics.drawString(
            this.font,
            this.title,
            5,
            4,
            4210752,
            false
    );
	}
	// =========================
	// 3D RENDER (NBT BASED)
	// =========================

		private void render3DSchematic(GuiGraphics guiGraphics, ItemStack stack,
		                              int x, int y, int width, int height, float partialTicks) {
		
		    String file = extractSchematicFile(stack);
		    if (file.isBlank()) return;
		
		    SchematicData data = loadRawSchematic(file);
		    if (data == null) return;
		
		    var mc = Minecraft.getInstance();
		    var pose = guiGraphics.pose();
		
		    // 🔒 CLIP TO BOX (prevents overflow)
		    guiGraphics.enableScissor(x, y, x + width, y + height);
		
		    pose.pushPose();
		
		    // center of box
		    pose.translate(x + width / 2f, y + height / 2f, 300);
		
		    // 🔥 FIXED SCALE (much tighter + consistent)
		    float maxDim = Math.max(data.width, Math.max(data.height, data.depth));
		    float scale = (Math.min(width, height) / maxDim) * 0.6f;
		
		    pose.scale(scale, -scale, scale);
		
		    // 🔥 BETTER CAMERA ANGLE
		    pose.mulPose(Axis.XP.rotationDegrees(25));
		    pose.mulPose(Axis.YP.rotationDegrees((mc.level.getGameTime() * 0.3f % 360)));
		
		    // 🔥 TRUE CENTERING (this was the big issue)
		    pose.translate(
		        -data.width / 2f,
		        -data.height / 2f,
		        -data.depth / 2f
		    );
		
		    renderBlocksFromNBT(data, pose);
		
		    pose.popPose();
		
		    guiGraphics.disableScissor();
		}

	// =========================
	// LOAD NBT
	// =========================

	private SchematicData loadRawSchematic(String fileName) {
		try {
			String name = fileName.endsWith(".nbt") ? fileName : fileName + ".nbt";

			Path path = Minecraft.getInstance().gameDirectory.toPath()
					.resolve("schematics")
					.resolve(name);

			if (!Files.exists(path)) return null;

			CompoundTag root;

			try (InputStream stream = Files.newInputStream(path)) {
				root = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
			} catch (Exception e) {
				try (InputStream stream = Files.newInputStream(path)) {
					root = NbtIo.read(new java.io.DataInputStream(stream), NbtAccounter.unlimitedHeap());
				}
			}

			ListTag size = root.getList("size", Tag.TAG_INT);
			ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
			ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);

			return new SchematicData(size, palette, blocks);

		} catch (Exception e) {
			System.out.println("NBT load fail: " + e.getMessage());
			return null;
		}
	}

	// =========================
	// RENDER BLOCKS
	// =========================

	private void renderBlocksFromNBT(SchematicData data, PoseStack pose) {
		var mc = Minecraft.getInstance();
		var renderer = mc.getBlockRenderer();
		var buffer = mc.renderBuffers().bufferSource();

		for (int i = 0; i < data.blocks.size(); i++) {
			CompoundTag blockTag = data.blocks.getCompound(i);

			ListTag pos = blockTag.getList("pos", Tag.TAG_INT);
			int x = pos.getInt(0);
			int y = pos.getInt(1);
			int z = pos.getInt(2);

			int stateIndex = blockTag.getInt("state");
			CompoundTag stateTag = data.palette.getCompound(stateIndex);

			String blockName = stateTag.getString("Name");

			var block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(blockName));
			if (block == null) continue;

			var state = block.defaultBlockState();
			if (state.isAir()) continue;

			pose.pushPose();
			pose.translate(x, y, z);

			renderer.renderSingleBlock(
					state,
					pose,
					buffer,
					15728880,
					OverlayTexture.NO_OVERLAY
			);

			pose.popPose();
		}

		buffer.endBatch();
	}

	// =========================
	// EXTRACT FILE NAME
	// =========================

	private String extractSchematicFile(ItemStack stack) {
		try {
			var mc = Minecraft.getInstance();
			if (mc.level == null) return "";

			var provider = mc.level.registryAccess();

			CompoundTag full = (CompoundTag) stack.save(provider);

			if (full.contains("components", Tag.TAG_COMPOUND)) {
				CompoundTag comps = full.getCompound("components");

				if (comps.contains("create:schematic_file")) {
					return comps.getString("create:schematic_file");
				}
			}
		} catch (Exception e) {
			System.out.println("NBT read fail: " + e.getMessage());
		}

		return "";
	}

	// =========================
	// UTIL
	// =========================

	private ItemStack getHoveredCabinetSchematic() {
		Slot slot = this.hoveredSlot;

		if (slot == null || !slot.hasItem() || slot.index < 0 || slot.index >= 16)
			return ItemStack.EMPTY;

		return slot.getItem();
	}

	// =========================
	// DATA CLASS
	// =========================

	private static class SchematicData {
		final int width;
		final int height;
		final int depth;

		final ListTag palette;
		final ListTag blocks;

		SchematicData(ListTag size, ListTag palette, ListTag blocks) {
			this.width = size.getInt(0);
			this.height = size.getInt(1);
			this.depth = size.getInt(2);
			this.palette = palette;
			this.blocks = blocks;
		}
	}
}