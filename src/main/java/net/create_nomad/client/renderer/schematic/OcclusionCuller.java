package net.create_nomad.client.renderer.schematic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Occlusion culling system for schematic rendering.
 * Determines which blocks are hidden behind other opaque blocks
 * from the current view angle.
 */
public class OcclusionCuller {
    
    // Cached visibility data
    private final Set<BlockPos> visibleBlocks = new HashSet<>();
    private float lastCheckRotX = -1;
    private float lastCheckRotY = -1;
    
    private static final float OCCLUSION_CHECK_THRESHOLD = 5.0f;
    
    /**
     * Update occlusion culling based on camera rotation
     * @param level The schematic level
     * @param boundsMin Minimum bounds of schematic
     * @param boundsMax Maximum bounds of schematic
     * @param rotX Camera rotation X
     * @param rotY Camera rotation Y
     * @return Set of block positions that are visible (not occluded)
     */
    public Set<BlockPos> update(net.createmod.catnip.levelWrappers.SchematicLevel level,
                                BlockPos boundsMin, BlockPos boundsMax,
                                float rotX, float rotY) {
        // Only recalculate if rotation changed significantly
        if (visibleBlocks.size() > 0 &&
            Math.abs(rotX - lastCheckRotX) < OCCLUSION_CHECK_THRESHOLD &&
            Math.abs(rotY - lastCheckRotY) < OCCLUSION_CHECK_THRESHOLD) {
            return visibleBlocks;
        }
        
        lastCheckRotX = rotX;
        lastCheckRotY = rotY;
        
        visibleBlocks.clear();
        
        // Calculate view direction from rotation
        float yawRad = rotY * ((float)Math.PI / 180f);
        float pitchRad = rotX * ((float)Math.PI / 180f);
        
        // View direction vector
        float viewX = (float)(-Math.sin(yawRad) * Math.cos(pitchRad));
        float viewY = (float)Math.sin(pitchRad);
        float viewZ = (float)(Math.cos(yawRad) * Math.cos(pitchRad));
        
        // Determine primary view axis (which axis are we looking most along?)
        float absX = Math.abs(viewX);
        float absY = Math.abs(viewY);
        float absZ = Math.abs(viewZ);
        
        // Collect all non-air blocks
        List<BlockPos> allBlocks = new ArrayList<>();
        Set<BlockPos> blockSet = new HashSet<>();
        
        for (BlockPos pos : BlockPos.betweenClosed(boundsMin, boundsMax)) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getRenderShape() != RenderShape.INVISIBLE) {
                allBlocks.add(pos.immutable());
                blockSet.add(pos.immutable());
            }
        }
        
        // Simple occlusion: for each column/row in view direction,
        // only the first block is visible
        if (absY > absX && absY > absZ) {
            // Looking mostly from top/bottom - cull by Y columns
            cullByColumn(level, allBlocks, blockSet, visibleBlocks, viewY > 0);
        } else if (absZ > absX) {
            // Looking mostly from front/back - cull by Z columns
            cullByColumnZ(level, allBlocks, blockSet, visibleBlocks, viewZ > 0);
        } else {
            // Looking mostly from left/right - cull by X columns
            cullByColumnX(level, allBlocks, blockSet, visibleBlocks, viewX > 0);
        }
        
        return visibleBlocks;
    }
    
    /**
     * Cull blocks by Y column (top-down or bottom-up view)
     */
    private void cullByColumn(net.createmod.catnip.levelWrappers.SchematicLevel level,
                              List<BlockPos> allBlocks, Set<BlockPos> blockSet,
                              Set<BlockPos> visible, boolean fromTop) {
        // Group blocks by X,Z column
        Map<Long, List<BlockPos>> columns = new HashMap<>();
        
        for (BlockPos pos : allBlocks) {
            long key = (((long)pos.getX()) << 32) | (pos.getZ() & 0xFFFFFFFFL);
            columns.computeIfAbsent(key, k -> new ArrayList<>()).add(pos);
        }
        
        // For each column, find the topmost/bottommost visible block
        for (List<BlockPos> column : columns.values()) {
            // Sort by Y
            column.sort(Comparator.comparingInt(BlockPos::getY));
            
            // Find first visible block from view direction
            if (fromTop) {
                // View from top - find highest block
                for (int i = column.size() - 1; i >= 0; i--) {
                    BlockPos pos = column.get(i);
                    if (isBlockVisibleFromDirection(level, pos, blockSet, 0, 1, 0)) {
                        visible.add(pos);
                        break;
                    }
                }
            } else {
                // View from bottom - find lowest block
                for (BlockPos pos : column) {
                    if (isBlockVisibleFromDirection(level, pos, blockSet, 0, -1, 0)) {
                        visible.add(pos);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Cull blocks by Z column (front-back view)
     */
    private void cullByColumnZ(net.createmod.catnip.levelWrappers.SchematicLevel level,
                               List<BlockPos> allBlocks, Set<BlockPos> blockSet,
                               Set<BlockPos> visible, boolean fromFront) {
        // Group blocks by X,Y column
        Map<Long, List<BlockPos>> columns = new HashMap<>();
        
        for (BlockPos pos : allBlocks) {
            long key = (((long)pos.getX()) << 32) | (pos.getY() & 0xFFFFFFFFL);
            columns.computeIfAbsent(key, k -> new ArrayList<>()).add(pos);
        }
        
        for (List<BlockPos> column : columns.values()) {
            column.sort(Comparator.comparingInt(BlockPos::getZ));
            
            if (fromFront) {
                for (int i = column.size() - 1; i >= 0; i--) {
                    BlockPos pos = column.get(i);
                    if (isBlockVisibleFromDirection(level, pos, blockSet, 0, 0, 1)) {
                        visible.add(pos);
                        break;
                    }
                }
            } else {
                for (BlockPos pos : column) {
                    if (isBlockVisibleFromDirection(level, pos, blockSet, 0, 0, -1)) {
                        visible.add(pos);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Cull blocks by X column (left-right view)
     */
    private void cullByColumnX(net.createmod.catnip.levelWrappers.SchematicLevel level,
                               List<BlockPos> allBlocks, Set<BlockPos> blockSet,
                               Set<BlockPos> visible, boolean fromLeft) {
        // Group blocks by Y,Z column
        Map<Long, List<BlockPos>> columns = new HashMap<>();
        
        for (BlockPos pos : allBlocks) {
            long key = (((long)pos.getY()) << 32) | (pos.getZ() & 0xFFFFFFFFL);
            columns.computeIfAbsent(key, k -> new ArrayList<>()).add(pos);
        }
        
        for (List<BlockPos> column : columns.values()) {
            column.sort(Comparator.comparingInt(BlockPos::getX));
            
            if (fromLeft) {
                for (BlockPos pos : column) {
                    if (isBlockVisibleFromDirection(level, pos, blockSet, -1, 0, 0)) {
                        visible.add(pos);
                        break;
                    }
                }
            } else {
                for (int i = column.size() - 1; i >= 0; i--) {
                    BlockPos pos = column.get(i);
                    if (isBlockVisibleFromDirection(level, pos, blockSet, 1, 0, 0)) {
                        visible.add(pos);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Check if a block is visible from a specific direction
     */
    private boolean isBlockVisibleFromDirection(net.createmod.catnip.levelWrappers.SchematicLevel level,
                                                 BlockPos pos, Set<BlockPos> blockSet,
                                                 int dirX, int dirY, int dirZ) {
        // Check if there's an opaque block in front of this block
        BlockPos adjacent = pos.offset(dirX, dirY, dirZ);
        
        // If adjacent is air, this block is visible
        if (level.getBlockState(adjacent).isAir()) {
            return true;
        }
        
        // If adjacent is not in our block set, it might be transparent
        if (!blockSet.contains(adjacent)) {
            return true;
        }
        
        // Block is occluded by adjacent block
        return false;
    }
    
    /**
     * Clear cached data
     */
    public void clear() {
        visibleBlocks.clear();
        lastCheckRotX = -1;
        lastCheckRotY = -1;
    }
    
    /**
     * Get number of visible blocks after culling
     */
    public int getVisibleCount() {
        return visibleBlocks.size();
    }
}
