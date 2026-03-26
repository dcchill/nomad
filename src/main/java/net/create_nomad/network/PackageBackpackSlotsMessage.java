package net.create_nomad.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.world.inventory.BrassBackpackGUIMenu;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record PackageBackpackSlotsMessage() implements CustomPacketPayload {
    public static final Type<PackageBackpackSlotsMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "package_backpack_slots"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PackageBackpackSlotsMessage> STREAM_CODEC =
            StreamCodec.of((RegistryFriendlyByteBuf buffer, PackageBackpackSlotsMessage message) -> {
            }, buffer -> new PackageBackpackSlotsMessage());

    @Override
    public Type<PackageBackpackSlotsMessage> type() {
        return TYPE;
    }

    public static void handleData(final PackageBackpackSlotsMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> pressAction(context.player())).exceptionally(e -> {
                context.connection().disconnect(Component.literal(e.getMessage()));
                return null;
            });
        }
    }

    public static void pressAction(Player entity) {
        if (entity instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu instanceof BrassBackpackGUIMenu backpackMenu) {
            backpackMenu.packageInputSlotsIntoRandomCreatePackage(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        CreateNomadMod.addNetworkMessage(PackageBackpackSlotsMessage.TYPE, PackageBackpackSlotsMessage.STREAM_CODEC, PackageBackpackSlotsMessage::handleData);
    }
}
