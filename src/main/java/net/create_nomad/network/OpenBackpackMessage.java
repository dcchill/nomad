package net.create_nomad.network;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.procedures.OpenBackpackOnKeyPressedProcedure;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class OpenBackpackMessage {
	private final int eventType;
	private final int pressedms;

	public OpenBackpackMessage(int eventType, int pressedms) {
		this.eventType = eventType;
		this.pressedms = pressedms;
	}

	public OpenBackpackMessage(net.minecraft.network.FriendlyByteBuf buffer) {
		this(buffer.readInt(), buffer.readInt());
	}

	public void buffer(net.minecraft.network.FriendlyByteBuf buffer) {
		buffer.writeInt(eventType);
		buffer.writeInt(pressedms);
	}

	public static void handler(OpenBackpackMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> pressAction(context.getSender(), message.eventType, message.pressedms));
		context.setPacketHandled(true);
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		if (entity == null) return;
		Level world = entity.level();
		if (!world.hasChunkAt(entity.blockPosition()))
			return;
		if (type == 0 && entity instanceof ServerPlayer serverPlayer) {
			OpenBackpackOnKeyPressedProcedure.execute(serverPlayer);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> CreateNomadMod.addNetworkMessage(OpenBackpackMessage.class, OpenBackpackMessage::buffer, OpenBackpackMessage::new, OpenBackpackMessage::handler));
	}
}
