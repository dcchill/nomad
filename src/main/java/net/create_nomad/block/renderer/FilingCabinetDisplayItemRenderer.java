package net.create_nomad.block.renderer;

import software.bernie.geckolib.renderer.GeoItemRenderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import net.create_nomad.block.model.FilingCabinetDisplayModel;
import net.create_nomad.block.display.FilingCabinetDisplayItem;

public class FilingCabinetDisplayItemRenderer extends GeoItemRenderer<FilingCabinetDisplayItem> {
	public FilingCabinetDisplayItemRenderer() {
		super(new FilingCabinetDisplayModel());
	}

	@Override
	public RenderType getRenderType(FilingCabinetDisplayItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}