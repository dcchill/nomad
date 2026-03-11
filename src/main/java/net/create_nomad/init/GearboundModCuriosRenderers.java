package net.create_nomad.init;

import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

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

public class GearboundModCuriosRenderers {
	public static void registerRenderers(FMLClientSetupEvent event) {
		CuriosRendererRegistry.register(GearboundModItems.BROWN_BRASS_BACKPACK_ITEM.get(), BrownBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.BLACK_BRASS_BACKPACK_ITEM.get(), BlackBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.GRAY_BRASS_BACKPACK_ITEM.get(), GrayBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.LIGHT_GRAY_BRASS_BACKPACK_ITEM.get(), LightGrayBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.LIGHT_BLUE_BRASS_BACKPACK_ITEM.get(), LightBlueBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.BLUE_BRASS_BACKPACK_ITEM.get(), BlueBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.YELLOW_BRASS_BACKPACK_ITEM.get(), YellowBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.GREEN_BRASS_BACKPACK_ITEM.get(), GreenBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.ORANGE_BRASS_BACKPACK_ITEM.get(), OrangeBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.RED_BRASS_BACKPACK_ITEM.get(), RedBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.PINK_BRASS_BACKPACK_ITEM.get(), PinkBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.PURPLE_BRASS_BACKPACK_ITEM.get(), PurpleBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.MAGENTA_BRASS_BACKPACK_ITEM.get(), MagentaBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.WHITE_BRASS_BACKPACK_ITEM.get(), WhiteBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.LIME_BRASS_BACKPACK_ITEM.get(), LimeBrassBackpackCuriosRenderer::new);
		CuriosRendererRegistry.register(GearboundModItems.CYAN_BRASS_BACKPACK_ITEM.get(), CyanBrassBackpackCuriosRenderer::new);
	}
}