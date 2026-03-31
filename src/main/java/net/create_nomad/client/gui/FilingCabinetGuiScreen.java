package net.create_nomad.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicItem;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.neoforged.neoforge.client.model.data.ModelData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import net.create_nomad.init.CreateNomadModScreens;
import net.create_nomad.world.inventory.FilingCabinetGuiMenu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class FilingCabinetGuiScreen extends AbstractContainerScreen<FilingCabinetGuiMenu>
        implements CreateNomadModScreens.ScreenAccessor {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.parse("create_nomad:textures/screens/filing_cabinet_gui.png");

    // ── schematic state ──────────────────────────────────────────────────────
    private static String cachedFilename = null;
    private static SchematicLevel cachedLevel = null;
    private static Vec3i cachedSize = Vec3i.ZERO;
    
    private String currentFilename = "";

    // ── rotation ─────────────────────────────────────────────────────────────
    private float   rotationX  = 30f;   // pitch
    private float   rotationY  = -45f;  // yaw
    private boolean isDragging = false;
    private double  lastMouseX = 0;
    private double  lastMouseY = 0;

    private int previewScreenX = 0;
    private int previewScreenY = 0;
    private static final int PREVIEW_X = 14;
    private static final int PREVIEW_Y = 20;
    private static final int PREVIEW_W = 72;
    private static final int PREVIEW_H = 72;

    public FilingCabinetGuiScreen(FilingCabinetGuiMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 256;
        this.imageHeight = 256;
    }

    // ── lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onClose() {
        super.onClose();
        // Clear static cache when screen closes
        cachedFilename = null;
        cachedLevel = null;
        cachedSize = Vec3i.ZERO;
        currentFilename = "";
    }

    @Override
    public void updateMenuState(int elementType, String name, Object elementState) {}

    // ── mouse ────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsidePreview(mouseX, mouseY)) {
            isDragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (isDragging && button == 0) {
            rotationY += (float)(mouseX - lastMouseX);
            rotationX += (float)(mouseY - lastMouseY) * 0.5f;
            rotationX  = Math.max(-89f, Math.min(89f, rotationX));
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInsidePreview(double mouseX, double mouseY) {
        return mouseX >= previewScreenX && mouseX < previewScreenX + PREVIEW_W
            && mouseY >= previewScreenY && mouseY < previewScreenY + PREVIEW_H;
    }

    // ── rendering ────────────────────────────────────────────────────────────

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 5, 5, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        previewScreenX = this.leftPos + PREVIEW_X;
        previewScreenY = this.topPos  + PREVIEW_Y;

        ItemStack stack = getHoveredCabinetSchematic();
        if (!stack.isEmpty()) {
            String filename = extractFile(stack);
            if (!filename.equals(currentFilename)) {
                currentFilename = filename;
                loadSchematic(filename);
            }
            
            if (cachedLevel != null && !cachedSize.equals(Vec3i.ZERO)) {
                // Auto-spin yaw when not dragging
                if (!isDragging) {
                    rotationY = (Minecraft.getInstance().level.getGameTime() / 4f) % 360f;
                }
                
                renderPreview(guiGraphics,
                        previewScreenX, previewScreenY, PREVIEW_W, PREVIEW_H,
                        rotationX, rotationY, 1f);
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // ── direct port of SchematicPreviewRenderer.kt renderPreview ─────────────

    private void renderPreview(GuiGraphics guiGraphics,
                               int x, int y, int w, int h,
                               float rotX, float rotY, float zoom) {
        var size = cachedSize;
        if (size.equals(Vec3i.ZERO)) return;

        guiGraphics.enableScissor(x, y, x + w, y + h);

        var pose = guiGraphics.pose();
        pose.pushPose();

        // Position at center of preview area, with Z depth for proper ordering
        pose.translate((x + w / 2.0), (y + h / 2.0), 150.0);

        // Scale to fit the preview rectangle, then apply user zoom
        float maxDim = Math.max(size.getX(), Math.max(size.getY(), size.getZ()));
        float scale  = Math.min(w, h) / (maxDim * 1.6f) * zoom;
        pose.scale(scale, -scale, scale); // Negative Y because GUI Y is downward

        // Rotation (isometric default: 30° X, -45° Y)
        pose.mulPose(Axis.XP.rotationDegrees(rotX));
        pose.mulPose(Axis.YP.rotationDegrees(rotY));

        // Center the schematic at origin
        pose.translate(-size.getX() / 2.0, -size.getY() / 2.0, -size.getZ() / 2.0);

        RenderSystem.enableDepthTest();

        // Render all blocks
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        var bufferSource = guiGraphics.bufferSource();
        var bounds = cachedLevel.getBounds();

        for (BlockPos blockPos : BlockPos.betweenClosed(
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ())) {

            var state = cachedLevel.getBlockState(blockPos);
            if (state.isAir()) continue;
            if (state.getRenderShape() == RenderShape.INVISIBLE) continue;

            pose.pushPose();
            pose.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());

            try {
                var blockEntity = cachedLevel.getBlockEntity(blockPos);
                var bakedModel = dispatcher.getBlockModel(state);
                ModelData modelData = (blockEntity != null)
                        ? bakedModel.getModelData(cachedLevel, blockPos, state, blockEntity.getModelData())
                        : ModelData.EMPTY;

                int color = Minecraft.getInstance().getBlockColors().getColor(state, null, null, 0);
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >>  8) & 0xFF) / 255f;
                float b = (color & 0xFF) / 255f;
                var rng = RandomSource.create(state.getSeed(blockPos));

                for (var renderType : bakedModel.getRenderTypes(state, rng, modelData)) {
                    dispatcher.getModelRenderer().renderModel(
                            pose.last(), bufferSource.getBuffer(renderType),
                            state, bakedModel, r, g, b,
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                            modelData, renderType);
                }
            } catch (Exception ignored) {}

            pose.popPose();
        }

        // Render block entities (belts, kinetic blocks, etc.) via their BlockEntityRenderers
        var beDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        for (var blockEntity : cachedLevel.getRenderedBlockEntities()) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            BlockEntityRenderer ber = beDispatcher.getRenderer(blockEntity);
            if (ber == null) continue;

            var pos = blockEntity.getBlockPos();
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            try {
                ber.render(blockEntity, 0f, pose, bufferSource,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } catch (Exception ignored) {}
            pose.popPose();
        }

        bufferSource.endBatch();
        pose.popPose();
        guiGraphics.disableScissor();
    }

    // ── schematic loading ─────────────────────────────────────────────────────

    private void loadSchematic(String filename) {
        if (filename.equals(cachedFilename)) return;
        
        cachedFilename = filename;
        cachedLevel = null;
        cachedSize = Vec3i.ZERO;
        
        if (filename.isBlank()) return;
        
        try {
            var mc = Minecraft.getInstance();
            var level = mc.level;
            var player = mc.player;
            if (level == null || player == null) return;

            Path path = mc.gameDirectory.toPath().resolve("schematics").resolve(filename);
            if (!Files.exists(path)) return;

            var fakeStack = new ItemStack(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                            ResourceLocation.parse("create:schematic")));
            fakeStack.set(AllDataComponents.SCHEMATIC_FILE,     filename);
            fakeStack.set(AllDataComponents.SCHEMATIC_OWNER,    player.getGameProfile().getName());
            fakeStack.set(AllDataComponents.SCHEMATIC_ANCHOR,   BlockPos.ZERO);
            fakeStack.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.NONE);
            fakeStack.set(AllDataComponents.SCHEMATIC_MIRROR,   Mirror.NONE);
            fakeStack.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);

            var template = SchematicItem.loadSchematic(level, fakeStack);
            if (template.getSize().equals(Vec3i.ZERO)) return;

            cachedSize = template.getSize();

            var schematicLevel = new SchematicLevel(level);
            var settings = new StructurePlaceSettings();
            settings.setRotation(Rotation.NONE);
            settings.setMirror(Mirror.NONE);

            template.placeInWorld(
                    schematicLevel, BlockPos.ZERO, BlockPos.ZERO,
                    settings, schematicLevel.getRandom(), Block.UPDATE_CLIENTS);

            for (var be : schematicLevel.getBlockEntities())
                be.setLevel(schematicLevel);

            cachedLevel = schematicLevel;

        } catch (Exception e) {
            e.printStackTrace();
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
            CompoundTag full = (CompoundTag) stack.save(
                    Minecraft.getInstance().level.registryAccess());
            if (full.contains("components")) {
                CompoundTag comps = full.getCompound("components");
                if (comps.contains("create:schematic_file"))
                    return comps.getString("create:schematic_file");
            }
        } catch (Exception ignored) {}
        return "";
    }
}