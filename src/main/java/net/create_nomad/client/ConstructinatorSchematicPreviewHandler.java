package net.create_nomad.client;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import com.simibubi.create.content.schematics.client.tools.ISchematicTool;
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
import java.lang.reflect.Proxy;
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
	private static Field outlineField;
	private static Field toolTypeToolField;

	private static Method bufferColorFloatMethod;
	private static Method bufferColorIntMethod;
	private static Method bufferSetColorIntMethod;

	private static final float PREVIEW_RED = 1f;
	private static final float PREVIEW_GREEN = 0.55f;
	private static final float PREVIEW_BLUE = 0.1f;
	private static final float PREVIEW_ALPHA = 0.5f;

	private static boolean reflectionReady = false;
	private static boolean reflectionFailed = false;
	private static String initializedOffhandSchematic = "";
	private static boolean forcedPreviewLastTick = false;

	private static ISchematicTool originalDeployTool;
	private static ISchematicTool orangeDeployToolProxy;

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
				installOrangeDeployToolProxy();
				activeSchematicItemField.set(schematicHandler, offhand);
				loadSettingsMethod.invoke(schematicHandler, offhand);
				deployedField.setBoolean(schematicHandler, true);
				displayedSchematicField.set(schematicHandler, schematicFile);
				setupRendererMethod.invoke(schematicHandler);
				schematicHandler.equip(ToolType.DEPLOY);
				applyPreviewTransparency(schematicHandler);
				initializedOffhandSchematic = schematicFile;
			}

			// Create's tick() sets active=false because it only checks main-hand schematics.
			// Restore the offhand schematic state after Create has finished ticking.
			activeSchematicItemField.set(schematicHandler, offhand);
			activeHotbarSlotField.setInt(schematicHandler, player.getInventory().selected);
			activeField.setBoolean(schematicHandler, true);
			forcedPreviewLastTick = true;
		} catch (ReflectiveOperationException ignored) {
			reflectionFailed = true;
		}
	}

	private static void clearForcedPreview() {
		if (!forcedPreviewLastTick || !reflectionReady) {
			forcedPreviewLastTick = false;
			initializedOffhandSchematic = "";
			uninstallOrangeDeployToolProxy();
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
		uninstallOrangeDeployToolProxy();
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

			outlineField = handlerClass.getDeclaredField("outline");
			outlineField.setAccessible(true);

			Class<ToolType> toolTypeClass = ToolType.class;
			toolTypeToolField = toolTypeClass.getDeclaredField("tool");
			toolTypeToolField.setAccessible(true);

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


	private static void installOrangeDeployToolProxy() {
		if (toolTypeToolField == null) {
			return;
		}

		try {
			if (originalDeployTool == null) {
				originalDeployTool = (ISchematicTool) toolTypeToolField.get(ToolType.DEPLOY);
			}
			if (originalDeployTool == null) {
				return;
			}
			if (orangeDeployToolProxy == null) {
				orangeDeployToolProxy = (ISchematicTool) Proxy.newProxyInstance(
					ISchematicTool.class.getClassLoader(),
					new Class<?>[]{ISchematicTool.class},
					(proxy, method, args) -> {
						if ("renderOnSchematic".equals(method.getName())) {
							return null;
						}
						return method.invoke(originalDeployTool, args);
					});
			}
			Object currentTool = toolTypeToolField.get(ToolType.DEPLOY);
			if (currentTool != orangeDeployToolProxy) {
				toolTypeToolField.set(ToolType.DEPLOY, orangeDeployToolProxy);
			}
		} catch (ReflectiveOperationException ignored) {
			// If ToolType internals change, keep default behavior.
		}
	}

	private static void uninstallOrangeDeployToolProxy() {
		if (toolTypeToolField == null || originalDeployTool == null) {
			return;
		}
		try {
			Object currentTool = toolTypeToolField.get(ToolType.DEPLOY);
			if (currentTool == orangeDeployToolProxy) {
				toolTypeToolField.set(ToolType.DEPLOY, originalDeployTool);
			}
		} catch (ReflectiveOperationException ignored) {
			// ignore
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
					applyBufferColor(buffer, PREVIEW_RED, PREVIEW_GREEN, PREVIEW_BLUE, PREVIEW_ALPHA);
				}
			}
		} catch (ReflectiveOperationException ignored) {
			reflectionFailed = true;
		}
	}

	private static void applyOutlineTint(SchematicHandler schematicHandler) {
		// Intentionally no-op. Kept for compatibility with stale generated call sites.
	}

	private static void applyBufferColor(Object buffer, float red, float green, float blue, float alpha) {
		if (buffer == null) {
			return;
		}

		try {
			int alphaInt = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
			int color = (alphaInt << 24) | toRgbInt(red, green, blue);

			if (bufferSetColorIntMethod == null) {
				try {
					bufferSetColorIntMethod = buffer.getClass().getMethod("setColor", int.class);
				} catch (NoSuchMethodException ignored) {
					bufferSetColorIntMethod = null;
				}
			}
			if (bufferSetColorIntMethod != null) {
				bufferSetColorIntMethod.invoke(buffer, color);
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
				bufferColorIntMethod.invoke(buffer, color);
				return;
			}

			if (bufferColorFloatMethod == null) {
				try {
					bufferColorFloatMethod = buffer.getClass().getMethod("color", float.class, float.class, float.class, float.class);
				} catch (NoSuchMethodException ignored) {
					bufferColorFloatMethod = null;
				}
			}
			if (bufferColorFloatMethod != null) {
				bufferColorFloatMethod.invoke(buffer, red, green, blue, alpha);
			}
		} catch (ReflectiveOperationException ignored) {
			// Ignore unknown buffer implementations and keep normal preview rendering.
		}
	}

	private static int toRgbInt(float red, float green, float blue) {
		int redInt = Math.max(0, Math.min(255, Math.round(red * 255f)));
		int greenInt = Math.max(0, Math.min(255, Math.round(green * 255f)));
		int blueInt = Math.max(0, Math.min(255, Math.round(blue * 255f)));
		return (redInt << 16) | (greenInt << 8) | blueInt;
	}

}
