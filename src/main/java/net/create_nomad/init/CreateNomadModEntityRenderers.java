/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.create_nomad.init;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.create_nomad.client.renderer.HarpoonRenderer;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateNomadModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(CreateNomadModEntities.HARPOON.get(), HarpoonRenderer::new);
	}
}