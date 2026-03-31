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
public record ToggleConstructinatorRenderMessage(int eventType, int pressedms) implements CustomPacketPayload {
	public static final Type<ToggleConstructinatorRenderMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "key_toggle_constructinator_render"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ToggleConstructinatorRenderMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, ToggleConstructinatorRenderMessage message) -> {
		buffer.writeInt(message.eventType);
		buffer.writeInt(message.pressedms);
	}, (RegistryFriendlyByteBuf buffer) -> new ToggleConstructinatorRenderMessage(buffer.readInt(), buffer.readInt()));

	@Override
	public Type<ToggleConstructinatorRenderMessage> type() {
		return TYPE;
	}

	public static void handleData(final ToggleConstructinatorRenderMessage message, final IPayloadContext context) {
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
		CreateNomadMod.addNetworkMessage(ToggleConstructinatorRenderMessage.TYPE, ToggleConstructinatorRenderMessage.STREAM_CODEC, ToggleConstructinatorRenderMessage::handleData);
	}
}