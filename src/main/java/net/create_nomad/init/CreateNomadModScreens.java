/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.create_nomad.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.create_nomad.client.gui.ToolbeltGuiScreen;
import net.create_nomad.client.gui.BrassBackpackGUIScreen;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateNomadModScreens {
	@SubscribeEvent
	public static void clientLoad(RegisterMenuScreensEvent event) {
		event.register(CreateNomadModMenus.BRASS_BACKPACK_GUI.get(), BrassBackpackGUIScreen::new);
		event.register(CreateNomadModMenus.TOOLBELT_GUI.get(), ToolbeltGuiScreen::new);
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}