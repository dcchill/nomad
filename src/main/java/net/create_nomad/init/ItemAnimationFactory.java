package net.create_nomad.init;

import software.bernie.geckolib.animatable.GeoItem;

import net.minecraftforge.event.tick.PlayerTickEvent;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import net.create_nomad.item.JackhammerItem;
import net.create_nomad.item.HarpoonGunItem;

@EventBusSubscriber
public class ItemAnimationFactory {
	@SubscribeEvent
	public static void animatedItems(PlayerTickEvent.Post event) {
		String animation = "";
		ItemStack mainhandItem = event.getEntity().getMainHandItem().copy();
		ItemStack offhandItem = event.getEntity().getOffhandItem().copy();
		if (mainhandItem.getItem() instanceof GeoItem || offhandItem.getItem() instanceof GeoItem) {
			if (mainhandItem.getItem() instanceof JackhammerItem animatable) {
				animation = getAnimationTag(mainhandItem).getString("geckoAnim");
				if (!animation.isEmpty()) {
					getAnimationTag(event.getEntity().getMainHandItem()).putString("geckoAnim", "");
					if (event.getEntity().level().isClientSide()) {
						((JackhammerItem) event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
					}
				}
			}
			if (offhandItem.getItem() instanceof JackhammerItem animatable) {
				animation = getAnimationTag(offhandItem).getString("geckoAnim");
				if (!animation.isEmpty()) {
					getAnimationTag(event.getEntity().getOffhandItem()).putString("geckoAnim", "");
					if (event.getEntity().level().isClientSide()) {
						((JackhammerItem) event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
					}
				}
			}
			if (mainhandItem.getItem() instanceof HarpoonGunItem animatable) {
				animation = getAnimationTag(mainhandItem).getString("geckoAnim");
				if (!animation.isEmpty()) {
					getAnimationTag(event.getEntity().getMainHandItem()).putString("geckoAnim", "");
					if (event.getEntity().level().isClientSide()) {
						((HarpoonGunItem) event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
					}
				}
			}
			if (offhandItem.getItem() instanceof HarpoonGunItem animatable) {
				animation = getAnimationTag(offhandItem).getString("geckoAnim");
				if (!animation.isEmpty()) {
					getAnimationTag(event.getEntity().getOffhandItem()).putString("geckoAnim", "");
					if (event.getEntity().level().isClientSide()) {
						((HarpoonGunItem) event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
					}
				}
			}
		}
	}

	private static CompoundTag getAnimationTag(ItemStack stack) {
		return stack.getOrCreateTag();
	}
}
