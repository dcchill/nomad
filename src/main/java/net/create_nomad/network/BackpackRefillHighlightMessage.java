package net.create_nomad.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.create_nomad.GearboundMod;
import net.create_nomad.util.BackpackRefillHighlightState;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record BackpackRefillHighlightMessage(int slot, boolean fromTrackpack) implements CustomPacketPayload {
    public static final Type<BackpackRefillHighlightMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GearboundMod.MODID, "backpack_refill_highlight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackRefillHighlightMessage> STREAM_CODEC = StreamCodec.of(BackpackRefillHighlightMessage::write, BackpackRefillHighlightMessage::read);

    private static void write(FriendlyByteBuf buffer, BackpackRefillHighlightMessage message) {
        buffer.writeVarInt(message.slot);
        buffer.writeBoolean(message.fromTrackpack);
    }

    private static BackpackRefillHighlightMessage read(FriendlyByteBuf buffer) {
        return new BackpackRefillHighlightMessage(buffer.readVarInt(), buffer.readBoolean());
    }

    @Override
    public Type<BackpackRefillHighlightMessage> type() {
        return TYPE;
    }

    public static void handle(final BackpackRefillHighlightMessage message, final IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }

        context.enqueueWork(() -> {
            if (message.fromTrackpack)
                BackpackRefillHighlightState.markTrackpackSlot(message.slot);
            else
                BackpackRefillHighlightState.markBackpackSlot(message.slot);
        });
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        GearboundMod.addNetworkMessage(TYPE, STREAM_CODEC, BackpackRefillHighlightMessage::handle);
    }
}
