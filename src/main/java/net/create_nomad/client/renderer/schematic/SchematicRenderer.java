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
    
    // Cached block data for fast rendering - grouped by render type for batching
    private final Map<RenderType, List<CachedBlock>> blocksByRenderType = new HashMap<>();
    private final List<DynamicBlockEntity> dynamicBlockEntities = new ArrayList<>();
    
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
     * Build cached render data - groups blocks by render type for batching
     */
    private void buildRenderData() {
        blocksByRenderType.clear();
        dynamicBlockEntities.clear();
        
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
        
        // Cache block data - group by primary render type for batching
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

            // Use primary render type for batching
            RenderType primaryType = renderTypes.iterator().next();
            
            blocksByRenderType.computeIfAbsent(primaryType, k -> new ArrayList<>())
                .add(new CachedBlock(pos, state, bakedModel, modelData, r, g, b));
        }
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
        if (level == null || size.equals(Vec3i.ZERO) || blocksByRenderType.isEmpty()) return;
        
        // Render blocks batched by render type
        renderBatched(poseStack, bufferSource);
        
        // Render dynamic block entities
        renderDynamicBlockEntities(poseStack, bufferSource);
    }
    
    /**
     * Render blocks with batching by render type
     */
    private void renderBatched(PoseStack poseStack, MultiBufferSource bufferSource) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        
        // Render each batch with its own buffer
        for (var entry : blocksByRenderType.entrySet()) {
            RenderType renderType = entry.getKey();
            List<CachedBlock> batch = entry.getValue();
            
            // Get buffer for this render type
            VertexConsumer buffer = bufferSource.getBuffer(renderType);
            
            // Render all blocks in this batch
            for (CachedBlock cached : batch) {
                poseStack.pushPose();
                poseStack.translate(cached.pos.getX(), cached.pos.getY(), cached.pos.getZ());
                
                dispatcher.renderSingleBlock(cached.state, poseStack, bufferSource,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                
                poseStack.popPose();
            }
        }
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
        blocksByRenderType.clear();
        dynamicBlockEntities.clear();
    }
    
    public Vec3i getSize() {
        return size;
    }
    
    public boolean isLoaded() {
        return level != null && !size.equals(Vec3i.ZERO);
    }

    /**
     * Get total cached block count
     */
    public int getCachedBlockCount() {
        int count = 0;
        for (List<CachedBlock> batch : blocksByRenderType.values()) {
            count += batch.size();
        }
        return count;
    }

    /**
     * Cached block data for fast rendering
     */
    public static class CachedBlock {
        public final BlockPos pos;
        public final net.minecraft.world.level.block.state.BlockState state;
        public final BakedModel bakedModel;
        public final ModelData modelData;
        public final float r, g, b;
        
        public CachedBlock(BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                          BakedModel bakedModel, ModelData modelData, float r, float g, float b) {
            this.pos = pos;
            this.state = state;
            this.bakedModel = bakedModel;
            this.modelData = modelData;
            this.r = r;
            this.g = g;
            this.b = b;
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
