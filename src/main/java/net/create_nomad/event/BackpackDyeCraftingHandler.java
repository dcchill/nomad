package net.create_nomad.event;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.create_nomad.CreateNomadMod;

@EventBusSubscriber(modid = CreateNomadMod.MODID)
public class BackpackDyeCraftingHandler {
	private static final TagKey<Item> BACKPACKS_TAG = TagKey.create(net.minecraft.core.registries.Registries.ITEM, new ResourceLocation(CreateNomadMod.MODID, "backpacks"));

	private BackpackDyeCraftingHandler() {
	}

	@SubscribeEvent
	public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
		ItemStack craftedStack = event.getCrafting();
		if (!craftedStack.is(BACKPACKS_TAG))
			return;

		for (int slot = 0; slot < event.getInventory().getContainerSize(); slot++) {
			ItemStack ingredientStack = event.getInventory().getItem(slot);
			if (!ingredientStack.is(BACKPACKS_TAG))
				continue;

			if (!ingredientStack.hasTag())
				continue;

			craftedStack.setTag(ingredientStack.getTag().copy());
			return;
		}
	}
}
