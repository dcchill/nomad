package net.create_nomad.client.renderer.schematic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import java.util.ArrayList;
import java.util.List;

/**
 * Level of Detail system for rendering large schematics efficiently.
 * Skips blocks when they're too small to be visible at current zoom level.
 */
public class LodManager {
    
    private final List<BlockPos> lodPositions = new ArrayList<>();
    private int currentLodLevel = 0;
    private float currentZoom = 1.0f;
    
    /**
     * Calculate which blocks to render based on zoom level
     * @param allPositions All block positions in schematic
     * @param zoom Current zoom level (0.0 - 2.0)
     * @param schematicSize Size of the schematic
     * @return Positions to render at this LOD
     */
    public List<BlockPos> getPositionsForLod(List<BlockPos> allPositions, float zoom, Vec3i schematicSize) {
        // For large schematics, always apply some LOD
        // Calculate LOD level based on zoom and size
        int targetLod = calculateLodLevel(zoom, allPositions.size());
        
        if (targetLod != currentLodLevel || lodPositions.isEmpty()) {
            currentLodLevel = targetLod;
            currentZoom = zoom;
            rebuildLod(allPositions, schematicSize);
        }
        
        return lodPositions;
    }
    
    private int calculateLodLevel(float zoom, int blockCount) {
        // LOD 0: all blocks (zoom >= 1.0 OR blockCount < 10000)
        // LOD 1: 50% of blocks (zoom >= 0.5 OR blockCount >= 10000)
        // LOD 2: 25% of blocks (zoom >= 0.25 OR blockCount >= 50000)
        // LOD 3: 12.5% of blocks (zoom < 0.25 OR blockCount >= 100000)
        
        if (blockCount >= 100000 || zoom < 0.25f) return 3;
        if (blockCount >= 50000 || zoom < 0.5f) return 2;
        if (blockCount >= 10000 || zoom < 1.0f) return 1;
        return 0;
    }
    
    private void rebuildLod(List<BlockPos> allPositions, Vec3i schematicSize) {
        lodPositions.clear();

        if (currentLodLevel == 0) {
            lodPositions.addAll(allPositions);
            return;
        }

        // Calculate stride based on LOD level
        // LOD 1: stride 2 (every 2nd block in each axis = ~12.5% of blocks)
        // LOD 2: stride 3 (every 3rd block in each axis = ~3.7% of blocks)
        // LOD 3: stride 4 (every 4th block in each axis = ~1.6% of blocks)
        int stride = currentLodLevel + 1;
        
        // Use offset to avoid always sampling the same grid positions
        int offset = 0;

        for (BlockPos pos : allPositions) {
            // Sample blocks at regular intervals
            if ((pos.getX() + offset) % stride == 0 &&
                (pos.getY() + offset) % stride == 0 &&
                (pos.getZ() + offset) % stride == 0) {
                lodPositions.add(pos);
            }
        }
        
        System.out.println("[LodManager] LOD " + currentLodLevel + " (stride=" + stride + "): " + allPositions.size() + " -> " + lodPositions.size());
    }
    
    /**
     * Get current LOD level (0 = highest detail)
     */
    public int getCurrentLodLevel() {
        return currentLodLevel;
    }
    
    /**
     * Get number of blocks at current LOD
     */
    public int getBlockCount() {
        return lodPositions.size();
    }
    
    /**
     * Clear cached LOD data
     */
    public void clear() {
        lodPositions.clear();
        currentLodLevel = 0;
        currentZoom = 1.0f;
    }
}
