package net.create_nomad.init;

import net.create_nomad.CreateNomadMod;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CreateNomadMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateNomadModItemProperties {
	@SubscribeEvent
	public static void registerItemProperties(FMLClientSetupEvent event) {
		event.enqueueWork(() -> ItemProperties.register(
				CreateNomadModItems.CHAINSAW.get(),
				ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "mining"),
				CreateNomadModItemProperties::getMiningAnimationPhase));
	}

	private static float getMiningAnimationPhase(ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level, LivingEntity entity, int seed) {
		if (entity == null || !entity.swinging || !isHeldByEntity(stack, entity)) {
			return 0.0F;
		}

		float phase = (entity.tickCount + (seed & 0x7) * 0.5F) * 1.15F;
		return (Mth.sin(phase) + 1.0F) * 0.5F;
	}

	private static boolean isHeldByEntity(ItemStack stack, LivingEntity entity) {
		return ItemStack.isSameItemSameComponents(entity.getMainHandItem(), stack)
				|| ItemStack.isSameItemSameComponents(entity.getOffhandItem(), stack);
	}
}
