package net.create_nomad.block.renderer;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import net.create_nomad.block.model.FilingCabinetBlockModel;
import net.create_nomad.block.entity.FilingCabinetTileEntity;

public class FilingCabinetTileRenderer extends GeoBlockRenderer<FilingCabinetTileEntity> {
	public FilingCabinetTileRenderer() {
		super(new FilingCabinetBlockModel());
	}

	@Override
	public RenderType getRenderType(FilingCabinetTileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}