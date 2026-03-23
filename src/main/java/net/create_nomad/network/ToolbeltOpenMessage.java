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
public record ToolbeltOpenMessage(int eventType, int pressedms) implements CustomPacketPayload {
	public static final Type<ToolbeltOpenMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "key_toolbelt_open"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ToolbeltOpenMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, ToolbeltOpenMessage message) -> {
		buffer.writeInt(message.eventType);
		buffer.writeInt(message.pressedms);
	}, (RegistryFriendlyByteBuf buffer) -> new ToolbeltOpenMessage(buffer.readInt(), buffer.readInt()));

	@Override
	public Type<ToolbeltOpenMessage> type() {
		return TYPE;
	}

	public static void handleData(final ToolbeltOpenMessage message, final IPayloadContext context) {
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
		CreateNomadMod.addNetworkMessage(ToolbeltOpenMessage.TYPE, ToolbeltOpenMessage.STREAM_CODEC, ToolbeltOpenMessage::handleData);
	}
}