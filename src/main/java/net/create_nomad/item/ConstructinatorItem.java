package net.create_nomad.item;

import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

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
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ConstructinatorItem extends Item implements GeoItem {
	private static final String PRINTER_TAG = "constructinatorPrinter";
	private static final String GECKO_ANIM_TAG = "geckoAnim";
	private static final String SCHEMATIC_FILE_TAG = "constructinatorSchematicFile";
	private static final int PLACE_INTERVAL_TICKS = 2;
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	public String animationprocedure = "empty";

	public ConstructinatorItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
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

			ItemRequirement requirement = printer.getCurrentRequirement();
			if (requirement.isInvalid()) {
				storePrinterState(constructinatorStack, printer, schematicFile);
				return false;
			}

			if (!canMeetRequirement(player, level, requirement)) {
				storePrinterState(constructinatorStack, printer, schematicFile);
				return true;
			}

			if (!consumeRequirement(player, level, requirement)) {
				storePrinterState(constructinatorStack, printer, schematicFile);
				return true;
			}

			final boolean[] placementSucceeded = { false };
			printer.handleCurrentTarget((pos, state, blockEntity) -> placeBlock(level, pos, state, blockEntity, placementSucceeded), (pos, entityTarget) -> {
			});

			if (placementSucceeded[0]) {
				placed = true;
				printer.sendBlockUpdates(level);
				CustomData.update(DataComponents.CUSTOM_DATA, constructinatorStack, tag -> tag.putString(GECKO_ANIM_TAG, "fire"));
			}
		}

		storePrinterState(constructinatorStack, printer, schematicFile);
		return true;
	}

	private static void placeBlock(ServerLevel level, net.minecraft.core.BlockPos pos, BlockState state, BlockEntity targetBlockEntity, boolean[] placementSucceeded) {
		if (state.isAir()) {
			placementSucceeded[0] = true;
			return;
		}

		boolean placed = level.setBlock(pos, state, 3);
		if (placed) {
			placementSucceeded[0] = true;
		}
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
		AnimationController procedureController = new AnimationController(this, "procedureController", 0, this::procedurePredicate);
		data.add(procedureController);
		AnimationController idleController = new AnimationController(this, "idleController", 0, this::idlePredicate);
		data.add(idleController);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
