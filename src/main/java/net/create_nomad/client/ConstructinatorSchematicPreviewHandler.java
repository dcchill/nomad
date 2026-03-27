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
import java.util.Map;
import java.util.Vector;

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
	private static Field renderersField;
	private static Field bufferCacheField;

	private static Method bufferColorFloatMethod;
	private static Method bufferColorIntMethod;
	private static Method bufferSetColorIntMethod;

	private static final float PREVIEW_ALPHA = 0.6f;

	private static boolean reflectionReady = false;
	private static boolean reflectionFailed = false;
	private static String initializedOffhandSchematic = "";
	private static boolean forcedPreviewLastTick = false;

	/**
	 * HIGH priority: runs before Create's SchematicHandler.tick().
	 *
	 * Create calls itemLost() when no main-hand schematic is found. itemLost() only scans
	 * the hotbar, so an offhand schematic is considered "lost" and Create clears renderers.
	 * Clearing activeSchematicItem ahead of Create's tick avoids that cleanup path.
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

		try {
			activeSchematicItemField.set(CreateClient.SCHEMATIC_HANDLER, null);
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
			SchematicHandler schematicHandler = CreateClient.SCHEMATIC_HANDLER;
			String schematicFile = offhand.get(AllDataComponents.SCHEMATIC_FILE);
			if (schematicFile == null || schematicFile.isEmpty()) {
				return;
			}

			boolean needsInit = !forcedPreviewLastTick
					|| !schematicFile.equals(initializedOffhandSchematic);

			if (needsInit) {
				activeSchematicItemField.set(schematicHandler, offhand);
				loadSettingsMethod.invoke(schematicHandler, offhand);
				deployedField.setBoolean(schematicHandler, true);
				displayedSchematicField.set(schematicHandler, schematicFile);
				setupRendererMethod.invoke(schematicHandler);
				schematicHandler.equip(ToolType.DEPLOY);
				initializedOffhandSchematic = schematicFile;
			}

			// Create's tick() sets active=false because it only checks main-hand schematics.
			// Restore the offhand schematic state after Create has finished ticking.
			activeSchematicItemField.set(schematicHandler, offhand);
			activeHotbarSlotField.setInt(schematicHandler, player.getInventory().selected);
			activeField.setBoolean(schematicHandler, true);
			applyPreviewTransparency(schematicHandler);
			forcedPreviewLastTick = true;
		} catch (ReflectiveOperationException ignored) {
			reflectionFailed = true;
		}
	}

	private static void clearForcedPreview() {
		if (!forcedPreviewLastTick || !reflectionReady) {
			forcedPreviewLastTick = false;
			initializedOffhandSchematic = "";
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
		initializedOffhandSchematic = "";
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

			renderersField = handlerClass.getDeclaredField("renderers");
			renderersField.setAccessible(true);

			Class<?> rendererClass = Class.forName("com.simibubi.create.content.schematics.client.SchematicRenderer");
			bufferCacheField = rendererClass.getDeclaredField("bufferCache");
			bufferCacheField.setAccessible(true);

			reflectionReady = true;
			return true;
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
			reflectionFailed = true;
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private static void applyPreviewTransparency(SchematicHandler schematicHandler) {
		if (renderersField == null || bufferCacheField == null) {
			return;
		}

		try {
			Object renderersObject = renderersField.get(schematicHandler);
			if (!(renderersObject instanceof Vector<?> renderers)) {
				return;
			}

			for (Object renderer : renderers) {
				Object cacheObject = bufferCacheField.get(renderer);
				if (!(cacheObject instanceof Map<?, ?> cache)) {
					continue;
				}

				for (Object buffer : cache.values()) {
					applyBufferAlpha(buffer, PREVIEW_ALPHA);
				}
			}
		} catch (ReflectiveOperationException ignored) {
			reflectionFailed = true;
		}
	}

	private static void applyBufferAlpha(Object buffer, float alpha) {
		if (buffer == null) {
			return;
		}

		try {
			if (bufferColorFloatMethod == null) {
				try {
					bufferColorFloatMethod = buffer.getClass().getMethod("color", float.class, float.class, float.class, float.class);
				} catch (NoSuchMethodException ignored) {
					bufferColorFloatMethod = null;
				}
			}
			if (bufferColorFloatMethod != null) {
				bufferColorFloatMethod.invoke(buffer, 1f, 1f, 1f, alpha);
				return;
			}

			if (bufferColorIntMethod == null) {
				try {
					bufferColorIntMethod = buffer.getClass().getMethod("color", int.class);
				} catch (NoSuchMethodException ignored) {
					bufferColorIntMethod = null;
				}
			}
			if (bufferColorIntMethod != null) {
				int alphaInt = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
				bufferColorIntMethod.invoke(buffer, (alphaInt << 24) | 0x00FFFFFF);
				return;
			}

			if (bufferSetColorIntMethod == null) {
				try {
					bufferSetColorIntMethod = buffer.getClass().getMethod("setColor", int.class);
				} catch (NoSuchMethodException ignored) {
					bufferSetColorIntMethod = null;
				}
			}
			if (bufferSetColorIntMethod != null) {
				int alphaInt = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
				bufferSetColorIntMethod.invoke(buffer, (alphaInt << 24) | 0x00FFFFFF);
			}
		} catch (ReflectiveOperationException ignored) {
			// Ignore unknown buffer implementations and keep normal preview rendering.
		}
	}
}