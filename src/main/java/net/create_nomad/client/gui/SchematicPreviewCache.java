package net.create_nomad.client.gui;

import com.simibubi.create.content.schematics.client.SchematicRenderer;
import net.createmod.catnip.levelWrappers.SchematicLevel;

import java.util.HashMap;
import java.util.Map;

public final class SchematicPreviewCache {

    private static final Map<String, CachedPreview> CACHE = new HashMap<>();

    private SchematicPreviewCache() {}

    public static CachedPreview get(String file) {
        return CACHE.get(file);
    }

    public static void put(String file, CachedPreview preview) {
        CACHE.put(file, preview);
    }

    public static void clear(String file) {
        CACHE.remove(file);
    }

    public static final class CachedPreview {
        public final SchematicLevel level;
        public final SchematicRenderer renderer;
        public final int width;
        public final int height;
        public final int depth;

        public CachedPreview(SchematicLevel level, SchematicRenderer renderer, int width, int height, int depth) {
            this.level = level;
            this.renderer = renderer;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }
    }
}