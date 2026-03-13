package net.create_nomad.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import net.create_nomad.item.HarpoonGunItem;

public class HarpoonGunItemModel extends GeoModel<HarpoonGunItem> {
	@Override
	public ResourceLocation getAnimationResource(HarpoonGunItem animatable) {
		return ResourceLocation.parse("create_nomad:animations/harpoon_gun.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(HarpoonGunItem animatable) {
		return ResourceLocation.parse("create_nomad:geo/harpoon_gun.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(HarpoonGunItem animatable) {
		return ResourceLocation.parse("create_nomad:textures/item/harpoon_texture.png");
	}
}