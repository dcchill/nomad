/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.create_nomad.init;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.create_nomad.client.model.Modelharpoon;
import net.create_nomad.client.model.Modelbackpack;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class CreateNomadModModels {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modelharpoon.LAYER_LOCATION, Modelharpoon::createBodyLayer);
		event.registerLayerDefinition(Modelbackpack.LAYER_LOCATION, Modelbackpack::createBodyLayer);
	}
}