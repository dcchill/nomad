package net.create_nomad.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import net.create_nomad.block.entity.FilingCabinetTileEntity;

public class FilingCabinetBlockModel extends GeoModel<FilingCabinetTileEntity> {
	@Override
	public ResourceLocation getAnimationResource(FilingCabinetTileEntity animatable) {
		return ResourceLocation.parse("create_nomad:animations/filing_cabinet.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(FilingCabinetTileEntity animatable) {
		return ResourceLocation.parse("create_nomad:geo/filing_cabinet.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FilingCabinetTileEntity animatable) {
		return ResourceLocation.parse("create_nomad:textures/block/filing_cabinet_texture.png");
	}
}