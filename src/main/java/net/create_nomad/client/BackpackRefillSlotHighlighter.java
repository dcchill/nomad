package net.create_nomad.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import net.create_nomad.GearboundMod;
import net.create_nomad.util.BackpackRefillHighlightState;

public final class BackpackRefillSlotHighlighter {
    private BackpackRefillSlotHighlighter() {
    }

    @EventBusSubscriber(modid = GearboundMod.MODID, value = Dist.CLIENT)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            BackpackRefillHighlightState.tickDown();
        }

        @SubscribeEvent
        public static void renderHotbarHighlights(RenderGuiLayerEvent.Post event) {
            if (!event.getName().equals(ResourceLocation.withDefaultNamespace("hotbar"))) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null) {
                return;
            }

            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();
            int hotbarLeft = screenWidth / 2 - 91;
            int hotbarTop = screenHeight - 22;

            for (int slot = 0; slot < BackpackRefillHighlightState.HOTBAR_SLOTS; slot++) {
                int backpackTimer = BackpackRefillHighlightState.getBackpackTimer(slot);
                int trackpackTimer = BackpackRefillHighlightState.getTrackpackTimer(slot);
                int timer = Math.max(backpackTimer, trackpackTimer);
                if (timer <= 0)
                    continue;

                float progress = timer / (float) BackpackRefillHighlightState.HIGHLIGHT_TICKS;
                int alpha = 40 + (int) (90 * progress);
                int baseColor = trackpackTimer > 0 ? 0xffa347 : 0x80f0ee;
                int color = (alpha << 24) | baseColor;
                int x = hotbarLeft + slot * 20;
                int y = hotbarTop + 1;

                event.getGuiGraphics().fill(RenderType.guiOverlay(), x, y, x + 20, y + 20, color);
            }
        }
    }
}
