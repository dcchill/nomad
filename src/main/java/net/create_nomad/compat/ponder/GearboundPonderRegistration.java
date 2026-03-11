package net.create_nomad.compat.ponder;

import net.createmod.ponder.foundation.PonderIndex;
import net.create_nomad.GearboundMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = GearboundMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GearboundPonderRegistration {
	@SubscribeEvent
	public static void onClientSetup(final FMLClientSetupEvent event) {
		if (!ModList.get().isLoaded("ponder")) {
			return;
		}
		event.enqueueWork(() -> PonderIndex.addPlugin(new GearboundPonderPlugin()));
	}
}
