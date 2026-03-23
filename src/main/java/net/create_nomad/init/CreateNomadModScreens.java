/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.create_nomad.init;

import net.minecraftforge.client.event.RegisterMenuScreensEvent;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.create_nomad.client.gui.BrassBackpackGUIScreen;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateNomadModScreens {
	@SubscribeEvent
	public static void clientLoad(RegisterMenuScreensEvent event) {
		event.register(CreateNomadModMenus.BRASS_BACKPACK_GUI.get(), BrassBackpackGUIScreen::new);
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}