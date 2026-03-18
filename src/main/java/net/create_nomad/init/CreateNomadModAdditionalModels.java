package net.create_nomad.init;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.item.renderer.SanderItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = CreateNomadMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateNomadModAdditionalModels {
	@SubscribeEvent
	public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
		event.register(SanderItemRenderer.ACCELERATOR_MODEL_ID);
	}
}
