package net.create_nomad.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.create_nomad.init.CreateNomadModScreens;
import net.create_nomad.world.inventory.FilingCabinetGuiMenu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import com.simibubi.create.content.schematics.client.SchematicRenderer;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FilingCabinetGuiScreen extends AbstractContainerScreen<FilingCabinetGuiMenu> implements CreateNomadModScreens.ScreenAccessor {

    private static final ResourceLocation texture =
            ResourceLocation.parse("create_nomad:textures/screens/filing_cabinet_gui.png");

    private static final Map<String, SchematicResult> SCHEMATIC_CACHE = new HashMap<>();

    private String cachedFile = "";
    private SchematicRenderer schematicRenderer = null;
    private SchematicLevel schematicLevel = null;

    private int schematicW = 0;
    private int schematicH = 0;
    private int schematicD = 0;

    private CompletableFuture<SchematicResult> loadFuture = null;

    public FilingCabinetGuiScreen(FilingCabinetGuiMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (loadFuture != null) {
            loadFuture.cancel(true);
            loadFuture = null;
        }
        schematicRenderer = null;
        schematicLevel = null;
    }

    @Override
    public void updateMenuState(int elementType, String name, Object elementState) {}

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 5, 5, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
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
        if (stack.isEmpty()) return;
        render3D(guiGraphics, stack, this.leftPos + 14, this.topPos + 20, 72, 72, partialTicks);
    }

    private void render3D(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int w, int h, float partialTicks) {
        String file = extractFile(stack);
        if (file.isBlank()) return;

        if (!file.equals(cachedFile)) {
            cachedFile = file;
            schematicRenderer = null;
            schematicLevel = null;
            schematicW = 0;
            schematicH = 0;
            schematicD = 0;

            if (loadFuture != null) {
                loadFuture.cancel(true);
                loadFuture = null;
            }

            SchematicResult cached = SCHEMATIC_CACHE.get(file);
            if (cached != null) {
                applyLoadedSchematic(cached);
            } else {
                var mc = Minecraft.getInstance();
                Path path = mc.gameDirectory.toPath().resolve("schematics").resolve(file);
                var level = mc.level;

                loadFuture = CompletableFuture.supplyAsync(() -> loadSchematicLevel(path, level, file));
            }
        }

        if (loadFuture != null && loadFuture.isDone()) {
            SchematicResult result = loadFuture.getNow(null);
            loadFuture = null;

            if (result != null) {
                SCHEMATIC_CACHE.put(file, result);
                applyLoadedSchematic(result);
            }
        }

        if (schematicRenderer == null || schematicLevel == null) return;

        var mc = Minecraft.getInstance();
        var pose = guiGraphics.pose();

        guiGraphics.enableScissor(x, y, x + w, y + h);
        pose.pushPose();

        pose.translate(x + w / 2f, y + h / 2f, 300);

        float maxDim = Math.max(schematicW, Math.max(schematicH, schematicD));
        if (maxDim <= 0) maxDim = 1;

        float scale = (Math.min(w, h) / maxDim) * 0.75f;
        pose.scale(scale, -scale, scale);

        pose.mulPose(Axis.XP.rotationDegrees(25));
        pose.mulPose(Axis.YP.rotationDegrees((mc.level.getGameTime() / 4f) % 360));

        pose.translate(-schematicW / 2f, -schematicH / 2f, -schematicD / 2f);

        DefaultSuperRenderTypeBuffer superBuffer = DefaultSuperRenderTypeBuffer.getInstance();
        schematicRenderer.render(pose, superBuffer);
        superBuffer.draw();

        pose.popPose();
        guiGraphics.disableScissor();
    }

    private void applyLoadedSchematic(SchematicResult result) {
        if (result == null || result.level() == null) return;

        schematicLevel = result.level();
        schematicRenderer = new SchematicRenderer();
        schematicRenderer.display(schematicLevel);

        var bounds = schematicLevel.getBounds();
        schematicW = bounds.getXSpan();
        schematicH = bounds.getYSpan();
        schematicD = bounds.getZSpan();
    }

    private SchematicResult loadSchematicLevel(Path path, net.minecraft.world.level.Level level, String cacheKey) {
        try {
            if (!Files.exists(path)) return null;

            CompoundTag root;
            try (InputStream stream = Files.newInputStream(path)) {
                root = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            }

            ListTag sizeTag = root.getList("size", Tag.TAG_INT);

            ListTag palette = root.contains("palette")
                    ? root.getList("palette", Tag.TAG_COMPOUND)
                    : root.getList("Palette", Tag.TAG_COMPOUND);

            ListTag blocks = root.contains("blocks")
                    ? root.getList("blocks", Tag.TAG_COMPOUND)
                    : root.getList("Blocks", Tag.TAG_COMPOUND);

            if (sizeTag.size() < 3 || palette.isEmpty() || blocks.isEmpty()) return null;

            SchematicLevel schematicLevel = new SchematicLevel(BlockPos.ZERO, level);

            for (int i = 0; i < blocks.size(); i++) {
                CompoundTag b = blocks.getCompound(i);
                ListTag pos = b.getList("pos", Tag.TAG_INT);

                int bx = pos.getInt(0);
                int by = pos.getInt(1);
                int bz = pos.getInt(2);

                CompoundTag stateTag = palette.getCompound(b.getInt("state"));
                var block = BuiltInRegistries.BLOCK.get(
                        ResourceLocation.tryParse(stateTag.getString("Name")));

                if (block == null) continue;

                BlockState state = block.defaultBlockState();
                if (state.isAir()) continue;

                schematicLevel.setBlock(new BlockPos(bx, by, bz), state, 3);
            }

            return new SchematicResult(schematicLevel);

        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack getHoveredCabinetSchematic() {
        Slot s = this.hoveredSlot;
        if (s == null || !s.hasItem() || s.index < 0 || s.index >= 16)
            return ItemStack.EMPTY;
        return s.getItem();
    }

    private String extractFile(ItemStack stack) {
        try {
            var components = stack.getComponents();

            if (components.has(DataComponents.CUSTOM_DATA)) {
                CompoundTag tag = components.get(DataComponents.CUSTOM_DATA).copyTag();
                if (tag.contains("create:schematic_file"))
                    return tag.getString("create:schematic_file");
            }

            CompoundTag full = (CompoundTag) stack.save(Minecraft.getInstance().level.registryAccess());

            if (full.contains("components")) {
                CompoundTag comps = full.getCompound("components");
                if (comps.contains("create:schematic_file"))
                    return comps.getString("create:schematic_file");
            }

        } catch (Exception ignored) {}

        return "";
    }

    private record SchematicResult(SchematicLevel level) {}
}