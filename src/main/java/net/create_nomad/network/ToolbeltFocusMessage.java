package net.create_nomad.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.create_nomad.CreateNomadMod;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record ToolbeltFocusMessage(int eventType, int pressedms) implements CustomPacketPayload {
	public static final Type<ToolbeltFocusMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "key_toolbelt_focus"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ToolbeltFocusMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, ToolbeltFocusMessage message) -> {
		buffer.writeInt(message.eventType);
		buffer.writeInt(message.pressedms);
	}, (RegistryFriendlyByteBuf buffer) -> new ToolbeltFocusMessage(buffer.readInt(), buffer.readInt()));

	@Override
	public Type<ToolbeltFocusMessage> type() {
		return TYPE;
	}

	public static void handleData(final ToolbeltFocusMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CreateNomadMod.addNetworkMessage(ToolbeltFocusMessage.TYPE, ToolbeltFocusMessage.STREAM_CODEC, ToolbeltFocusMessage::handleData);
	}
}