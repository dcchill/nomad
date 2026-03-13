package net.create_nomad.item;

import com.simibubi.create.content.equipment.armor.BacktankUtil;

import net.create_nomad.item.renderer.JackhammerItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JackhammerItem extends PickaxeItem implements GeoItem {
	private static final int MAX_VEIN_BLOCKS = 128;
	private static final int BACKTANK_AIR_COST_PER_BLOCK = 1;
	private static final TagKey<Block> C_ORES_TAG = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
	private static final TagKey<Block> FORGE_ORES_TAG = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", "ores"));
	public static final EnumProxy<HumanoidModel.ArmPose> ARM_POSE = new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) (model, entity, arm) -> {
		if (arm == HumanoidArm.LEFT) {
			model.leftArm.xRot = -1.35F + model.head.xRot;
			model.leftArm.yRot = 0.22F;
		} else {
			model.rightArm.xRot = -1.35F + model.head.xRot;
			model.rightArm.yRot = -0.22F;
		}
	});

	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 1024;
		}

		@Override
		public float getSpeed() {
			return 21f;
		}

		@Override
		public float getAttackDamageBonus() {
			return 0;
		}

		@Override
		public TagKey<Block> getIncorrectBlocksForDrops() {
			return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
		}

		@Override
		public int getEnchantmentValue() {
			return 3;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of();
		}
	};

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	public String animationprocedure = "empty";

	public JackhammerItem() {
		super(TOOL_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(TOOL_TIER, 3f, -3f)));
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			@Override
			public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
				// Keep this as a hand transform only; custom ArmPose enum extensions are not configured for this item.
				float armSide = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
				poseStack.translate(armSide * 0.56F, -0.52F, -0.72F);
				return true;
			}
		});
	}


	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);

		if (Screen.hasShiftDown()) {
			tooltip.add(Component.translatable("tooltip.create_nomad.jackhammer.description_1").withStyle(ChatFormatting.WHITE));
			tooltip.add(Component.translatable("tooltip.create_nomad.jackhammer.description_2").withStyle(ChatFormatting.GOLD));
		} else {
			tooltip.add(Component.translatable("tooltip.create_nomad.shift_for_info",
					Component.translatable("key.keyboard.left.shift").withStyle(ChatFormatting.YELLOW))
					.withStyle(ChatFormatting.GRAY));
		}
	}


	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
		int durabilityBeforeBreak = stack.getDamageValue();
		boolean mined = super.mineBlock(stack, level, state, pos, entity);
		if (level.isClientSide || !(entity instanceof Player player)) {
			return mined;
		}

		int durabilityConsumedByInitialBreak = stack.getDamageValue() - durabilityBeforeBreak;
		if (mined && durabilityConsumedByInitialBreak > 0 && tryConsumeBacktankAir(player, BACKTANK_AIR_COST_PER_BLOCK)) {
			stack.setDamageValue(Math.max(0, stack.getDamageValue() - durabilityConsumedByInitialBreak));
		}

		if (!isVeinMineable(state)) {
			return mined;
		}

		Set<BlockPos> connectedVein = getConnectedVein(level, pos, state.getBlock());
		for (BlockPos connectedOre : connectedVein) {
			if (level.getBlockState(connectedOre).isAir()) {
				continue;
			}

			int durabilityBeforeDestroy = stack.getDamageValue();
			boolean destroyed = level.destroyBlock(connectedOre, true, player);
			if (!destroyed) {
				continue;
			}

			int durabilityConsumed = stack.getDamageValue() - durabilityBeforeDestroy;
			if (durabilityConsumed > 0 && tryConsumeBacktankAir(player, BACKTANK_AIR_COST_PER_BLOCK)) {
				stack.setDamageValue(Math.max(0, stack.getDamageValue() - durabilityConsumed));
			}
		}

		return mined;
	}

	@Override
	public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("geckoAnim", "mining"));
		return true;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
		super.inventoryTick(stack, level, entity, slotId, isSelected);
		if (level.isClientSide && entity instanceof LivingEntity livingEntity && !livingEntity.swinging && this.animationprocedure.equals("mining")) {
			this.animationprocedure = "empty";
		}
	}



	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}

	@Override
	public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
		consumer.accept(new GeoRenderProvider() {
			private JackhammerItemRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
				if (this.renderer == null)
					this.renderer = new JackhammerItemRenderer();
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

	private static Set<BlockPos> getConnectedVein(Level level, BlockPos startPos, Block targetBlock) {
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(startPos);

		while (!queue.isEmpty() && visited.size() < MAX_VEIN_BLOCKS) {
			BlockPos currentPos = queue.removeFirst();
			if (!visited.add(currentPos)) {
				continue;
			}

			for (int xOffset = -1; xOffset <= 1; xOffset++) {
				for (int yOffset = -1; yOffset <= 1; yOffset++) {
					for (int zOffset = -1; zOffset <= 1; zOffset++) {
						if (xOffset == 0 && yOffset == 0 && zOffset == 0) {
							continue;
						}

						BlockPos neighborPos = currentPos.offset(xOffset, yOffset, zOffset);
						if (!visited.contains(neighborPos) && level.getBlockState(neighborPos).is(targetBlock)) {
							queue.add(neighborPos);
						}
					}
				}
			}
		}

		visited.remove(startPos);
		return visited;
	}

	private static boolean isVeinMineable(BlockState state) {
		return state.is(BlockTags.COAL_ORES) || state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.IRON_ORES)
				|| state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.REDSTONE_ORES)
				|| state.is(BlockTags.EMERALD_ORES) || state.is(BlockTags.LAPIS_ORES)
				|| state.is(BlockTags.DIAMOND_ORES) || state.is(Blocks.ANCIENT_DEBRIS)
				|| state.is(C_ORES_TAG) || state.is(FORGE_ORES_TAG);
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
}
