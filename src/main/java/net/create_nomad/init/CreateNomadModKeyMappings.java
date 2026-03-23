/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.create_nomad.init;

import org.lwjgl.glfw.GLFW;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ClientTickEvent;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import net.create_nomad.network.OpenBackpackMessage;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class CreateNomadModKeyMappings {
	public static final KeyMapping OPEN_BACKPACK = new KeyMapping("key.create_nomad.open_backpack", GLFW.GLFW_KEY_B, "key.categories.gearbound") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				CreateNomadMod.sendToServer(new OpenBackpackMessage(0, 0));
				OpenBackpackMessage.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(OPEN_BACKPACK);
	}

	@EventBusSubscriber({Dist.CLIENT})
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			if (Minecraft.getInstance().screen == null) {
				OPEN_BACKPACK.consumeClick();
			}
		}
	}
}