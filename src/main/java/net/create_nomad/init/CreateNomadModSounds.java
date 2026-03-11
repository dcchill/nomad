/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.create_nomad.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import net.create_nomad.CreateNomadMod;

public class CreateNomadModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, CreateNomadMod.MODID);
	public static final DeferredHolder<SoundEvent, SoundEvent> BACKPACK_EQUIP = REGISTRY.register("backpack_equip", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("create_nomad", "backpack_equip")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BACKPACK_CLOSE = REGISTRY.register("backpack_close", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("create_nomad", "backpack_close")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BACKPACK_OPEN = REGISTRY.register("backpack_open", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("create_nomad", "backpack_open")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BACKPACK_OFF = REGISTRY.register("backpack_off", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("create_nomad", "backpack_off")));
}