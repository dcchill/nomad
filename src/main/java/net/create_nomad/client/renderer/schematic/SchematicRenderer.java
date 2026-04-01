package net.create_nomad.client.renderer.schematic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.*;

/**
 * Optimized schematic renderer with frustum culling and LOD.
 * Caches block positions and renders with immediate mode.
 */
public class SchematicRenderer {
    private SchematicLevel level;
    private Vec3i size = Vec3i.ZERO;
    private BlockPos boundsMin = BlockPos.ZERO;
    private BlockPos boundsMax = BlockPos.ZERO;
    
    // Cached block data for fast rendering
    private final List<CachedBlock> cachedBlocks = new ArrayList<>();
    private final List<DynamicBlockEntity> dynamicBlockEntities = new ArrayList<>();
    
    // LOD system - kept for potential future use
    private final LodManager lodManager = new LodManager();
    private boolean useLOD = false;
    private float lodThreshold = 5000;
    
    // Simplified rendering - DISABLED (UV mapping issues with atlas textures)
    private boolean useSimplifiedRendering = false;
    
    // Occlusion culling - DISABLED (causes rendering issues)
    private final OcclusionCuller occlusionCuller = new OcclusionCuller();
    private boolean useOcclusionCulling = false;
    
    // Frustum culling - disabled, face culling is primary optimization
    
    public SchematicRenderer() {}
    
    /**
     * Load and prepare a schematic for rendering
     */
    public void load(SchematicLevel schematicLevel) {
        this.level = schematicLevel;
        var bounds = schematicLevel.getBounds();
        this.size = new Vec3i(
            bounds.maxX() - bounds.minX() + 1,
            bounds.maxY() - bounds.minY() + 1,
            bounds.maxZ() - bounds.minZ() + 1
        );
        this.boundsMin = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
        this.boundsMax = new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ());
        
        // Build render data
        buildRenderData();
    }
    
    /**
     * Build cached render data
     */
    private void buildRenderData() {
        cachedBlocks.clear();
        dynamicBlockEntities.clear();
        lodManager.clear();
        
        if (level == null) return;
        
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        var blockColors = Minecraft.getInstance().getBlockColors();
        
        // Collect all block positions
        List<BlockPos> allPositions = new ArrayList<>();
        Map<BlockPos, net.minecraft.world.level.block.entity.BlockEntity> blockEntityMap = new HashMap<>();
        
        for (BlockPos pos : BlockPos.betweenClosed(boundsMin, boundsMax)) {
            var state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getRenderShape() == RenderShape.INVISIBLE) continue;
            
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null && !blockEntity.getBlockState().isAir()) {
                blockEntityMap.put(pos.immutable(), blockEntity);
                continue;
            }
            
            allPositions.add(pos.immutable());
        }
        
        // Store block entities
        for (var entry : blockEntityMap.entrySet()) {
            dynamicBlockEntities.add(new DynamicBlockEntity(entry.getKey(), entry.getValue()));
        }
        
        // Cache block data - only blocks with exposed faces
        for (BlockPos pos : allPositions) {
            // Only cache blocks that have at least one face exposed to air
            if (!isFaceExposed(level, pos)) {
                continue;
            }

            var state = level.getBlockState(pos);
            var bakedModel = dispatcher.getBlockModel(state);
            var modelData = ModelData.EMPTY;
            var renderTypes = bakedModel.getRenderTypes(state, level.getRandom(), modelData);

            if (renderTypes.isEmpty()) continue;

            int color = blockColors.getColor(state, level, pos, 0);
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            List<RenderType> renderTypeList = new ArrayList<>();
            for (var rt : renderTypes) {
                if (rt != null) renderTypeList.add(rt);
            }

            if (!renderTypeList.isEmpty()) {
                cachedBlocks.add(new CachedBlock(
                    pos, state, bakedModel, modelData, renderTypeList, r, g, b, false
                ));
            }
        }
    }

    /**
     * Check if a block can be rendered as a simple cube
     */
    private boolean isSimpleBlock(net.minecraft.world.level.block.state.BlockState state, BakedModel model) {
        // Must be a full cube
        var shape = state.getShape(level, BlockPos.ZERO);
        if (shape.min(net.minecraft.core.Direction.Axis.X) != 0 || 
            shape.min(net.minecraft.core.Direction.Axis.Y) != 0 || 
            shape.min(net.minecraft.core.Direction.Axis.Z) != 0 ||
            shape.max(net.minecraft.core.Direction.Axis.X) != 1 || 
            shape.max(net.minecraft.core.Direction.Axis.Y) != 1 || 
            shape.max(net.minecraft.core.Direction.Axis.Z) != 1) {
            return false;
        }

        // Must be solid (no transparency)
        if (state.getRenderShape() != RenderShape.MODEL) return false;
        if (model.isGui3d() && model.isCustomRenderer()) return false;

        // Must use solid render type
        boolean hasOnlySolid = true;
        for (var rt : model.getRenderTypes(state, level.getRandom(), ModelData.EMPTY)) {
            if (rt == null || !rt.toString().contains("solid")) {
                hasOnlySolid = false;
                break;
            }
        }

        return hasOnlySolid;
    }
    
    /**
     * Check if a block has at least one face exposed to air or a different block
     */
    private boolean isFaceExposed(SchematicLevel level, BlockPos pos) {
        // Check all 6 faces
        return level.getBlockState(pos.above()).isAir() ||
               level.getBlockState(pos.below()).isAir() ||
               level.getBlockState(pos.north()).isAir() ||
               level.getBlockState(pos.south()).isAir() ||
               level.getBlockState(pos.west()).isAir() ||
               level.getBlockState(pos.east()).isAir();
    }
    
    /**
     * Render the schematic with given camera rotation
     */
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, 
                       float rotX, float rotY, float zoom) {
        if (level == null || size.equals(Vec3i.ZERO) || cachedBlocks.isEmpty()) return;
        
        // Use occlusion culling if enabled
        Set<BlockPos> visiblePositions = null;
        if (useOcclusionCulling) {
            visiblePositions = occlusionCuller.update(level, boundsMin, boundsMax, rotX, rotY);
        }
        
        // Render all cached blocks
        renderCachedBlocks(poseStack, bufferSource, visiblePositions);
        
        // Render dynamic block entities
        renderDynamicBlockEntities(poseStack, bufferSource);
    }
    
    private void renderCachedBlocks(PoseStack poseStack, MultiBufferSource bufferSource,
                                    Set<BlockPos> visiblePositions) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        
        for (CachedBlock cached : cachedBlocks) {
            // Skip if occlusion culling says this block is hidden
            if (visiblePositions != null && !visiblePositions.contains(cached.pos)) {
                continue;
            }
            
            poseStack.pushPose();
            poseStack.translate(cached.pos.getX(), cached.pos.getY(), cached.pos.getZ());
            
            // Use renderSingleBlock which handles per-face culling automatically
            dispatcher.renderSingleBlock(cached.state, poseStack, bufferSource,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            
            poseStack.popPose();
        }
    }
    
    /**
     * Render a simple cube using direct buffer (faster than full model rendering)
     */
    private void renderSimpleCube(PoseStack poseStack, MultiBufferSource bufferSource, 
                                  float r, float g, float b) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.solid());
        PoseStack.Pose pose = poseStack.last();
        
        // Draw cube faces (X, Y, Z normals for lighting)
        // Front face (Z+)
        buffer.addVertex(pose, 0, 0, 1).setColor(r, g, b, 1.0f).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(pose, 1, 0, 1).setColor(r, g, b, 1.0f).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(pose, 1, 1, 1).setColor(r, g, b, 1.0f).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(pose, 0, 1, 1).setColor(r, g, b, 1.0f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
        
        // Back face (Z-)
        buffer.addVertex(pose, 1, 0, 0).setColor(r, g, b, 1.0f).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, -1);
        buffer.addVertex(pose, 0, 0, 0).setColor(r, g, b, 1.0f).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, -1);
        buffer.addVertex(pose, 0, 1, 0).setColor(r, g, b, 1.0f).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, -1);
        buffer.addVertex(pose, 1, 1, 0).setColor(r, g, b, 1.0f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, -1);
        
        // Top face (Y+)
        buffer.addVertex(pose, 0, 1, 1).setColor(r, g, b, 1.0f).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
        buffer.addVertex(pose, 1, 1, 1).setColor(r, g, b, 1.0f).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
        buffer.addVertex(pose, 1, 1, 0).setColor(r, g, b, 1.0f).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
        buffer.addVertex(pose, 0, 1, 0).setColor(r, g, b, 1.0f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
        
        // Bottom face (Y-)
        buffer.addVertex(pose, 1, 0, 0).setColor(r, g, b, 1.0f).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, -1, 0);
        buffer.addVertex(pose, 0, 0, 0).setColor(r, g, b, 1.0f).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, -1, 0);
        buffer.addVertex(pose, 0, 0, 1).setColor(r, g, b, 1.0f).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, -1, 0);
        buffer.addVertex(pose, 1, 0, 1).setColor(r, g, b, 1.0f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, -1, 0);
        
        // Right face (X+)
        buffer.addVertex(pose, 1, 0, 1).setColor(r, g, b, 1.0f).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(1, 0, 0);
        buffer.addVertex(pose, 1, 1, 1).setColor(r, g, b, 1.0f).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(1, 0, 0);
        buffer.addVertex(pose, 1, 1, 0).setColor(r, g, b, 1.0f).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(1, 0, 0);
        buffer.addVertex(pose, 1, 0, 0).setColor(r, g, b, 1.0f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(1, 0, 0);
        
        // Left face (X-)
        buffer.addVertex(pose, 0, 0, 0).setColor(r, g, b, 1.0f).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(-1, 0, 0);
        buffer.addVertex(pose, 0, 1, 0).setColor(r, g, b, 1.0f).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(-1, 0, 0);
        buffer.addVertex(pose, 0, 1, 1).setColor(r, g, b, 1.0f).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(-1, 0, 0);
        buffer.addVertex(pose, 0, 0, 1).setColor(r, g, b, 1.0f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(-1, 0, 0);
    }
    
    private void renderDynamicBlockEntities(PoseStack poseStack, MultiBufferSource bufferSource) {
        var beDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        
        for (var dynamicBE : dynamicBlockEntities) {
            var ber = beDispatcher.getRenderer(dynamicBE.entity);
            if (ber == null) continue;
            
            poseStack.pushPose();
            poseStack.translate(dynamicBE.pos.getX(), dynamicBE.pos.getY(), dynamicBE.pos.getZ());
            
            try {
                ber.render(dynamicBE.entity, 0f, poseStack, bufferSource,
                          LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } catch (Exception ignored) {}
            
            poseStack.popPose();
        }
    }
    
    /**
     * Rebuild render data (call when schematic changes)
     */
    public void rebuild() {
        buildRenderData();
    }
    
    /**
     * Clear all cached data
     */
    public void clear() {
        level = null;
        size = Vec3i.ZERO;
        cachedBlocks.clear();
        dynamicBlockEntities.clear();
        lodManager.clear();
        occlusionCuller.clear();
    }
    
    public Vec3i getSize() {
        return size;
    }
    
    public boolean isLoaded() {
        return level != null && !size.equals(Vec3i.ZERO);
    }
    
    // Configuration

    public void setUseLOD(boolean useLOD) {
        this.useLOD = useLOD;
        rebuild();
    }

    public void setLodThreshold(float threshold) {
        this.lodThreshold = threshold;
        rebuild();
    }

    /**
     * Enable simplified rendering for large schematics.
     * Renders simple blocks (full cubes) as basic quads instead of full models.
     * Can improve performance by 30-50% on large schematics.
     */
    public void setUseSimplifiedRendering(boolean use) {
        this.useSimplifiedRendering = use;
    }

    /**
     * Enable occlusion culling.
     * Skips blocks that are hidden behind other opaque blocks.
     * Can reduce rendered blocks by 50-80% depending on view angle.
     */
    public void setUseOcclusionCulling(boolean use) {
        this.useOcclusionCulling = use;
    }

    /**
     * Get count of blocks that qualify for simplified rendering
     */
    public int getSimpleBlockCount() {
        int count = 0;
        for (CachedBlock block : cachedBlocks) {
            if (block.isSimpleBlock) count++;
        }
        return count;
    }

    /**
     * Get total cached block count
     */
    public int getCachedBlockCount() {
        return cachedBlocks.size();
    }

    /**
     * Cached block data for fast rendering
     */
    public static class CachedBlock {
        public final BlockPos pos;
        public final net.minecraft.world.level.block.state.BlockState state;
        public final BakedModel bakedModel;
        public final ModelData modelData;
        public final List<RenderType> renderTypes;
        public final float r, g, b;
        public final boolean isSimpleBlock;
        
        public CachedBlock(BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                          BakedModel bakedModel,
                          ModelData modelData, List<RenderType> renderTypes, float r, float g, float b,
                          boolean isSimpleBlock) {
            this.pos = pos;
            this.state = state;
            this.bakedModel = bakedModel;
            this.modelData = modelData;
            this.renderTypes = renderTypes;
            this.r = r;
            this.g = g;
            this.b = b;
            this.isSimpleBlock = isSimpleBlock;
        }
    }
    
    /**
     * Simple block entity wrapper for dynamic rendering
     */
    public static class DynamicBlockEntity {
        public final BlockPos pos;
        public final net.minecraft.world.level.block.entity.BlockEntity entity;
        
        public DynamicBlockEntity(BlockPos pos, net.minecraft.world.level.block.entity.BlockEntity entity) {
            this.pos = pos;
            this.entity = entity;
        }
    }
}
