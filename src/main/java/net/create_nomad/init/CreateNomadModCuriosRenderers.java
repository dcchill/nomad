package net.create_nomad.init;

import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import net.create_nomad.client.renderer.YellowBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.WhiteBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.RedBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.PurpleBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.PinkBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.OrangeBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.MagentaBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.LimeBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.LightGrayBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.LightBlueBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.GreenBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.GrayBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.CyanBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.BrownBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.BlueBrassBackpackCuriosRenderer;
import net.create_nomad.client.renderer.BlackBrassBackpackCuriosRenderer;

public class CreateNomadModCuriosRenderers {
	public static void registerRenderers(FMLClientSetupEvent event) {
		CuriosRendererRegistry.register(CreateNomadModItems.BROWN_BRASS_BACKPACK_ITEM.get(), BrownBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.BLACK_BRASS_BACKPACK_ITEM.get(), BlackBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.GRAY_BRASS_BACKPACK_ITEM.get(), GrayBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.LIGHT_GRAY_BRASS_BACKPACK_ITEM.get(), LightGrayBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.LIGHT_BLUE_BRASS_BACKPACK_ITEM.get(), LightBlueBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.BLUE_BRASS_BACKPACK_ITEM.get(), BlueBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.YELLOW_BRASS_BACKPACK_ITEM.get(), YellowBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.GREEN_BRASS_BACKPACK_ITEM.get(), GreenBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.ORANGE_BRASS_BACKPACK_ITEM.get(), OrangeBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.RED_BRASS_BACKPACK_ITEM.get(), RedBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.PINK_BRASS_BACKPACK_ITEM.get(), PinkBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.PURPLE_BRASS_BACKPACK_ITEM.get(), PurpleBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.MAGENTA_BRASS_BACKPACK_ITEM.get(), MagentaBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.WHITE_BRASS_BACKPACK_ITEM.get(), WhiteBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.LIME_BRASS_BACKPACK_ITEM.get(), LimeBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(CreateNomadModItems.CYAN_BRASS_BACKPACK_ITEM.get(), CyanBrassBackpackCuriosRenderer::new);
	}
}