/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.create_nomad.init;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.DeferredHolder;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.capabilities.Capabilities;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.registries.BuiltInRegistries;

import net.create_nomad.block.entity.BrownBrassBackpackBlockEntity;
import net.create_nomad.CreateNomadMod;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CreateNomadModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CreateNomadMod.MODID);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> BROWN_BRASS_BACKPACK = register("brown_brass_backpack", CreateNomadModBlocks.BROWN_BRASS_BACKPACK, BrownBrassBackpackBlockEntity::new);

	// Start of user code block custom block entities
	// End of user code block custom block entities
	private static DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> register(String registryname, DeferredHolder<Block, Block> block, BlockEntityType.BlockEntitySupplier<?> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BROWN_BRASS_BACKPACK.get(), (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side));
	}
}