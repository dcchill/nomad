package net.create_nomad.init;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.client.gui.ToolbeltScreen;

@EventBusSubscriber(modid = CreateNomadMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CreateNomadToolbeltScreens {
	private CreateNomadToolbeltScreens() {
	}

	@SubscribeEvent
	public static void register(RegisterMenuScreensEvent event) {
		event.register(CreateNomadToolbeltMenu.TOOLBELT_MENU.get(), ToolbeltScreen::new);
	}
}
