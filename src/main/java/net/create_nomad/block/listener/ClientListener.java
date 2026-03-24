package net.create_nomad.block.listener;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.block.entity.BlockEntityType;

import net.create_nomad.init.CreateNomadModBlockEntities;
import net.create_nomad.block.renderer.FilingCabinetTileRenderer;
import net.create_nomad.block.entity.FilingCabinetTileEntity;
import net.create_nomad.CreateNomadMod;

@EventBusSubscriber(modid = CreateNomadMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientListener {
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer((BlockEntityType<FilingCabinetTileEntity>) CreateNomadModBlockEntities.FILING_CABINET.get(), context -> new FilingCabinetTileRenderer());
	}
}