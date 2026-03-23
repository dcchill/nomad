package net.create_nomad.network;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.util.BackpackRefillHighlightState;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BackpackRefillHighlightMessage {
    private final int slot;
    private final boolean fromTrackpack;

    public BackpackRefillHighlightMessage(int slot, boolean fromTrackpack) {
        this.slot = slot;
        this.fromTrackpack = fromTrackpack;
    }

    public BackpackRefillHighlightMessage(net.minecraft.network.FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readBoolean());
    }

    public void buffer(net.minecraft.network.FriendlyByteBuf buffer) {
        buffer.writeVarInt(slot);
        buffer.writeBoolean(fromTrackpack);
    }

    public static void handler(BackpackRefillHighlightMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (message.fromTrackpack)
                BackpackRefillHighlightState.markTrackpackSlot(message.slot);
            else
                BackpackRefillHighlightState.markBackpackSlot(message.slot);
        });
        context.setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CreateNomadMod.addNetworkMessage(BackpackRefillHighlightMessage.class, BackpackRefillHighlightMessage::buffer, BackpackRefillHighlightMessage::new, BackpackRefillHighlightMessage::handler));
    }
}
