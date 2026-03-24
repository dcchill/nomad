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
import java.util.*;

public class FilingCabinetGuiScreen extends AbstractContainerScreen<FilingCabinetGuiMenu> implements CreateNomadModScreens.ScreenAccessor {

    private static final ResourceLocation texture = ResourceLocation.parse("create_nomad:textures/screens/filing_cabinet_gui.png");

    private SchematicData cachedData = null;
    private String cachedFile = "";

    private float buildProgress = 0f;
    private boolean animating = false;

    private Map<Long, List<BlockEntry>> chunkCache = new HashMap<>();

    public FilingCabinetGuiScreen(FilingCabinetGuiMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    public void updateMenuState(int elementType, String name, Object elementState) {}

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 5, 5, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1,1,1,1);
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        ItemStack stack = getHoveredCabinetSchematic();
        renderPreview(guiGraphics, stack, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderPreview(GuiGraphics guiGraphics, ItemStack stack, float partialTicks) {
        int x = this.leftPos + 14;
        int y = this.topPos + 20;
        int w = 72;
        int h = 72;
        if (stack.isEmpty()) return;
        render3D(guiGraphics, stack, x, y, w, h, partialTicks);
    }

    private void render3D(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int w, int h, float partialTicks) {

        String file = extractFile(stack);
        if (file.isBlank()) return;

        if (!file.equals(cachedFile)) {
            cachedFile = file;
            cachedData = load(file);
            buildChunkCache(cachedData);
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
        pose.mulPose(Axis.YP.rotationDegrees((mc.level.getGameTime() / 4f) % 360));

        pose.translate(-cachedData.w/2f, -cachedData.h/2f, -cachedData.d/2f);

        renderChunks(pose);

        pose.popPose();

        guiGraphics.disableScissor();
    }

    private void buildChunkCache(SchematicData data) {

        chunkCache.clear();
        if (data == null) return;

        int maxRender = Math.min(500000, data.size);
        int step = Math.max(1, data.size / maxRender);

        for (int i = 0; i < data.size; i += step) {
            BlockEntry e = data.entries[i];

            int cx = e.x >> 4;
            int cy = e.y >> 4;
            int cz = e.z >> 4;

            long key = (((long)cx & 0x3FFFFF) << 42) | (((long)cy & 0xFFFFF) << 22) | ((long)cz & 0x3FFFFF);

            chunkCache.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }
    }

    private void renderChunks(PoseStack pose) {

        var mc = Minecraft.getInstance();
        var renderer = mc.getBlockRenderer();
        var bufferSource = mc.renderBuffers().bufferSource();

        if (animating) {
            buildProgress += 0.03f;
            if (buildProgress >= 1f) {
                buildProgress = 1f;
                animating = false;
            }
        }

        for (var entry : chunkCache.values()) {

            for (BlockEntry e : entry) {

                if (e.y > buildProgress * cachedData.h) continue;

                pose.pushPose();
                pose.translate(e.x, e.y, e.z);

                renderer.renderSingleBlock(
                    e.state,
                    pose,
                    bufferSource,
                    15728880,
                    OverlayTexture.NO_OVERLAY
                );

                pose.popPose();
            }
        }

        bufferSource.endBatch();
    }

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

            for (int i = 0; i < blocks.size(); i++) {
                ListTag pos = blocks.getCompound(i).getList("pos", Tag.TAG_INT);
                blockSet.add(BlockPos.asLong(pos.getInt(0), pos.getInt(1), pos.getInt(2)));
            }

            ArrayList<BlockEntry> visible = new ArrayList<>();

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