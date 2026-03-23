package net.create_nomad.network;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.client.Minecraft;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.init.CreateNomadModMenus;
import net.create_nomad.init.CreateNomadModScreens;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MenuStateUpdateMessage {
	private final int elementType;
	private final String name;
	private final Object elementState;

	public MenuStateUpdateMessage(int elementType, String name, Object elementState) {
		this.elementType = elementType;
		this.name = name;
		this.elementState = elementState;
	}

	public MenuStateUpdateMessage(net.minecraft.network.FriendlyByteBuf buffer) {
		this.elementType = buffer.readInt();
		this.name = buffer.readUtf();
		this.elementState = this.elementType == 1 ? buffer.readBoolean() : buffer.readUtf();
	}

	public void buffer(net.minecraft.network.FriendlyByteBuf buffer) {
		buffer.writeInt(elementType);
		buffer.writeUtf(name);
		if (elementType == 1) {
			buffer.writeBoolean((boolean) elementState);
		} else {
			buffer.writeUtf(String.valueOf(elementState));
		}
	}

	public static void handler(MenuStateUpdateMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		if (message.name.length() > 256 || message.elementState instanceof String string && string.length() > 8192) {
			context.setPacketHandled(true);
			return;
		}
		context.enqueueWork(() -> {
			if (context.getSender() != null) {
				if (context.getSender().containerMenu instanceof CreateNomadModMenus.MenuAccessor menu) {
					menu.getMenuState().put(message.elementType + ":" + message.name, message.elementState);
				}
			} else if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof CreateNomadModMenus.MenuAccessor menu) {
				menu.getMenuState().put(message.elementType + ":" + message.name, message.elementState);
				if (Minecraft.getInstance().screen instanceof CreateNomadModScreens.ScreenAccessor accessor) {
					accessor.updateMenuState(message.elementType, message.name, message.elementState);
				}
			}
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> CreateNomadMod.addNetworkMessage(MenuStateUpdateMessage.class, MenuStateUpdateMessage::buffer, MenuStateUpdateMessage::new, MenuStateUpdateMessage::handler));
	}
}
