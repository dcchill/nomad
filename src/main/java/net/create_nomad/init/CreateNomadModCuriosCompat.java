package net.create_nomad.init;

import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.CuriosCapability;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

public class CreateNomadModCuriosCompat {
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.BROWN_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.BLACK_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.GRAY_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.LIGHT_GRAY_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.LIGHT_BLUE_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.BLUE_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.YELLOW_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.GREEN_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.ORANGE_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.RED_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.PINK_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.PURPLE_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.MAGENTA_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.WHITE_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.LIME_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("create_nomad:backpack_equip")).value(), 1, 1);
			}
		}, CreateNomadModItems.CYAN_BRASS_BACKPACK_ITEM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}
		}, CreateNomadModItems.TOOLBELT.get());
	}
}