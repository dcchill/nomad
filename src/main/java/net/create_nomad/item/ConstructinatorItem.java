package net.create_nomad.item;

import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.phys.Vec3;

import net.create_nomad.item.renderer.ConstructinatorItemRenderer;
import net.create_nomad.CreateNomadMod;
import net.create_nomad.util.BackpackDataUtils;
import net.create_nomad.util.BackpackInventoryRules;
import net.create_nomad.util.BackpackItemAssociations;

import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ConstructinatorItem extends Item implements GeoItem {
	private static final String PRINTER_TAG = "constructinatorPrinter";
	private static final String GECKO_ANIM_TAG = "geckoAnim";
	private static final String SCHEMATIC_FILE_TAG = "constructinatorSchematicFile";
	private static final int PLACE_INTERVAL_TICKS = 2;
	private static final int BACKTANK_AIR_COST_PER_BLOCK = 1;
	private static final int SHOT_VISUAL_LIFETIME_TICKS = 8;
	private static final double SHOT_VISUAL_SPEED = 0.85;
	private static final RawAnimation FIRE_ANIMATION = RawAnimation.begin().thenPlay("fire");
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	public String animationprocedure = "empty";

	public ConstructinatorItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
		SingletonGeoAnimatable.registerSyncedAnimatable(this);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResultHolder.pass(stack);
		}

		if (!hasSchematicFile(player.getOffhandItem(), level)) {
			return InteractionResultHolder.fail(stack);
		}

		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
		super.onUseTick(level, entity, stack, remainingUseDuration);
		if (!(entity instanceof Player player)) {
			return;
		}

		if (level.isClientSide) {
			return;
		}

		if (player.tickCount % PLACE_INTERVAL_TICKS != 0) {
			return;
		}

		if (!tryPlaceNextFromSchematic((ServerLevel) level, player, stack)) {
			player.releaseUsingItem();
		}
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	private static boolean tryPlaceNextFromSchematic(ServerLevel level, Player player, ItemStack constructinatorStack) {
		ItemStack schematicStack = player.getOffhandItem();
		String schematicFile = extractSchematicFile(schematicStack, level);
		if (schematicFile.isEmpty()) {
			return false;
		}

		SchematicPrinter printer = new SchematicPrinter();
		CompoundTag printerTag = getCustomTag(constructinatorStack).getCompound(PRINTER_TAG);
		if (!printerTag.isEmpty()) {
			printer.fromTag(printerTag, false);
		}

		String previousFile = getCustomTag(constructinatorStack).getString(SCHEMATIC_FILE_TAG);
		if (!printer.isLoaded() || !schematicFile.equals(previousFile)) {
			printer.resetSchematic();
			printer.loadSchematic(schematicStack, level, false);
		}

		if (!printer.isLoaded() || printer.isErrored()) {
			return false;
		}

		boolean placed = false;
		int safety = 256;
		while (!placed && safety-- > 0) {
			if (!printer.advanceCurrentPos()) {
				storePrinterState(constructinatorStack, printer, schematicFile);
				return false;
			}

			if (!printer.shouldPlaceCurrent(level)) {
				continue;
			}

			final BlockState[] targetState = { null };
			final BlockPos[] targetPos = { null };
			printer.handleCurrentTarget((pos, state, blockEntity) -> {
				targetPos[0] = pos.immutable();
				targetState[0] = state;
			}, (pos, entityTarget) -> {
			});
			if (targetState[0] == null || targetPos[0] == null || shouldSkipPlacementState(targetState[0])) {
				continue;
			}

			if (!targetState[0].canSurvive(level, targetPos[0])) {
				continue;
			}

			ItemRequirement requirement = printer.getCurrentRequirement();
			if (requirement.isInvalid()) {
				continue;
			}

			if (!canMeetRequirement(player, level, requirement)) {
				storePrinterState(constructinatorStack, printer, schematicFile);
				return false;
			}

			if (!tryConsumeBacktankAir(player, BACKTANK_AIR_COST_PER_BLOCK)) {
				storePrinterState(constructinatorStack, printer, schematicFile);
				return false;
			}

			if (!consumeRequirement(player, level, requirement)) {
				storePrinterState(constructinatorStack, printer, schematicFile);
				return false;
			}

			final boolean[] placementSucceeded = { false };
			printer.handleCurrentTarget((pos, state, blockEntity) -> placeBlock(level, player, pos, state, requirement, placementSucceeded), (pos, entityTarget) -> {
			});

			if (placementSucceeded[0]) {
				placed = true;
				printer.sendBlockUpdates(level);
				CustomData.update(DataComponents.CUSTOM_DATA, constructinatorStack, tag -> tag.putString(GECKO_ANIM_TAG, "fire"));
				if (constructinatorStack.getItem() instanceof ConstructinatorItem constructinatorItem) {
					constructinatorItem.triggerAnim(player, GeoItem.getOrAssignId(constructinatorStack, level), "procedureController", "fire");
				}
			}
		}

		storePrinterState(constructinatorStack, printer, schematicFile);
		return true;
	}

	private static void placeBlock(ServerLevel level, Player player, net.minecraft.core.BlockPos pos, BlockState state, ItemRequirement requirement, boolean[] placementSucceeded) {
		if (state.isAir()) {
			placementSucceeded[0] = true;
			return;
		}

		boolean placed = level.setBlock(pos, state, 3);
		if (placed) {
			spawnShotVisual(level, player, pos, state, requirement);
			placementSucceeded[0] = true;
		}
	}

	private static void spawnShotVisual(ServerLevel level, Player player, net.minecraft.core.BlockPos targetPos, BlockState placedState, ItemRequirement requirement) {
		ItemStack display = ItemStack.EMPTY;
		for (ItemRequirement.StackRequirement stackRequirement : requirement.getRequiredItems()) {
			if (stackRequirement.usage == ItemRequirement.ItemUseType.CONSUME && !stackRequirement.stack.isEmpty()) {
				display = stackRequirement.stack.copyWithCount(1);
				break;
			}
		}

		if (display.isEmpty()) {
			if (placedState.getBlock() instanceof net.minecraft.world.level.block.AirBlock) {
				return;
			}
			Item item = BlockItem.BY_BLOCK.get(placedState.getBlock());
			if (item != null) {
				display = new ItemStack(item);
			}
		}

		if (display.isEmpty()) {
			return;
		}

		Vec3 look = player.getLookAngle().normalize();
		Vec3 muzzle = player.getEyePosition().add(look.scale(0.7));
		Vec3 targetCenter = Vec3.atCenterOf(targetPos);
		Vec3 velocity = targetCenter.subtract(muzzle).normalize().scale(SHOT_VISUAL_SPEED);

		ItemEntity projectileVisual = new ItemEntity(level, muzzle.x, muzzle.y, muzzle.z, display);
		projectileVisual.setNoGravity(true);
		projectileVisual.setPickUpDelay(32767);
		projectileVisual.setDeltaMovement(velocity);
		projectileVisual.setInvulnerable(true);
		level.addFreshEntity(projectileVisual);
		CreateNomadMod.queueServerWork(SHOT_VISUAL_LIFETIME_TICKS, projectileVisual::discard);
	}

	private static boolean shouldSkipPlacementState(BlockState state) {
		if (state.isAir()) {
			return true;
		}
		return state.getFluidState().is(Fluids.WATER) || state.getFluidState().is(Fluids.LAVA);
	}

	private static boolean tryConsumeBacktankAir(Player player, int airCost) {
		if (airCost <= 0 || player.isCreative()) {
			return true;
		}

		List<ItemStack> backtanksWithAir = BacktankUtil.getAllWithAir(player);
		if (backtanksWithAir.isEmpty()) {
			return false;
		}

		ItemStack backtank = backtanksWithAir.getFirst();
		if (BacktankUtil.getAir(backtank) < airCost) {
			return false;
		}

		BacktankUtil.consumeAir(player, backtank, airCost);
		return true;
	}

	private static boolean canMeetRequirement(Player player, ServerLevel level, ItemRequirement requirement) {
		if (player.getAbilities().instabuild || requirement.isEmpty()) {
			return true;
		}

		for (ItemRequirement.StackRequirement stackRequirement : requirement.getRequiredItems()) {
			if (!hasMatchingStack(player, level, stackRequirement)) {
				return false;
			}
		}

		return true;
	}

	private static boolean consumeRequirement(Player player, ServerLevel level, ItemRequirement requirement) {
		if (player.getAbilities().instabuild || requirement.isEmpty()) {
			return true;
		}

		for (ItemRequirement.StackRequirement stackRequirement : requirement.getRequiredItems()) {
			if (!consumeMatchingStack(player, level, stackRequirement)) {
				return false;
			}
		}

		return true;
	}

	private static boolean hasMatchingStack(Player player, ServerLevel level, ItemRequirement.StackRequirement requirement) {
		for (ItemStack stack : player.getInventory().offhand) {
			if (requirement.matches(stack) && !stack.isEmpty()) {
				return true;
			}
		}
		for (ItemStack stack : player.getInventory().items) {
			if (requirement.matches(stack) && !stack.isEmpty()) {
				return true;
			}
		}

		ItemStackHandler backpackHandler = getEquippedBackpackHandler(player, level);
		if (backpackHandler != null) {
			int slots = Math.min(BackpackInventoryRules.STORAGE_SLOT_COUNT, backpackHandler.getSlots());
			for (int i = 0; i < slots; i++) {
				ItemStack stack = backpackHandler.getStackInSlot(i);
				if (requirement.matches(stack) && !stack.isEmpty()) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean consumeMatchingStack(Player player, ServerLevel level, ItemRequirement.StackRequirement requirement) {
		for (ItemStack stack : player.getInventory().offhand) {
			if (!requirement.matches(stack) || stack.isEmpty()) {
				continue;
			}

			if (requirement.usage == ItemRequirement.ItemUseType.DAMAGE) {
				if (!stack.isDamageableItem()) {
					continue;
				}
				stack.setDamageValue(stack.getDamageValue() + 1);
				if (stack.getDamageValue() > stack.getMaxDamage()) {
					stack.shrink(1);
				}
			} else {
				stack.shrink(1);
			}
			return true;
		}

		for (ItemStack stack : player.getInventory().items) {
			if (!requirement.matches(stack) || stack.isEmpty()) {
				continue;
			}

			if (requirement.usage == ItemRequirement.ItemUseType.DAMAGE) {
				if (!stack.isDamageableItem()) {
					continue;
				}
				stack.setDamageValue(stack.getDamageValue() + 1);
				if (stack.getDamageValue() > stack.getMaxDamage()) {
					stack.shrink(1);
				}
			} else {
				stack.shrink(1);
			}
			return true;
		}

		ItemStack backpackStack = findFirstCuriosBackpack(player);
		if (!backpackStack.isEmpty()) {
			ItemStackHandler backpackHandler = BackpackDataUtils.loadHandlerFromItem(backpackStack, level, BackpackInventoryRules.TOTAL_SLOT_COUNT);
			int slots = Math.min(BackpackInventoryRules.STORAGE_SLOT_COUNT, backpackHandler.getSlots());
			for (int slot = 0; slot < slots; slot++) {
				ItemStack stack = backpackHandler.getStackInSlot(slot);
				if (!requirement.matches(stack) || stack.isEmpty()) {
					continue;
				}

				if (requirement.usage == ItemRequirement.ItemUseType.DAMAGE) {
					if (!stack.isDamageableItem()) {
						continue;
					}
					stack.setDamageValue(stack.getDamageValue() + 1);
					if (stack.getDamageValue() > stack.getMaxDamage()) {
						stack.shrink(1);
					}
				} else {
					stack.shrink(1);
				}

				backpackHandler.setStackInSlot(slot, stack);
				BackpackDataUtils.saveHandlerToItem(backpackHandler, backpackStack, level);
				return true;
			}
		}

		return false;
	}

	private static ItemStackHandler getEquippedBackpackHandler(Player player, ServerLevel level) {
		ItemStack backpackStack = findFirstCuriosBackpack(player);
		if (backpackStack.isEmpty()) {
			return null;
		}
		return BackpackDataUtils.loadHandlerFromItem(backpackStack, level, BackpackInventoryRules.TOTAL_SLOT_COUNT);
	}

	private static ItemStack findFirstCuriosBackpack(Player player) {
		IItemHandler curiosInventory = CreateNomadMod.CuriosApiHelper.getCuriosInventory(player);
		if (curiosInventory == null) {
			return ItemStack.EMPTY;
		}

		for (int i = 0; i < curiosInventory.getSlots(); i++) {
			ItemStack stack = curiosInventory.getStackInSlot(i);
			if (BackpackItemAssociations.isBackpackItem(stack)) {
				return stack;
			}
		}

		return ItemStack.EMPTY;
	}

	private static boolean hasSchematicFile(ItemStack stack, Level level) {
		return !extractSchematicFile(stack, level).isEmpty();
	}

	private static String extractSchematicFile(ItemStack stack, Level level) {
		if (stack.isEmpty()) {
			return "";
		}

		CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		CompoundTag tag = customData.copyTag();
		if (tag.contains("create:schematic_file")) {
			return tag.getString("create:schematic_file");
		}

		try {
			CompoundTag full = (CompoundTag) stack.saveOptional(level.registryAccess());
			if (full.contains("components")) {
				CompoundTag components = full.getCompound("components");
				if (components.contains("create:schematic_file")) {
					return components.getString("create:schematic_file");
				}
			}
		} catch (Exception ignored) {
		}

		return "";
	}

	private static void storePrinterState(ItemStack stack, SchematicPrinter printer, String schematicFile) {
		CompoundTag printerData = new CompoundTag();
		printer.write(printerData);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.put(PRINTER_TAG, printerData);
			tag.putString(SCHEMATIC_FILE_TAG, schematicFile);
		});
	}

	private static CompoundTag getCustomTag(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	@Override
	public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
		consumer.accept(new GeoRenderProvider() {
			private ConstructinatorItemRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
				if (this.renderer == null)
					this.renderer = new ConstructinatorItemRenderer();
				return this.renderer;
			}
		});
	}

	private PlayState idlePredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
			return PlayState.CONTINUE;
		}
		return PlayState.STOP;
	}

	String prevAnim = "empty";

	private PlayState procedurePredicate(AnimationState event) {
		if (!this.animationprocedure.equals("empty") && event.getController().getAnimationState() == AnimationController.State.STOPPED || (!this.animationprocedure.equals(prevAnim) && !this.animationprocedure.equals("empty"))) {
			if (!this.animationprocedure.equals(prevAnim))
				event.getController().forceAnimationReset();
			event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationprocedure));
			if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
				this.animationprocedure = "empty";
				event.getController().forceAnimationReset();
			}
		} else if (this.animationprocedure.equals("empty")) {
			prevAnim = "empty";
			return PlayState.STOP;
		}
		prevAnim = this.animationprocedure;
		return PlayState.CONTINUE;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar data) {
		AnimationController procedureController = new AnimationController(this, "procedureController", 0, this::procedurePredicate)
			.triggerableAnim("fire", FIRE_ANIMATION);
		data.add(procedureController);
		AnimationController idleController = new AnimationController(this, "idleController", 0, this::idlePredicate);
		data.add(idleController);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
