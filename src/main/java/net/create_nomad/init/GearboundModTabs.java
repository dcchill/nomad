/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.create_nomad.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import net.create_nomad.GearboundMod;

public class GearboundModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GearboundMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATE_GEARBOUND_MENU = REGISTRY.register("create_gearbound_menu",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.gearbound.create_gearbound_menu")).icon(() -> new ItemStack(GearboundModItems.BROWN_BRASS_BACKPACK_ITEM.get())).displayItems((parameters, tabData) -> {
				tabData.accept(GearboundModItems.GRAPPLING_HOOK.get());
				tabData.accept(GearboundModItems.CHAINSAW.get());
				tabData.accept(GearboundModItems.JACKHAMMER.get());
				tabData.accept(GearboundModItems.TRACK_PACK.get());
				tabData.accept(GearboundModItems.BROWN_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.WHITE_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.LIGHT_GRAY_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.GRAY_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.BLACK_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModBlocks.BROWN_BRASS_BACKPACK.get().asItem());
				tabData.accept(GearboundModItems.RED_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.ORANGE_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.YELLOW_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.LIME_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.GREEN_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.CYAN_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.LIGHT_BLUE_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.BLUE_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.PURPLE_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.MAGENTA_BRASS_BACKPACK_ITEM.get());
				tabData.accept(GearboundModItems.PINK_BRASS_BACKPACK_ITEM.get());
			}).build());
}