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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.item.ToolbeltItem;
import net.create_nomad.util.ToolbeltDataUtils;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record ToolbeltSelectionMessage(int selectedSlot) implements CustomPacketPayload {
    public static final Type<ToolbeltSelectionMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "toolbelt_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolbeltSelectionMessage> STREAM_CODEC = StreamCodec.of(ToolbeltSelectionMessage::write, ToolbeltSelectionMessage::read);

    private static void write(FriendlyByteBuf buffer, ToolbeltSelectionMessage message) {
        buffer.writeVarInt(message.selectedSlot);
    }

    private static ToolbeltSelectionMessage read(FriendlyByteBuf buffer) {
        return new ToolbeltSelectionMessage(buffer.readVarInt());
    }

    @Override
    public Type<ToolbeltSelectionMessage> type() {
        return TYPE;
    }

    public static void handle(ToolbeltSelectionMessage message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }

        context.enqueueWork(() -> {
            if (!(context.player().level() instanceof ServerLevel)) {
                return;
            }

            ItemStack equippedToolbelt = ToolbeltItem.findEquippedToolbelt(context.player());
            if (equippedToolbelt.isEmpty()) {
                return;
            }

            ToolbeltDataUtils.setSelectedSlot(equippedToolbelt, message.selectedSlot);
        }).exceptionally(error -> {
            context.connection().disconnect(Component.literal(error.getMessage()));
            return null;
        });
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        CreateNomadMod.addNetworkMessage(TYPE, STREAM_CODEC, ToolbeltSelectionMessage::handle);
    }
}
