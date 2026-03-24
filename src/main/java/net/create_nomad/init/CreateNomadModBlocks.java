/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.create_nomad.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

import net.create_nomad.block.FilingCabinetBlock;
import net.create_nomad.block.BrownBrassBackpackBlock;
import net.create_nomad.CreateNomadMod;

public class CreateNomadModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(CreateNomadMod.MODID);
	public static final DeferredBlock<Block> BROWN_BRASS_BACKPACK = REGISTRY.register("brown_brass_backpack", BrownBrassBackpackBlock::new);
	public static final DeferredBlock<Block> FILING_CABINET = REGISTRY.register("filing_cabinet", FilingCabinetBlock::new);
	// Start of user code block custom blocks
	// End of user code block custom blocks
}