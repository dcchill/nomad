package net.create_nomad.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.item.ToolbeltItem;
import net.create_nomad.util.ToolbeltDataUtils;

@EventBusSubscriber(modid = CreateNomadMod.MODID, value = Dist.CLIENT)
public final class ToolbeltHandRenderer {
    private ToolbeltHandRenderer() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !ToolbeltHotbarOverlay.isUtilitySelected()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        ItemStack toolbelt = ToolbeltItem.findEquippedToolbelt(player);
        if (toolbelt.isEmpty()) {
            return;
        }

        ItemStack selectedTool = ToolbeltDataUtils.getSelectedStack(toolbelt, player.level().registryAccess());
        if (selectedTool.isEmpty()) {
            return;
        }

        event.setCanceled(true);
        minecraft.getItemRenderer().renderStatic(player, selectedTool, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, event.getPoseStack(), event.getMultiBufferSource(), player.level(), event.getPackedLight(), OverlayTexture.NO_OVERLAY, player.getId());
    }
}
