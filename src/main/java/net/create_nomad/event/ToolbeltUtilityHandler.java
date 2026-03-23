package net.create_nomad.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.item.ToolbeltItem;
import net.create_nomad.network.ToolbeltUtilitySelectionMessage;
import net.create_nomad.util.ToolbeltDataUtils;

@EventBusSubscriber(modid = CreateNomadMod.MODID)
public final class ToolbeltUtilityHandler {
	private ToolbeltUtilityHandler() {
	}

	@SubscribeEvent
	public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
		if (!event.getEntity().getPersistentData().getBoolean(ToolbeltUtilitySelectionMessage.PLAYER_TAG)) {
			return;
		}

		ItemStack toolbelt = ToolbeltItem.findEquippedToolbelt(event.getEntity());
		if (toolbelt.isEmpty()) {
			return;
		}

		ItemStack utilityTool = ToolbeltDataUtils.getSelectedStack(toolbelt, event.getEntity().level().registryAccess());
		if (utilityTool.isEmpty()) {
			return;
		}

		float utilitySpeed = utilityTool.getDestroySpeed(event.getState());
		if (utilitySpeed > event.getNewSpeed()) {
			event.setNewSpeed(utilitySpeed);
		}
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().getPersistentData().getBoolean(ToolbeltUtilitySelectionMessage.PLAYER_TAG)) {
			return;
		}

		ItemStack toolbelt = ToolbeltItem.findEquippedToolbelt(event.getEntity());
		if (toolbelt.isEmpty()) {
			return;
		}

		ItemStack utilityTool = ToolbeltDataUtils.getSelectedStack(toolbelt, event.getEntity().level().registryAccess());
		if (utilityTool.isEmpty()) {
			return;
		}

		UseOnContext context = new UseOnContext(event.getEntity(), event.getHand(), event.getHitVec());
		InteractionResult result = utilityTool.useOn(context);
		if (result.consumesAction()) {
			ItemStackHandler handler = ToolbeltDataUtils.loadHandler(toolbelt, event.getEntity().level().registryAccess());
			handler.setStackInSlot(ToolbeltDataUtils.getSelectedSlot(toolbelt), utilityTool);
			ToolbeltDataUtils.saveHandler(toolbelt, handler, event.getEntity().level().registryAccess());
			event.setCancellationResult(result);
			event.setCanceled(true);
		}
	}
}
