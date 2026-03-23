package net.create_nomad.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.init.CreateNomadModKeyMappings;
import net.create_nomad.item.ToolbeltItem;
import net.create_nomad.network.ToolbeltSelectionMessage;
import net.create_nomad.network.ToolbeltUtilitySelectionMessage;
import net.create_nomad.util.ToolbeltDataUtils;

@EventBusSubscriber(modid = CreateNomadMod.MODID, value = Dist.CLIENT)
public final class ToolbeltHotbarOverlay {
	private static final int SLOT_SIZE = 20;
	private static boolean utilitySelected;
	private static int lastHotbarSlot = -1;

	private ToolbeltHotbarOverlay() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			setUtilitySelected(false);
			lastHotbarSlot = -1;
			return;
		}

		ItemStack toolbelt = ToolbeltItem.findEquippedToolbelt(minecraft.player);
		if (toolbelt.isEmpty()) {
			setUtilitySelected(false);
			lastHotbarSlot = minecraft.player.getInventory().selected;
			return;
		}

		if (minecraft.screen == null) {
			while (CreateNomadModKeyMappings.TOOLBELT_FOCUS.consumeClick()) {
				lastHotbarSlot = minecraft.player.getInventory().selected;
				setUtilitySelected(true);
			}
		}

		int selected = minecraft.player.getInventory().selected;
		if (utilitySelected && selected != lastHotbarSlot) {
			setUtilitySelected(false);
		}

		if (!utilitySelected) {
			lastHotbarSlot = selected;
		}
	}

	@SubscribeEvent
	public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.screen != null) {
			return;
		}

		ItemStack toolbelt = ToolbeltItem.findEquippedToolbelt(player);
		if (toolbelt.isEmpty()) {
			return;
		}

		double delta = event.getScrollDeltaY();
		if (delta == 0) {
			return;
		}

		if (!utilitySelected) {
			return;
		}

		if (CreateNomadModKeyMappings.TOOLBELT_FOCUS.isDown()) {
			cycleToolbeltSelection(toolbelt, delta > 0 ? -1 : 1);
			event.setCanceled(true);
			return;
		}

		setUtilitySelected(false);
		lastHotbarSlot = player.getInventory().selected;
	}

	private static void cycleToolbeltSelection(ItemStack toolbelt, int direction) {
		int nextSlot = Math.floorMod(ToolbeltDataUtils.getSelectedSlot(toolbelt) + direction, ToolbeltDataUtils.SLOT_COUNT);
		PacketDistributor.sendToServer(new ToolbeltSelectionMessage(nextSlot));
		ToolbeltDataUtils.setSelectedSlot(toolbelt, nextSlot);
	}

	@SubscribeEvent
	public static void renderUtilitySlot(RenderGuiLayerEvent.Post event) {
		if (!event.getName().equals(ResourceLocation.withDefaultNamespace("hotbar"))) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}

		ItemStack toolbelt = ToolbeltItem.findEquippedToolbelt(player);
		if (toolbelt.isEmpty()) {
			return;
		}

		int screenWidth = minecraft.getWindow().getGuiScaledWidth();
		int screenHeight = minecraft.getWindow().getGuiScaledHeight();
		int hotbarLeft = screenWidth / 2 - 91;
		int hotbarTop = screenHeight - 22;
		int x = hotbarLeft + 9 * SLOT_SIZE + 8;
		int y = hotbarTop;
		GuiGraphics guiGraphics = event.getGuiGraphics();

		guiGraphics.fill(RenderType.guiOverlay(), x, y, x + 24, y + 24, 0xD0101010);
		guiGraphics.fill(RenderType.guiOverlay(), x + 1, y + 1, x + 23, y + 23, utilitySelected ? 0xFF111111 : 0xC0404040);
		guiGraphics.fill(RenderType.guiOverlay(), x + 3, y + 3, x + 21, y + 21, 0xCC4C6E97);

		if (utilitySelected) {
			guiGraphics.renderOutline(x - 1, y - 1, 26, 26, 0xFFF4F6DA);
		}

		ItemStack selectedTool = ToolbeltDataUtils.getSelectedStack(toolbelt, player.level().registryAccess());
		if (!selectedTool.isEmpty()) {
			guiGraphics.renderItem(selectedTool, x + 4, y + 4);
			guiGraphics.renderItemDecorations(minecraft.font, selectedTool, x + 4, y + 4);
		}
	}

	public static boolean isUtilitySelected() {
		return utilitySelected;
	}

	private static void setUtilitySelected(boolean selected) {
		if (utilitySelected == selected) {
			return;
		}
		utilitySelected = selected;
		PacketDistributor.sendToServer(new ToolbeltUtilitySelectionMessage(selected));
	}
}
