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

import net.create_nomad.CreateNomadMod;

public class CreateNomadModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateNomadMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATE_GEARBOUND_MENU = REGISTRY.register("create_gearbound_menu",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.create_nomad.create_gearbound_menu")).icon(() -> new ItemStack(CreateNomadModItems.BROWN_BRASS_BACKPACK_ITEM.get())).displayItems((parameters, tabData) -> {
				tabData.accept(CreateNomadModItems.GRAPPLING_HOOK.get());
				tabData.accept(CreateNomadModItems.CHAINSAW.get());
				tabData.accept(CreateNomadModItems.JACKHAMMER.get());
				tabData.accept(CreateNomadModItems.HARPOON_GUN.get());
				tabData.accept(CreateNomadModItems.HARPOON_ITEM.get());
				tabData.accept(CreateNomadModItems.TRACK_PACK.get());
				tabData.accept(CreateNomadModItems.BROWN_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.WHITE_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.LIGHT_GRAY_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.GRAY_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.BLACK_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModBlocks.BROWN_BRASS_BACKPACK.get().asItem());
				tabData.accept(CreateNomadModItems.RED_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.ORANGE_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.YELLOW_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.LIME_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.GREEN_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.CYAN_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.LIGHT_BLUE_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.BLUE_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.PURPLE_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.MAGENTA_BRASS_BACKPACK_ITEM.get());
				tabData.accept(CreateNomadModItems.PINK_BRASS_BACKPACK_ITEM.get());
			}).build());
}