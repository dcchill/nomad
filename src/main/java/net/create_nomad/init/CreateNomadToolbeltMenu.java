package net.create_nomad.init;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.world.inventory.ToolbeltMenu;

public final class CreateNomadToolbeltMenu {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, CreateNomadMod.MODID);
	public static final DeferredHolder<MenuType<?>, MenuType<ToolbeltMenu>> TOOLBELT_MENU = REGISTRY.register("toolbelt_menu",
			() -> IMenuTypeExtension.create((id, inventory, buffer) -> new ToolbeltMenu(id, inventory, inventory.player.getInventory().selected)));

	private CreateNomadToolbeltMenu() {
	}

	public static void register(IEventBus modEventBus) {
		REGISTRY.register(modEventBus);
	}
}
