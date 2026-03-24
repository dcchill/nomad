package net.create_nomad.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.create_nomad.init.CreateNomadModScreens;
import net.create_nomad.world.inventory.FilingCabinetGuiMenu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;


public class FilingCabinetGuiScreen extends AbstractContainerScreen<FilingCabinetGuiMenu> implements CreateNomadModScreens.ScreenAccessor {

    private static final ResourceLocation texture = ResourceLocation.parse("create_nomad:textures/screens/filing_cabinet_gui.png");

    private SchematicData cachedData = null;
    private String cachedFile = "";


	// ANIMATION STATE
		private float buildProgress = 0f;
		private boolean animating = false;
		
    public FilingCabinetGuiScreen(FilingCabinetGuiMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    public void updateMenuState(int elementType, String name, Object elementState) {}

    // REMOVE INVENTORY LABEL
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 5, 5, 4210752, false);
    }

    // BACKGROUND
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1,1,1,1);
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, 256, 256);
    }

    // MAIN RENDER
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        ItemStack stack = getHoveredCabinetSchematic();
        renderPreview(guiGraphics, stack, partialTicks);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // PREVIEW BOX 
    private void renderPreview(GuiGraphics guiGraphics, ItemStack stack, float partialTicks) {
        int x = this.leftPos + 14;
        int y = this.topPos + 20;

        int w = 72;
        int h = 72;

        if (stack.isEmpty()) return;

        render3D(guiGraphics, stack, x, y, w, h, partialTicks);
    }


    // 3D RENDER
    private void render3D(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int w, int h, float partialTicks) {

        String file = extractFile(stack);
        if (file.isBlank()) return;

        if (!file.equals(cachedFile)) {
            cachedFile = file;
            cachedData = load(file);

                buildProgress = 0f;
  				 animating = true;
        }

        if (cachedData == null) return;

        var mc = Minecraft.getInstance();
        var pose = guiGraphics.pose();

        guiGraphics.enableScissor(x, y, x + w, y + h);

        pose.pushPose();

        pose.translate(x + w/2f, y + h/2f, 300);

        float maxDim = Math.max(cachedData.w, Math.max(cachedData.h, cachedData.d));
        float scale = (Math.min(w, h) / maxDim) * 0.75f;

        pose.scale(scale, -scale, scale);

        pose.mulPose(Axis.XP.rotationDegrees(25));
        pose.mulPose(Axis.YP.rotationDegrees((mc.level.getGameTime() + partialTicks) * 0.2f % 360));

        pose.translate(-cachedData.w/2f, -cachedData.h/2f, -cachedData.d/2f);

        renderBlocks(cachedData, pose);

        pose.popPose();

        guiGraphics.disableScissor();
    }

// RENDER BLOCKS
private void renderBlocks(SchematicData data, PoseStack pose) {

    var mc = Minecraft.getInstance();
    var renderer = mc.getBlockRenderer();
    var buffer = mc.renderBuffers().bufferSource();

    int totalBlocks = Math.min(7500, data.size);

    // ✅ update animation ONCE per frame
    if (animating) {
        buildProgress += 0.02f;

        if (buildProgress >= 1f) {
            buildProgress = 1f;
            animating = false;
        }
    }

    int visibleBlocks = (int)(totalBlocks * buildProgress);

    for (int i = 0; i < visibleBlocks; i++) {

        BlockEntry e = data.entries[i];
        if (e == null) continue;

        pose.pushPose();

        pose.translate(e.x, e.y, e.z);

        float blockProgress = (float)e.y / data.h;
        float diff = buildProgress - blockProgress;

        float scale = 1f;

        if (diff < 0.1f) {
            float t = Math.max(0f, diff / 0.1f);
            t = 1f - (1f - t) * (1f - t);
            scale = 0.2f + 0.8f * t;
        }

        pose.translate(0.5, 0.5, 0.5);
        pose.scale(scale, scale, scale);
        pose.translate(-0.5, -0.5, -0.5);

        renderer.renderSingleBlock(
            e.state,
            pose,
            buffer,
            15728880,
            OverlayTexture.NO_OVERLAY
        );

        pose.popPose();
    }

    buffer.endBatch();
}

    // DATA STRUCTURES
    private static class BlockEntry {
        final int x,y,z;
        final net.minecraft.world.level.block.state.BlockState state;
        BlockEntry(int x,int y,int z, net.minecraft.world.level.block.state.BlockState s){this.x=x;this.y=y;this.z=z;this.state=s;}
    }

    private static class SchematicData {
        final int w,h,d;
        final BlockEntry[] entries;
        final int size;
        SchematicData(int w,int h,int d,BlockEntry[] e,int s){this.w=w;this.h=h;this.d=d;this.entries=e;this.size=s;}
    }


    // LOAD
		private SchematicData load(String file) {
		    try {
		        Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("schematics").resolve(file);
		        if (!Files.exists(path)) return null;
		
		        CompoundTag root;
		        try (InputStream stream = Files.newInputStream(path)) {
		            root = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
		        }
		
		        ListTag size = root.getList("size", Tag.TAG_INT);
		        ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
		        ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
		
		        int w = size.getInt(0), h = size.getInt(1), d = size.getInt(2);
		
		        HashSet<Long> blockSet = new HashSet<>();
		
		        // pass 1: store positions
		        for (int i = 0; i < blocks.size(); i++) {
		            ListTag pos = blocks.getCompound(i).getList("pos", Tag.TAG_INT);
		            blockSet.add(BlockPos.asLong(pos.getInt(0), pos.getInt(1), pos.getInt(2)));
		        }
		
		        ArrayList<BlockEntry> visible = new ArrayList<>();
		
		        // pass 2: build surface blocks
		        for (int i = 0; i < blocks.size(); i++) {
		
		            CompoundTag b = blocks.getCompound(i);
		            ListTag pos = b.getList("pos", Tag.TAG_INT);
		
		            int x = pos.getInt(0);
		            int y = pos.getInt(1);
		            int z = pos.getInt(2);
		
		            CompoundTag stateTag = palette.getCompound(b.getInt("state"));
		            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(stateTag.getString("Name")));
		            if (block == null) continue;
		
		            var state = block.defaultBlockState();
		            if (state.isAir()) continue;
		
		            if (isSurface(blockSet, x, y, z)) {
		                visible.add(new BlockEntry(x, y, z, state));
		            }
		        }
		
		        visible.sort((a, b) -> {
		            if (a.y != b.y) return Integer.compare(a.y, b.y); 
		            if (a.x != b.x) return Integer.compare(a.x, b.x);
		            return Integer.compare(a.z, b.z);
		        });
		
		        BlockEntry[] arr = visible.toArray(new BlockEntry[0]);
		        return new SchematicData(w, h, d, arr, arr.length);
		
		    } catch (Exception e) {
		        return null;
		    }
		}

    private boolean isSurface(HashSet<Long> set, int x, int y, int z) {
        return !(
            set.contains(BlockPos.asLong(x+1,y,z)) &&
            set.contains(BlockPos.asLong(x-1,y,z)) &&
            set.contains(BlockPos.asLong(x,y+1,z)) &&
            set.contains(BlockPos.asLong(x,y-1,z)) &&
            set.contains(BlockPos.asLong(x,y,z+1)) &&
            set.contains(BlockPos.asLong(x,y,z-1))
        );
    }


    private ItemStack getHoveredCabinetSchematic() {
        Slot s = this.hoveredSlot;
        if (s == null || !s.hasItem() || s.index < 0 || s.index >= 16)
            return ItemStack.EMPTY;
        return s.getItem();
    }

    private String extractFile(ItemStack stack) {
        try {
            CompoundTag tag = (CompoundTag) stack.save(Minecraft.getInstance().level.registryAccess());
            if (tag.contains("components")) {
                return tag.getCompound("components").getString("create:schematic_file");
            }
        } catch(Exception ignored){}
        return "";
    }
}