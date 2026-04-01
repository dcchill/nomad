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
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.function.Consumer;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ConstructinatorItem extends Item implements GeoItem {
	private static final String PRINTER_TAG = "constructinatorPrinter";
	private static final String GECKO_ANIM_TAG = "geckoAnim";
	private static final String SCHEMATIC_FILE_TAG = "constructinatorSchematicFile";
	private static final String FAILED_TARGET_POS_TAG = "constructinatorFailedTargetPos";
	private static final String FAILED_TARGET_COUNT_TAG = "constructinatorFailedTargetCount";
	private static final String SKIPPED_TARGETS_TAG = "constructinatorSkippedTargets";
	private static final int PLACE_INTERVAL_TICKS = 2;
	private static final int BACKTANK_AIR_COST_PER_BLOCK = 1;
	private static final int SHOT_VISUAL_LIFETIME_TICKS = 8;
	private static final double SHOT_VISUAL_SPEED = 0.85;
	private static final int FAILED_TARGET_SKIP_THRESHOLD = 3;
	private static final String PROGRESS_TOTAL_TAG = "constructinatorPlaceTotal";
	private static final String PROGRESS_DONE_TAG = "constructinatorPlaceDone";
	private static final String SCHEMATIC_HASH_TAG = "constructinatorSchematicHash";
	// Persistent storage for schematic totals (file -> total)
	private static final java.util.Map<String, Integer> SCHEMATIC_TOTALS = new java.util.HashMap<>();
	private static final RawAnimation FIRE_ANIMATION = RawAnimation.begin().thenPlay("fire");
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	public String animationprocedure = "empty";

	public ConstructinatorItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
		SingletonGeoAnimatable.registerSyncedAnimatable(this);
	}

	@Override
	public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
		return net.minecraft.world.item.UseAnim.NONE;
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
		boolean schematicChanged = !schematicFile.equals(previousFile);
		if (schematicChanged) {
			printer.resetSchematic();
			clearFailedTargetTracking(constructinatorStack);
		}
		if (!printer.isLoaded() || schematicChanged) {
			printer.loadSchematic(schematicStack, level, false);

			// Compute total placements for progress tracking when schematic changes or total is not set
			if (printer.isLoaded()) {
				CompoundTag tag = getCustomTag(constructinatorStack);
				int existingTotal = tag.getInt(PROGRESS_TOTAL_TAG);
				// Check if we have a cached total for this schematic
				Integer cachedTotal = SCHEMATIC_TOTALS.get(schematicFile);
				// Only recalculate total if schematic changed or total was never set
				if (schematicChanged || existingTotal <= 0) {
					int total;
					if (cachedTotal != null) {
						total = cachedTotal;
					} else {
						total = countValidPlacements(schematicStack, level);
						SCHEMATIC_TOTALS.put(schematicFile, total);
					}
					final int finalTotal = total;
					CustomData.update(DataComponents.CUSTOM_DATA, constructinatorStack, tag2 -> {
						tag2.putInt(PROGRESS_TOTAL_TAG, finalTotal);
						if (schematicChanged) {
							tag2.putInt(PROGRESS_DONE_TAG, 0);
						}
						tag2.putString(SCHEMATIC_FILE_TAG, schematicFile);
					});
				}
			}
		}

		if (!printer.isLoaded() || printer.isErrored()) {
			return false;
		}

		boolean placed = false;
		while (!placed) {
			if (!printer.advanceCurrentPos()) {
				storePrinterState(constructinatorStack, printer, schematicFile);
				CustomData.update(DataComponents.CUSTOM_DATA, constructinatorStack, tag -> {
					tag.putInt(PROGRESS_DONE_TAG, 0);
					tag.putInt(PROGRESS_TOTAL_TAG, 0);
				});
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

			if (isAlreadySatisfiedIgnoringVolatileState(level, targetPos[0], targetState[0])) {
				continue;
			}

			ItemRequirement requirement = printer.getCurrentRequirement();
			if (requirement.isInvalid()) {
				continue;
			}

			if (!canMeetRequirement(player, level, requirement)) {
				if (isOnlyDamageRequirement(requirement)) {
					continue;
				}
				storePrinterState(constructinatorStack, printer, schematicFile);
				return false;
			}

			if (!tryConsumeBacktankAir(player, BACKTANK_AIR_COST_PER_BLOCK)) {
				storePrinterState(constructinatorStack, printer, schematicFile);
				return false;
			}

			if (!consumeRequirement(player, level, requirement)) {
				if (isOnlyDamageRequirement(requirement)) {
					continue;
				}
				storePrinterState(constructinatorStack, printer, schematicFile);
				return false;
			}

			final boolean[] placementSucceeded = { false };
			printer.handleCurrentTarget((pos, state, blockEntity) -> placeBlock(level, player, pos, state, requirement, placementSucceeded), (pos, entityTarget) -> {
			});

			if (placementSucceeded[0]) {
				clearRecentFailedTarget(constructinatorStack);
				placed = true;
				printer.sendBlockUpdates(level);
				CustomData.update(DataComponents.CUSTOM_DATA, constructinatorStack, tag -> tag.putString(GECKO_ANIM_TAG, "fire"));
				if (constructinatorStack.getItem() instanceof ConstructinatorItem constructinatorItem) {
					constructinatorItem.triggerAnim(player, GeoItem.getOrAssignId(constructinatorStack, level), "procedureController", "fire");
				}

				incrementProgress(constructinatorStack);
				// Progress bar removed - schematic preview is semi-transparent so you can see placed blocks through it
			} else {
				recordFailedTarget(constructinatorStack, targetPos[0]);
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

		BlockState existingState = level.getBlockState(pos);
		
		// Check if existing block is mineable (not bedrock, barriers, etc.)
		if (!existingState.isAir() && !isBlockMineable(existingState, level, pos, player)) {
			placementSucceeded[0] = false;
			return;
		}

		// Drop existing block's drops if there's a block to replace
		if (!existingState.isAir() && existingState.getBlock() != state.getBlock()) {
			dropBlock(level, pos, existingState, player);
		}

		BlockState stateToPlace = normalizePlacementState(state);
		boolean placed = level.setBlock(pos, stateToPlace, 18);
		if (placed) {
			spawnShotVisual(level, player, pos, stateToPlace, requirement);
			placementSucceeded[0] = true;
		}
	}

	private static boolean isBlockMineable(BlockState state, ServerLevel level, BlockPos pos, Player player) {
		// Check for unbreakable blocks
		if (state.getBlock().defaultDestroyTime() < 0) {
			return false;
		}
		
		// Check for specific unbreakable blocks
		if (state.getBlock() == net.minecraft.world.level.block.Blocks.BEDROCK ||
			state.getBlock() instanceof net.minecraft.world.level.block.BarrierBlock ||
			state.getBlock() instanceof net.minecraft.world.level.block.CommandBlock ||
			state.getBlock() instanceof net.minecraft.world.level.block.StructureBlock ||
			state.getBlock() instanceof net.minecraft.world.level.block.StructureVoidBlock) {
			return false;
		}
		
		// Check if the block can be destroyed by the player
		return state.canHarvestBlock(level, pos, player);
	}

	private static void dropBlock(ServerLevel level, BlockPos pos, BlockState state, Player player) {
		// Get the block's drops using LootParams
		net.minecraft.world.level.storage.loot.LootParams.Builder builder = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
			.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, state)
			.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(pos))
			.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, player.getMainHandItem())
			.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, player);
		
		List<net.minecraft.world.item.ItemStack> drops = state.getDrops(builder);
		
		// Spawn the drops as item entities
		for (net.minecraft.world.item.ItemStack drop : drops) {
			if (!drop.isEmpty()) {
				net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
					level,
					pos.getX() + 0.5,
					pos.getY() + 0.5,
					pos.getZ() + 0.5,
					drop
				);
				itemEntity.setDeltaMovement(
					level.random.nextGaussian() * 0.1,
					0.2 + level.random.nextGaussian() * 0.05,
					level.random.nextGaussian() * 0.1
				);
				level.addFreshEntity(itemEntity);
			}
		}
		
		// Break the block (this also triggers block events)
		level.destroyBlock(pos, false);
	}

	private static BlockState normalizePlacementState(BlockState state) {
		if (state.getBlock() instanceof LeavesBlock && state.hasProperty(LeavesBlock.PERSISTENT)) {
			return state.setValue(LeavesBlock.PERSISTENT, true);
		}
		return state;
	}

	private static boolean isAlreadySatisfiedIgnoringVolatileState(ServerLevel level, BlockPos targetPos, BlockState targetState) {
		BlockState existingState = level.getBlockState(targetPos);
		if (existingState.getBlock() != targetState.getBlock()) {
			return false;
		}

		if (!(targetState.getBlock() instanceof LeavesBlock)) {
			return false;
		}

		for (Property<?> property : targetState.getProperties()) {
			if (property == LeavesBlock.DISTANCE || property == LeavesBlock.PERSISTENT) {
				continue;
			}

			if (!existingState.hasProperty(property)) {
				return false;
			}

			Comparable<?> targetValue = targetState.getValue(property);
			Comparable<?> existingValue = existingState.getValue(property);
			if (!targetValue.equals(existingValue)) {
				return false;
			}
		}

		return true;
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

	// Progress bar removed - schematic preview is now semi-transparent
	/*
	private static String makeProgressBar(int done, int total, int length) {
		if (total <= 0) {
			StringBuilder full = new StringBuilder("[");
			for (int i = 0; i < length; i++)
				full.append('█');
			full.append(']');
			return full.toString();
		}

		done = Math.max(0, Math.min(done, total));
		int filled = (int) Math.round((done / (double) total) * length);
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < filled; i++)
			sb.append('█');
		for (int i = filled; i < length; i++)
			sb.append('░');
		sb.append(']');
		return sb.toString();
	}
	*/

	private static void incrementProgress(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			int done = tag.getInt(PROGRESS_DONE_TAG);
			int total = tag.getInt(PROGRESS_TOTAL_TAG);

			if (total <= 0) {
				tag.putInt(PROGRESS_DONE_TAG, done + 1);
				return;
			}

			if (done < total) {
				tag.putInt(PROGRESS_DONE_TAG, done + 1);
			} else {
				tag.putInt(PROGRESS_DONE_TAG, total);
			}
		});
	}

	// Progress bar display removed
	/*
	private static void showProgress(Player player, ItemStack stack) {
		if (player == null)
			return;

		CompoundTag tag = getCustomTag(stack);
		int total = tag.getInt(PROGRESS_TOTAL_TAG);
		int done = tag.getInt(PROGRESS_DONE_TAG);

		if (total > 0) {
			done = Math.min(done, total);
			String bar = makeProgressBar(done, total, 20);
			int pct = (int) Math.round((done / (double) total) * 100);
			player.displayClientMessage(Component.literal("Constructing: " + bar + " " + pct + "%"), true);
		} else {
			player.displayClientMessage(Component.literal("Constructing: " + done + " blocks"), true);
		}
	}
	*/

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

	private static boolean isSkippedTarget(ItemStack stack, BlockPos targetPos) {
		if (targetPos == null) {
			return false;
		}

		long target = targetPos.asLong();
		long[] skipped = getCustomTag(stack).getLongArray(SKIPPED_TARGETS_TAG);
		for (long skippedTarget : skipped) {
			if (skippedTarget == target) {
				return true;
			}
		}

		return false;
	}

	private static void recordFailedTarget(ItemStack stack, BlockPos targetPos) {
		if (targetPos == null) {
			return;
		}

		long target = targetPos.asLong();
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			long lastFailed = tag.getLong(FAILED_TARGET_POS_TAG);
			int failedCount = tag.getInt(FAILED_TARGET_COUNT_TAG);
			if (lastFailed == target) {
				failedCount++;
			} else {
				lastFailed = target;
				failedCount = 1;
			}

			if (failedCount >= FAILED_TARGET_SKIP_THRESHOLD) {
				long[] skipped = tag.getLongArray(SKIPPED_TARGETS_TAG);
				boolean known = false;
				for (long skippedTarget : skipped) {
					if (skippedTarget == target) {
						known = true;
						break;
					}
				}
				if (!known) {
					long[] updated = java.util.Arrays.copyOf(skipped, skipped.length + 1);
					updated[skipped.length] = target;
					tag.putLongArray(SKIPPED_TARGETS_TAG, updated);
				}
				tag.putLong(FAILED_TARGET_POS_TAG, 0L);
				tag.putInt(FAILED_TARGET_COUNT_TAG, 0);
				return;
			}

			tag.putLong(FAILED_TARGET_POS_TAG, lastFailed);
			tag.putInt(FAILED_TARGET_COUNT_TAG, failedCount);
		});
	}

	private static void clearRecentFailedTarget(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putLong(FAILED_TARGET_POS_TAG, 0L);
			tag.putInt(FAILED_TARGET_COUNT_TAG, 0);
		});
	}

	private static void clearFailedTargetTracking(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putLong(FAILED_TARGET_POS_TAG, 0L);
			tag.putInt(FAILED_TARGET_COUNT_TAG, 0);
			tag.remove(SKIPPED_TARGETS_TAG);
		});
	}


	private static boolean isOnlyDamageRequirement(ItemRequirement requirement) {
		List<ItemRequirement.StackRequirement> requiredItems = requirement.getRequiredItems();
		if (requiredItems.isEmpty()) {
			return false;
		}

		for (ItemRequirement.StackRequirement stackRequirement : requiredItems) {
			if (stackRequirement.usage != ItemRequirement.ItemUseType.DAMAGE) {
				return false;
			}
		}

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

	private static int countValidPlacements(ItemStack schematicStack, Level level) {
		int total = 0;
		SchematicPrinter temp = new SchematicPrinter();
		try {
			temp.loadSchematic(schematicStack, level, false);
			while (temp.advanceCurrentPos()) {
				final BlockState[] targetState = { null };
				final BlockPos[] targetPos = { null };
				temp.handleCurrentTarget((pos, state, blockEntity) -> {
					targetPos[0] = pos.immutable();
					targetState[0] = state;
				}, (pos, entityTarget) -> {
				});
				if (targetState[0] == null || targetPos[0] == null)
					continue;
				if (shouldSkipPlacementState(targetState[0]))
					continue;
				total++;
			}
		} catch (Exception ignored) {
			// If counting fails, fall back to 0
		}
		return total;
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
	
	@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.create_nomad.backpack.description_1").withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("tooltip.create_nomad.constructinator.description_1").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.create_nomad.shift_for_info",
                    Component.translatable("key.keyboard.left.shift").withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.GRAY));
        }
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