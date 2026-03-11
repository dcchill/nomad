package net.create_nomad.init;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

import net.minecraft.world.WorldlyContainer;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CreateNomadCustomBackpackCapabilities {
	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		register(event, CreateNomadModBlockEntities.BROWN_BRASS_BACKPACK);
	}

	private static void register(RegisterCapabilitiesEvent event,
			net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<?>> holder) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, holder.get(), (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side));
	}
}
