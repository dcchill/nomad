package net.create_nomad.client;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import net.create_nomad.CreateNomadMod;
import net.create_nomad.item.ConstructinatorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@EventBusSubscriber(modid = CreateNomadMod.MODID, value = Dist.CLIENT)
public class ConstructinatorSchematicPreviewHandler {
	private static Method initMethod;
	private static Field activeSchematicItemField;
	private static Field activeHotbarSlotField;
	private static Field activeField;
	private static Field syncCooldownField;
	private static boolean reflectionReady = false;
	private static boolean reflectionFailed = false;
	private static String lastOffhandSchematic = "";
	private static boolean forcedPreviewLastTick = false;

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			clearForcedPreview();
			return;
		}

		ItemStack mainHand = player.getMainHandItem();
		ItemStack offhand = player.getOffhandItem();
		boolean shouldForcePreview = mainHand.getItem() instanceof ConstructinatorItem && isSchematicWithFile(offhand);

		if (!shouldForcePreview) {
			clearForcedPreview();
			return;
		}

		if (!ensureReflection()) {
			return;
		}

		SchematicHandler schematicHandler = CreateClient.SCHEMATIC_HANDLER;
		String schematicFile = offhand.get(AllDataComponents.SCHEMATIC_FILE);
		if (schematicFile == null || schematicFile.isEmpty()) {
			clearForcedPreview();
			return;
		}

		try {
			ItemStack activeSchematic = schematicHandler.getActiveSchematicItem();
			boolean needsInit = activeSchematic == null
					|| !ItemStack.matches(activeSchematic, offhand)
					|| !schematicFile.equals(lastOffhandSchematic);

			if (needsInit) {
				initMethod.invoke(schematicHandler, player, offhand);
				lastOffhandSchematic = schematicFile;
			}

			activeSchematicItemField.set(schematicHandler, offhand);
			activeHotbarSlotField.setInt(schematicHandler, player.getInventory().selected);
			activeField.setBoolean(schematicHandler, false);
			syncCooldownField.setInt(schematicHandler, 0);
			forcedPreviewLastTick = true;
		} catch (ReflectiveOperationException ignored) {
			reflectionFailed = true;
		}
	}

	private static void clearForcedPreview() {
		if (!forcedPreviewLastTick || !reflectionReady) {
			forcedPreviewLastTick = false;
			lastOffhandSchematic = "";
			return;
		}

		try {
			SchematicHandler schematicHandler = CreateClient.SCHEMATIC_HANDLER;
			activeSchematicItemField.set(schematicHandler, null);
			activeField.setBoolean(schematicHandler, false);
			syncCooldownField.setInt(schematicHandler, 0);
		} catch (ReflectiveOperationException ignored) {
			reflectionFailed = true;
		}

		forcedPreviewLastTick = false;
		lastOffhandSchematic = "";
	}

	private static boolean isSchematicWithFile(ItemStack stack) {
		return AllItems.SCHEMATIC.isIn(stack) && stack.has(AllDataComponents.SCHEMATIC_FILE);
	}

	private static boolean ensureReflection() {
		if (reflectionReady) {
			return true;
		}
		if (reflectionFailed) {
			return false;
		}

		try {
			Class<SchematicHandler> handlerClass = SchematicHandler.class;
			initMethod = handlerClass.getDeclaredMethod("init", LocalPlayer.class, ItemStack.class);
			initMethod.setAccessible(true);

			activeSchematicItemField = handlerClass.getDeclaredField("activeSchematicItem");
			activeSchematicItemField.setAccessible(true);

			activeHotbarSlotField = handlerClass.getDeclaredField("activeHotbarSlot");
			activeHotbarSlotField.setAccessible(true);

			activeField = handlerClass.getDeclaredField("active");
			activeField.setAccessible(true);

			syncCooldownField = handlerClass.getDeclaredField("syncCooldown");
			syncCooldownField.setAccessible(true);

			reflectionReady = true;
			return true;
		} catch (ReflectiveOperationException ignored) {
			reflectionFailed = true;
			return false;
		}
	}
}
