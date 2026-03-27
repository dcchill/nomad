package net.create_nomad.client;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import com.simibubi.create.content.schematics.client.tools.ToolType;
import net.create_nomad.CreateNomadMod;
import net.create_nomad.item.ConstructinatorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
	private static final ResourceLocation CREATE_SCHEMATIC_ID = ResourceLocation.fromNamespaceAndPath("create", "schematic");

	// Reflected methods
	private static Method loadSettingsMethod;
	private static Method setupRendererMethod;

	// Reflected fields
	private static Field activeSchematicItemField;
	private static Field activeHotbarSlotField;
	private static Field activeField;
	private static Field deployedField;
	private static Field displayedSchematicField;
	private static Field currentToolField;

	private static boolean reflectionReady = false;
	private static boolean reflectionFailed = false;
	private static String lastOffhandSchematic = "";
	private static boolean forcedPreviewLastTick = false;

	/**
	 * HIGH priority: runs before Create's SchematicHandler.tick().
	 * Manually replicates what init() does but always calls setupRenderer(),
	 * bypassing the deployed check that normally gates it.
	 */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onClientTickEarly(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			return;
		}

		ItemStack mainHand = player.getMainHandItem();
		ItemStack offhand = player.getOffhandItem();
		if (!(mainHand.getItem() instanceof ConstructinatorItem) || !isSchematicWithFile(offhand)) {
			return;
		}

		if (!ensureReflection()) {
			return;
		}

		SchematicHandler schematicHandler = CreateClient.SCHEMATIC_HANDLER;
		String schematicFile = offhand.get(AllDataComponents.SCHEMATIC_FILE);
		if (schematicFile == null || schematicFile.isEmpty()) {
			return;
		}

		try {
			String currentDisplayed = (String) displayedSchematicField.get(schematicHandler);
			boolean needsInit = !schematicFile.equals(lastOffhandSchematic)
					|| !schematicFile.equals(currentDisplayed);

			if (needsInit) {
				// Replicate init() manually so we can force deployed=true
				// before setupRenderer() is called, without touching the item's components
				activeSchematicItemField.set(schematicHandler, offhand);
				loadSettingsMethod.invoke(schematicHandler, offhand);         // loads bounds/transform from item
				deployedField.setBoolean(schematicHandler, true);             // force deployed so setupRenderer runs
				displayedSchematicField.set(schematicHandler, schematicFile);
				setupRendererMethod.invoke(schematicHandler);                 // build the render buffers
				schematicHandler.equip(ToolType.DEPLOY);
				lastOffhandSchematic = schematicFile;
			}

			activeSchematicItemField.set(schematicHandler, offhand);
			activeHotbarSlotField.setInt(schematicHandler, player.getInventory().selected);
			activeField.setBoolean(schematicHandler, true);

		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
			reflectionFailed = true;
		}
	}

	/**
	 * LOWEST priority: runs after Create's SchematicHandler.tick().
	 * Create's tick sets active=false because the schematic isn't in main hand.
	 * We flip it back to true here so the renderer sees it as active.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onClientTickLate(ClientTickEvent.Post event) {
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

		if (!reflectionReady) {
			return;
		}

		try {
			// Create's tick() will have set active=false because the schematic
			// isn't in the main hand. Restore it so the renderer stays active.
			activeField.setBoolean(CreateClient.SCHEMATIC_HANDLER, true);
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
			deployedField.setBoolean(schematicHandler, false);
		} catch (ReflectiveOperationException ignored) {
			reflectionFailed = true;
		}

		forcedPreviewLastTick = false;
		lastOffhandSchematic = "";
	}

	private static boolean isSchematicWithFile(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(CREATE_SCHEMATIC_ID)
				&& stack.has(AllDataComponents.SCHEMATIC_FILE);
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

			loadSettingsMethod = handlerClass.getDeclaredMethod("loadSettings", ItemStack.class);
			loadSettingsMethod.setAccessible(true);

			setupRendererMethod = handlerClass.getDeclaredMethod("setupRenderer");
			setupRendererMethod.setAccessible(true);

			activeSchematicItemField = handlerClass.getDeclaredField("activeSchematicItem");
			activeSchematicItemField.setAccessible(true);

			activeHotbarSlotField = handlerClass.getDeclaredField("activeHotbarSlot");
			activeHotbarSlotField.setAccessible(true);

			activeField = handlerClass.getDeclaredField("active");
			activeField.setAccessible(true);

			deployedField = handlerClass.getDeclaredField("deployed");
			deployedField.setAccessible(true);

			displayedSchematicField = handlerClass.getDeclaredField("displayedSchematic");
			displayedSchematicField.setAccessible(true);

			currentToolField = handlerClass.getDeclaredField("currentTool");
			currentToolField.setAccessible(true);

			reflectionReady = true;
			return true;
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
			reflectionFailed = true;
			return false;
		}
	}
}