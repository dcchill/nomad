package net.create_nomad.client;

import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Vector;

@EventBusSubscriber(modid = CreateNomadMod.MODID, value = Dist.CLIENT)
public class ConstructinatorSchematicPreviewHandler {
	private static final ResourceLocation CREATE_SCHEMATIC_ID = ResourceLocation.fromNamespaceAndPath("create", "schematic");

	private static Method loadSettingsMethod;
	private static Method setupRendererMethod;
	private static Method schematicRenderMethod;
	private static Method superBufferGetInstanceMethod;
	private static Method superBufferOfMethod;
	private static Method rendererUpdateMethod;
	private static Method schematicGetBoundsMethod;
	private static Method schematicGetBlockStateMethod;
	private static Method schematicSetBlockMethod;

	private static Field activeSchematicItemField;
	private static Field activeHotbarSlotField;
	private static Field activeField;
	private static Field deployedField;
	private static Field displayedSchematicField;
	private static Field renderersField;
	private static Field rendererSchematicField;
	private static Field rendererAnchorField;
	// compatibility fields for stale generated code
	private static Field outlineField;
	private static Field toolTypeToolField;

	private static final float PREVIEW_RED = 1f;
	private static final float PREVIEW_GREEN = 0.55f;
	private static final float PREVIEW_BLUE = 0.1f;
	private static final float PREVIEW_ALPHA = 0.5f;

	private static boolean reflectionReady = false;
	private static boolean reflectionFailed = false;
	private static String initializedOffhandSchematic = "";
	private static boolean forcedPreviewLastTick = false;
	private static boolean customRenderActive = false;
	private static final Map<Object, Map<BlockPos, Object>> originalStatesByRenderer = new IdentityHashMap<>();
	// compatibility fields for stale generated code
	private static ISchematicTool originalDeployTool;
	private static ISchematicTool orangeDeployToolProxy;

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onClientTickEarly(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) return;

		ItemStack mainHand = player.getMainHandItem();
		ItemStack offhand = player.getOffhandItem();
		if (!(mainHand.getItem() instanceof ConstructinatorItem) || !isSchematicWithFile(offhand)) return;
		if (!ensureReflection()) return;

		try {
			activeSchematicItemField.set(CreateClient.SCHEMATIC_HANDLER, null);
		} catch (ReflectiveOperationException e) {
			reflectionFailed = true;
		}
	}

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
		if (!reflectionReady) return;

		try {
			SchematicHandler schematicHandler = CreateClient.SCHEMATIC_HANDLER;
			String schematicFile = offhand.get(AllDataComponents.SCHEMATIC_FILE);
			if (schematicFile == null || schematicFile.isEmpty()) return;

			boolean needsInit = !forcedPreviewLastTick || !schematicFile.equals(initializedOffhandSchematic);
			if (needsInit) {
				installOrangeDeployToolProxy();
				activeSchematicItemField.set(schematicHandler, offhand);
				loadSettingsMethod.invoke(schematicHandler, offhand);
				deployedField.setBoolean(schematicHandler, true);
				displayedSchematicField.set(schematicHandler, schematicFile);
				setupRendererMethod.invoke(schematicHandler);
				schematicHandler.equip(ToolType.DEPLOY);
				cacheRendererOriginalStates(schematicHandler);
				initializedOffhandSchematic = schematicFile;
			}

			activeSchematicItemField.set(schematicHandler, offhand);
			activeHotbarSlotField.setInt(schematicHandler, player.getInventory().selected);
			activeField.setBoolean(schematicHandler, false);
			customRenderActive = true;
			forcedPreviewLastTick = true;
		} catch (ReflectiveOperationException ignored) {
			reflectionFailed = true;
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (!customRenderActive || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !reflectionReady) return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) return;

		try {
			Object superBuffer = getSuperRenderTypeBuffer(minecraft.renderBuffers().bufferSource());
			if (superBuffer == null || schematicRenderMethod == null) return;

			hideAlreadyPlacedBlocks(CreateClient.SCHEMATIC_HANDLER, minecraft);
			Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShaderColor(PREVIEW_RED, PREVIEW_GREEN, PREVIEW_BLUE, PREVIEW_ALPHA);
			schematicRenderMethod.invoke(CreateClient.SCHEMATIC_HANDLER, event.getPoseStack(), superBuffer, cameraPos);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			RenderSystem.disableBlend();
		} catch (ReflectiveOperationException ignored) {
			reflectionFailed = true;
		}
	}

	private static Object getSuperRenderTypeBuffer(MultiBufferSource bufferSource) throws ReflectiveOperationException {
		if (superBufferGetInstanceMethod != null) return superBufferGetInstanceMethod.invoke(null);
		if (superBufferOfMethod != null) return superBufferOfMethod.invoke(null, bufferSource);
		return null;
	}

	@SuppressWarnings("unchecked")
	private static void cacheRendererOriginalStates(SchematicHandler schematicHandler) throws ReflectiveOperationException {
		originalStatesByRenderer.clear();
		Object renderersObject = renderersField.get(schematicHandler);
		if (!(renderersObject instanceof Vector<?> renderers)) return;

		for (Object renderer : renderers) {
			Object schematic = rendererSchematicField.get(renderer);
			Object anchorObj = rendererAnchorField.get(renderer);
			if (!(anchorObj instanceof BlockPos anchor) || schematic == null) continue;

			Object bounds = schematicGetBoundsMethod.invoke(schematic);
			int minX = (int) bounds.getClass().getMethod("minX").invoke(bounds);
			int minY = (int) bounds.getClass().getMethod("minY").invoke(bounds);
			int minZ = (int) bounds.getClass().getMethod("minZ").invoke(bounds);
			int maxX = (int) bounds.getClass().getMethod("maxX").invoke(bounds);
			int maxY = (int) bounds.getClass().getMethod("maxY").invoke(bounds);
			int maxZ = (int) bounds.getClass().getMethod("maxZ").invoke(bounds);

			Map<BlockPos, Object> originalStates = new HashMap<>();
			for (BlockPos localPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
				BlockPos worldPos = localPos.offset(anchor);
				Object originalState = schematicGetBlockStateMethod.invoke(schematic, worldPos);
				if (originalState != null && !originalState.equals(Blocks.AIR.defaultBlockState())) {
					originalStates.put(worldPos.immutable(), originalState);
				}
			}
			originalStatesByRenderer.put(renderer, originalStates);
		}
	}

	@SuppressWarnings("unchecked")
	private static void hideAlreadyPlacedBlocks(SchematicHandler schematicHandler, Minecraft minecraft) throws ReflectiveOperationException {
		Object renderersObject = renderersField.get(schematicHandler);
		if (!(renderersObject instanceof Vector<?> renderers)) return;

		Object airState = Blocks.AIR.defaultBlockState();
		for (Object renderer : renderers) {
			Map<BlockPos, Object> originals = originalStatesByRenderer.get(renderer);
			if (originals == null || originals.isEmpty()) continue;

			Object schematic = rendererSchematicField.get(renderer);
			if (schematic == null) continue;

			boolean changed = false;
			for (Map.Entry<BlockPos, Object> entry : originals.entrySet()) {
				BlockPos worldPos = entry.getKey();
				Object originalState = entry.getValue();
				Object worldState = minecraft.level.getBlockState(worldPos);
				Object desired = worldState.equals(originalState) ? airState : originalState;
				Object current = schematicGetBlockStateMethod.invoke(schematic, worldPos);
				if (!desired.equals(current)) {
					schematicSetBlockMethod.invoke(schematic, worldPos, desired, 2);
					changed = true;
				}
			}
			if (changed) rendererUpdateMethod.invoke(renderer);
		}
	}

	private static void clearForcedPreview() {
		if (!forcedPreviewLastTick || !reflectionReady) {
			forcedPreviewLastTick = false;
			initializedOffhandSchematic = "";
			customRenderActive = false;
			originalStatesByRenderer.clear();
			return;
		}

		Object renderersObject = renderersField.get(schematicHandler);
		if (!(renderersObject instanceof Vector<?> renderers)) {
			return;
		}

		for (Object renderer : renderers) {
			Object schematic = rendererSchematicField.get(renderer);
			Object anchorObj = rendererAnchorField.get(renderer);
			if (!(anchorObj instanceof BlockPos anchor) || schematic == null) {
				continue;
			}

			Object bounds = schematicGetBoundsMethod.invoke(schematic);
			Map<BlockPos, Object> originalStates = new HashMap<>();
			int minX = (int) bounds.getClass().getMethod("minX").invoke(bounds);
			int minY = (int) bounds.getClass().getMethod("minY").invoke(bounds);
			int minZ = (int) bounds.getClass().getMethod("minZ").invoke(bounds);
			int maxX = (int) bounds.getClass().getMethod("maxX").invoke(bounds);
			int maxY = (int) bounds.getClass().getMethod("maxY").invoke(bounds);
			int maxZ = (int) bounds.getClass().getMethod("maxZ").invoke(bounds);

			for (BlockPos localPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
				BlockPos worldPos = localPos.offset(anchor);
				Object originalState = schematicGetBlockStateMethod.invoke(schematic, worldPos);
				if (originalState != null && !originalState.equals(Blocks.AIR.defaultBlockState())) {
					originalStates.put(worldPos.immutable(), originalState);
				}
			}
			originalStatesByRenderer.put(renderer, originalStates);
		}
	}

	@SuppressWarnings("unchecked")
	private static void hideAlreadyPlacedBlocks(SchematicHandler schematicHandler, Minecraft minecraft) throws ReflectiveOperationException {
		if (renderersField == null || rendererSchematicField == null || schematicSetBlockMethod == null) {
			return;
		}

		Object renderersObject = renderersField.get(schematicHandler);
		if (!(renderersObject instanceof Vector<?> renderers)) {
			return;
		}

		Object airState = Blocks.AIR.defaultBlockState();
		for (Object renderer : renderers) {
			Map<BlockPos, Object> originals = originalStatesByRenderer.get(renderer);
			if (originals == null || originals.isEmpty()) {
				continue;
			}

			Object schematic = rendererSchematicField.get(renderer);
			if (schematic == null) {
				continue;
			}

			boolean changed = false;
			for (Map.Entry<BlockPos, Object> entry : originals.entrySet()) {
				BlockPos worldPos = entry.getKey();
				Object originalState = entry.getValue();
				Object worldState = minecraft.level.getBlockState(worldPos);
				Object desired = worldState.equals(originalState) ? airState : originalState;
				Object current = schematicGetBlockStateMethod.invoke(schematic, worldPos);
				if (!desired.equals(current)) {
					schematicSetBlockMethod.invoke(schematic, worldPos, desired, 2);
					changed = true;
				}
			}

			if (changed && rendererUpdateMethod != null) {
				rendererUpdateMethod.invoke(renderer);
			}
		}
	}

	// Compatibility shims for stale generated code paths.
	private static void installOrangeDeployToolProxy() {
		// Intentionally no-op.
	}

	private static void uninstallOrangeDeployToolProxy() {
		// Intentionally no-op.
	}

	private static void clearForcedPreview() {
			if (!forcedPreviewLastTick || !reflectionReady) {
				forcedPreviewLastTick = false;
				initializedOffhandSchematic = "";
				customRenderActive = false;
				originalStatesByRenderer.clear();
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
		customRenderActive = false;
		originalStatesByRenderer.clear();
	}

	private static boolean isSchematicWithFile(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(CREATE_SCHEMATIC_ID)
				&& stack.has(AllDataComponents.SCHEMATIC_FILE);
	}

	private static boolean ensureReflection() {
		if (reflectionReady) return true;
		if (reflectionFailed) return false;

		try {
			Class<SchematicHandler> handlerClass = SchematicHandler.class;
			loadSettingsMethod = handlerClass.getDeclaredMethod("loadSettings", ItemStack.class);
			loadSettingsMethod.setAccessible(true);
			setupRendererMethod = handlerClass.getDeclaredMethod("setupRenderer");
			setupRendererMethod.setAccessible(true);

			Class<?> superRenderTypeBufferClass = Class.forName("net.createmod.catnip.render.SuperRenderTypeBuffer");
			schematicRenderMethod = handlerClass.getDeclaredMethod("render", com.mojang.blaze3d.vertex.PoseStack.class,
					superRenderTypeBufferClass, net.minecraft.world.phys.Vec3.class);
			schematicRenderMethod.setAccessible(true);
			try {
				superBufferGetInstanceMethod = superRenderTypeBufferClass.getDeclaredMethod("getInstance");
				superBufferGetInstanceMethod.setAccessible(true);
			} catch (NoSuchMethodException ignored) {
				superBufferGetInstanceMethod = null;
			}
			if (superBufferGetInstanceMethod == null) {
				try {
					superBufferOfMethod = superRenderTypeBufferClass.getDeclaredMethod("of", MultiBufferSource.class);
					superBufferOfMethod.setAccessible(true);
				} catch (NoSuchMethodException ignored) {
					superBufferOfMethod = null;
				}
			}

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
			renderersField = handlerClass.getDeclaredField("renderers");
			renderersField.setAccessible(true);

			outlineField = handlerClass.getDeclaredField("outline");
			outlineField.setAccessible(true);

			Class<ToolType> toolTypeClass = ToolType.class;
			toolTypeToolField = toolTypeClass.getDeclaredField("tool");
			toolTypeToolField.setAccessible(true);

			Class<?> rendererClass = Class.forName("com.simibubi.create.content.schematics.client.SchematicRenderer");
			rendererSchematicField = rendererClass.getDeclaredField("schematic");
			rendererSchematicField.setAccessible(true);
			rendererAnchorField = rendererClass.getDeclaredField("anchor");
			rendererAnchorField.setAccessible(true);
			rendererUpdateMethod = rendererClass.getDeclaredMethod("update");
			rendererUpdateMethod.setAccessible(true);

			Class<?> schematicLevelClass = Class.forName("net.createmod.catnip.levelWrappers.SchematicLevel");
			schematicGetBoundsMethod = schematicLevelClass.getDeclaredMethod("getBounds");
			schematicGetBoundsMethod.setAccessible(true);
			schematicGetBlockStateMethod = schematicLevelClass.getDeclaredMethod("getBlockState", BlockPos.class);
			schematicGetBlockStateMethod.setAccessible(true);
			schematicSetBlockMethod = schematicLevelClass.getMethod("setBlock", BlockPos.class,
					Class.forName("net.minecraft.world.level.block.state.BlockState"), int.class);
			schematicSetBlockMethod.setAccessible(true);

			reflectionReady = true;
			return true;
		} catch (ReflectiveOperationException e) {
			reflectionFailed = true;
			return false;
		}
	}

	// compatibility shims for stale generated code paths
	private static void installOrangeDeployToolProxy() { }
	private static void uninstallOrangeDeployToolProxy() { }
	private static void applyOutlineTint(SchematicHandler schematicHandler) { }
}
