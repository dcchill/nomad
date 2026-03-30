package net.create_nomad.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicItem;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.neoforged.neoforge.client.model.data.ModelData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;

import net.create_nomad.init.CreateNomadModScreens;
import net.create_nomad.world.inventory.FilingCabinetGuiMenu;

public class FilingCabinetGuiScreen extends AbstractContainerScreen<FilingCabinetGuiMenu>
        implements CreateNomadModScreens.ScreenAccessor {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.parse("create_nomad:textures/screens/filing_cabinet_gui.png");

    private static final int PREVIEW_X = 14;
    private static final int PREVIEW_Y = 20;
    private static final int PREVIEW_W = 72;
    private static final int PREVIEW_H = 72;
    private static final float ISOMETRIC_ROTATION_X = 30f;
    private static final float ISOMETRIC_ROTATION_Y = -45f;

    // ── cache — mirrors Kotlin object fields exactly ──────────────────────────
    private String         cachedFilename = null;
    private SchematicLevel cachedLevel    = null;
    private Vec3i          cachedSize     = Vec3i.ZERO;
    private float          cachedScaleBase = 1f;
    private List<RenderableBlock> cachedRenderableBlocks = List.of();
    private List<BlockEntity> cachedBlockEntities = List.of();

    private float rotationX = ISOMETRIC_ROTATION_X;
    private float rotationY = ISOMETRIC_ROTATION_Y;
    private boolean isDragging = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    private int previewScreenX = 0;
    private int previewScreenY = 0;

    public FilingCabinetGuiScreen(FilingCabinetGuiMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 256;
        this.imageHeight = 256;
    }

    // ── lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onClose() {
        super.onClose();
        // mirror Kotlin clear()
        cachedFilename = null;
        cachedLevel    = null;
        cachedSize     = Vec3i.ZERO;
        cachedScaleBase = 1f;
        cachedRenderableBlocks = List.of();
        cachedBlockEntities = List.of();
    }

    @Override
    public void updateMenuState(int elementType, String name, Object elementState) {}

    private boolean isInsidePreview(double mouseX, double mouseY) {
        return mouseX >= previewScreenX && mouseX < previewScreenX + PREVIEW_W
            && mouseY >= previewScreenY && mouseY < previewScreenY + PREVIEW_H;
    }

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
            rotationY += (float) (mouseX - lastMouseX);
            rotationX += (float) (mouseY - lastMouseY) * 0.5f;
            rotationX = Math.max(-89f, Math.min(89f, rotationX));
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
            String file = extractFile(stack);
            if (!file.isBlank()) {
                // mirrors Kotlin renderPreview(filename, guiGraphics, x, y, w, h)
                // which calls getOrLoadLevel(filename) inline — sync load, cached after
                renderPreview(file, guiGraphics,
                        previewScreenX, previewScreenY,
                        PREVIEW_W, PREVIEW_H,
                        ISOMETRIC_ROTATION_X, ISOMETRIC_ROTATION_Y, 1f);
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // ── exact port of Kotlin renderPreview(filename, guiGraphics, ...) ────────

    private void renderPreview(String filename, GuiGraphics guiGraphics,
                               int x, int y, int w, int h,
                               float rotX, float rotY, float zoom) {
        // mirrors Kotlin getOrLoadLevel — sync, returns cached level on subsequent calls
        SchematicLevel level = getOrLoadLevel(filename);
        if (level == null) return;

        Vec3i size = cachedSize;
        if (size.equals(Vec3i.ZERO)) return;

        guiGraphics.enableScissor(x, y, x + w, y + h);

        var pose = guiGraphics.pose();
        pose.pushPose();

        // Position at center of preview area, with Z depth for proper ordering
        pose.translate((x + w / 2.0), (y + h / 2.0), 150.0);

        // Scale to fit the preview rectangle, then apply user zoom
        float scale  = cachedScaleBase * Math.min(w, h) * zoom;
        pose.scale(scale, -scale, scale); // Negative Y because GUI Y is downward

        // Rotation (isometric default: 30° X, -45° Y)
        pose.mulPose(Axis.XP.rotationDegrees(rotX));
        pose.mulPose(Axis.YP.rotationDegrees(rotY));

        // Center the schematic at origin
        pose.translate(-size.getX() / 2.0, -size.getY() / 2.0, -size.getZ() / 2.0);

        // Render each block
        var mc           = Minecraft.getInstance();
        var dispatcher   = mc.getBlockRenderer();
        var bufferSource = guiGraphics.bufferSource();
        RenderSystem.enableDepthTest();

        for (var renderable : cachedRenderableBlocks) {
            var blockPos = renderable.pos();
            var state = renderable.state();

            pose.pushPose();
            pose.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());

            try {
                var blockEntity = level.getBlockEntity(blockPos);
                ModelData modelData = blockEntity != null
                        ? renderable.bakedModel().getModelData(level, blockPos, state, blockEntity.getModelData())
                        : renderable.modelData();

                for (var renderType : renderable.renderTypes()) {
                    dispatcher.getModelRenderer().renderModel(
                            pose.last(), bufferSource.getBuffer(renderType),
                            state, renderable.bakedModel(), renderable.r(), renderable.g(), renderable.b(),
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                            modelData, renderType);
                }
            } catch (Exception ignored) {}

            pose.popPose();
        }

        // Render block entities (belts, kinetic blocks, etc.) via their BlockEntityRenderers
        var beDispatcher = mc.getBlockEntityRenderDispatcher();
        for (var blockEntity : cachedBlockEntities) {
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

    // ── exact port of Kotlin getOrLoadLevel ──────────────────────────────────

    private SchematicLevel getOrLoadLevel(String filename) {
        // return cached immediately — this is why the Kotlin has no FPS drop
        if (filename.equals(cachedFilename)) return cachedLevel;

        cachedFilename = filename;
        cachedLevel    = null;
        cachedSize     = Vec3i.ZERO;
        cachedScaleBase = 1f;
        cachedRenderableBlocks = List.of();
        cachedBlockEntities = List.of();

        try {
            var mc     = Minecraft.getInstance();
            var level  = mc.level;
            var player = mc.player;
            if (level == null || player == null) return null;

            ItemStack fakeStack = new ItemStack(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                            ResourceLocation.parse("create:schematic")));
            fakeStack.set(AllDataComponents.SCHEMATIC_FILE,     filename);
            fakeStack.set(AllDataComponents.SCHEMATIC_OWNER,    player.getGameProfile().getName());
            fakeStack.set(AllDataComponents.SCHEMATIC_ANCHOR,   BlockPos.ZERO);
            fakeStack.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.NONE);
            fakeStack.set(AllDataComponents.SCHEMATIC_MIRROR,   Mirror.NONE);
            fakeStack.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);

            StructureTemplate template = SchematicItem.loadSchematic(level, fakeStack);
            if (template.getSize().equals(Vec3i.ZERO)) return null;

            cachedSize = template.getSize();
            float maxDim = Math.max(cachedSize.getX(), Math.max(cachedSize.getY(), cachedSize.getZ()));
            cachedScaleBase = maxDim <= 0 ? 1f : (1f / (maxDim * 1.6f));

            var schematicLevel = new SchematicLevel(level);
            var settings       = new StructurePlaceSettings();
            settings.setRotation(Rotation.NONE);
            settings.setMirror(Mirror.NONE);

            template.placeInWorld(
                    schematicLevel, BlockPos.ZERO, BlockPos.ZERO,
                    settings, schematicLevel.getRandom(), Block.UPDATE_CLIENTS);

            for (var be : schematicLevel.getBlockEntities())
                be.setLevel(schematicLevel);

            var dispatcher = Minecraft.getInstance().getBlockRenderer();
            var renderableBlocks = new ArrayList<RenderableBlock>();
            var bounds = schematicLevel.getBounds();
            for (BlockPos blockPos : BlockPos.betweenClosed(
                    bounds.minX(), bounds.minY(), bounds.minZ(),
                    bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                BlockState state = schematicLevel.getBlockState(blockPos);
                if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) continue;

                var bakedModel = dispatcher.getBlockModel(state);
                var blockEntity = schematicLevel.getBlockEntity(blockPos);
                ModelData modelData = blockEntity != null
                        ? bakedModel.getModelData(schematicLevel, blockPos, state, blockEntity.getModelData())
                        : ModelData.EMPTY;
                int color = Minecraft.getInstance().getBlockColors().getColor(state, null, null, 0);
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >> 8) & 0xFF) / 255f;
                float b = (color & 0xFF) / 255f;
                var rng = RandomSource.create(state.getSeed(blockPos));
                var renderTypes = new ArrayList<RenderType>();
                for (var renderType : bakedModel.getRenderTypes(state, rng, modelData)) {
                    renderTypes.add(renderType);
                }
                if (hasAnimatedTexture(state, bakedModel, modelData, rng, renderTypes)) continue;

                renderableBlocks.add(new RenderableBlock(
                        blockPos.immutable(), state, bakedModel, modelData, r, g, b, List.copyOf(renderTypes)));
            }
            cachedRenderableBlocks = List.copyOf(renderableBlocks);

            var renderedBlockEntities = new ArrayList<BlockEntity>();
            for (var blockEntity : schematicLevel.getRenderedBlockEntities()) {
                renderedBlockEntities.add(blockEntity);
            }
            cachedBlockEntities = List.copyOf(renderedBlockEntities);

            cachedLevel = schematicLevel;
            return schematicLevel;

        } catch (Exception e) {
            return null;
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ItemStack getHoveredCabinetSchematic() {
        Slot s = this.hoveredSlot;
        if (s == null || !s.hasItem() || s.index < 0 || s.index >= 16)
            return ItemStack.EMPTY;
        return s.getItem();
    }

    private record RenderableBlock(
            BlockPos pos,
            BlockState state,
            net.minecraft.client.resources.model.BakedModel bakedModel,
            ModelData modelData,
            float r,
            float g,
            float b,
            List<RenderType> renderTypes) {}

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

    private boolean hasAnimatedTexture(
            BlockState state,
            net.minecraft.client.resources.model.BakedModel bakedModel,
            ModelData modelData,
            RandomSource rng,
            List<RenderType> renderTypes
    ) {
        for (RenderType renderType : renderTypes) {
            if (isAnimatedQuadList(bakedModel.getQuads(state, null, rng, modelData, renderType))) return true;
            for (Direction direction : Direction.values()) {
                if (isAnimatedQuadList(bakedModel.getQuads(state, direction, rng, modelData, renderType))) return true;
            }
        }
        return false;
    }

    private boolean isAnimatedQuadList(List<net.minecraft.client.renderer.block.model.BakedQuad> quads) {
        for (var quad : quads) {
            if (quad.getSprite() == null) continue;
            if (isAnimatedSprite(quad.getSprite())) return true;
        }
        return false;
    }

    private boolean isAnimatedSprite(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        try {
            Object contents = sprite.contents();
            for (Field field : contents.getClass().getDeclaredFields()) {
                if (!field.getType().getSimpleName().toLowerCase().contains("animated")) continue;
                field.setAccessible(true);
                if (field.get(contents) != null) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
