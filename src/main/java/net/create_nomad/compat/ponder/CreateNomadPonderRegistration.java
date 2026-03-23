package net.create_nomad.compat.ponder;

import net.createmod.ponder.foundation.PonderIndex;
import net.create_nomad.CreateNomadMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CreateNomadMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateNomadPonderRegistration {
	@SubscribeEvent
	public static void onClientSetup(final FMLClientSetupEvent event) {
		if (!ModList.get().isLoaded("ponder")) {
			return;
		}
		event.enqueueWork(() -> PonderIndex.addPlugin(new CreateNomadPonderPlugin()));
	}
}
