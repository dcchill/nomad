package net.create_nomad.init;

import software.bernie.geckolib.animatable.GeoItem;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

import net.create_nomad.item.HarpoonGunItem;
import net.create_nomad.item.JackhammerItem;

@EventBusSubscriber
public class ItemAnimationFactory {
	@SubscribeEvent
	public static void animatedItems(PlayerTickEvent.Post event) {
		ItemStack mainhandItem = event.getEntity().getMainHandItem().copy();
		ItemStack offhandItem = event.getEntity().getOffhandItem().copy();
		if (mainhandItem.getItem() instanceof GeoItem || offhandItem.getItem() instanceof GeoItem) {
			applyAnimation(event.getEntity().getMainHandItem(), mainhandItem, event.getEntity().level().isClientSide());
			applyAnimation(event.getEntity().getOffhandItem(), offhandItem, event.getEntity().level().isClientSide());
		}
	}

	private static void applyAnimation(ItemStack liveStack, ItemStack copiedStack, boolean isClientSide) {
		String animation = copiedStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
		if (animation.isEmpty()) {
			return;
		}

		CustomData.update(DataComponents.CUSTOM_DATA, liveStack, tag -> tag.putString("geckoAnim", ""));
		if (!isClientSide) {
			return;
		}

		if (liveStack.getItem() instanceof JackhammerItem jackhammerItem) {
			jackhammerItem.animationprocedure = animation;
		} else if (liveStack.getItem() instanceof HarpoonGunItem harpoonGunItem) {
			harpoonGunItem.animationprocedure = animation;
		}
	}
}
