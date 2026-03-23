package net.create_nomad.init;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.capabilities.Capabilities;
import net.minecraftforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import net.minecraft.world.WorldlyContainer;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CreateNomadCustomBackpackCapabilities {
	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		register(event, CreateNomadModBlockEntities.BROWN_BRASS_BACKPACK);
	}

	private static void register(RegisterCapabilitiesEvent event,
			net.minecraftforge.registries.DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<?>> holder) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, holder.get(), (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side));
	}
}
