package net.create_nomad.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import net.create_nomad.item.ConstructinatorItem;

public class ConstructinatorItemModel extends GeoModel<ConstructinatorItem> {
	@Override
	public ResourceLocation getAnimationResource(ConstructinatorItem animatable) {
		return ResourceLocation.parse("create_nomad:animations/constructinator.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ConstructinatorItem animatable) {
		return ResourceLocation.parse("create_nomad:geo/constructinator.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ConstructinatorItem animatable) {
		return ResourceLocation.parse("create_nomad:textures/item/constructinator_texture.png");
	}
}