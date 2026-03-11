package net.create_nomad.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.create_nomad.CreateNomadMod;

@EventBusSubscriber(modid = CreateNomadMod.MODID)
public class BackpackDyeCraftingHandler {
	private static final TagKey<Item> BACKPACKS_TAG = TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "backpacks"));

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

			var customData = ingredientStack.get(DataComponents.CUSTOM_DATA);
			if (customData == null)
				continue;

			craftedStack.set(DataComponents.CUSTOM_DATA, customData);
			return;
		}
	}
}
