package net.create_nomad.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import net.create_nomad.item.JackhammerItem;

public class JackhammerItemModel extends GeoModel<JackhammerItem> {
	@Override
	public ResourceLocation getAnimationResource(JackhammerItem animatable) {
		return ResourceLocation.parse("create_nomad:animations/jackhammer.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(JackhammerItem animatable) {
		return ResourceLocation.parse("create_nomad:geo/jackhammer.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(JackhammerItem animatable) {
		return ResourceLocation.parse("create_nomad:textures/item/jackhammer_texture.png");
	}
}