package net.create_nomad.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.create_nomad.CreateNomadMod;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record ToolbeltUtilitySelectionMessage(boolean utilitySelected) implements CustomPacketPayload {
    public static final String PLAYER_TAG = "gearboundToolbeltUtilitySelected";
    public static final Type<ToolbeltUtilitySelectionMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "toolbelt_utility_selected"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolbeltUtilitySelectionMessage> STREAM_CODEC = StreamCodec.of(ToolbeltUtilitySelectionMessage::write, ToolbeltUtilitySelectionMessage::read);

    private static void write(FriendlyByteBuf buffer, ToolbeltUtilitySelectionMessage message) {
        buffer.writeBoolean(message.utilitySelected);
    }

    private static ToolbeltUtilitySelectionMessage read(FriendlyByteBuf buffer) {
        return new ToolbeltUtilitySelectionMessage(buffer.readBoolean());
    }

    @Override
    public Type<ToolbeltUtilitySelectionMessage> type() {
        return TYPE;
    }

    public static void handle(ToolbeltUtilitySelectionMessage message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }

        context.enqueueWork(() -> context.player().getPersistentData().putBoolean(PLAYER_TAG, message.utilitySelected))
                .exceptionally(error -> {
                    context.connection().disconnect(Component.literal(error.getMessage()));
                    return null;
                });
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        CreateNomadMod.addNetworkMessage(TYPE, STREAM_CODEC, ToolbeltUtilitySelectionMessage::handle);
    }
}
