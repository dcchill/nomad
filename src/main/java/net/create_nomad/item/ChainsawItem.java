package net.create_nomad.item;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.simibubi.create.content.equipment.armor.BacktankUtil;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;


public class ChainsawItem extends AxeItem {
	private static final int MAX_TREE_LOGS = 256;
	private static final int LEAF_SCAN_RANGE = 2;
	private static final int LEAF_SCAN_HEIGHT = 16;
	private static final int BACKTANK_AIR_COST_PER_LOG = 1;
	public static final EnumProxy<HumanoidModel.ArmPose> ARM_POSE = new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) (model, entity, arm) -> {
		if (arm == HumanoidArm.LEFT) {
			model.leftArm.xRot = -1.25F + model.head.xRot;
			model.leftArm.yRot = 0.35F;
		} else {
			model.rightArm.xRot = -1.25F + model.head.xRot;
			model.rightArm.yRot = -0.35F;
		}
	});

	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 1024;
		}

		@Override
		public float getSpeed() {
			return 8f;
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
			return 0;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of();
		}
	};

	public ChainsawItem() {
		super(TOOL_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(TOOL_TIER, 3f, -3.5f)));
	}
		
	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			@Override
			public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
				if (!itemStack.isEmpty() && entityLiving.getItemInHand(hand) == itemStack) {
					return (HumanoidModel.ArmPose) ARM_POSE.getValue();
				}
				return HumanoidModel.ArmPose.EMPTY;
			}

			@Override
			public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
				float armSide = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
				poseStack.translate(armSide * 0.56F, -0.52F, -0.72F);

				float miningProgress = swingProcess;
				if (miningProgress <= 0.0F && player.swinging) {
					float swingTime = player.tickCount + partialTick;
					miningProgress = (float) ((Math.sin(swingTime * 1.35F) + 1.0F) * 0.5F);
				}

				if (miningProgress > 0.0F) {
					float thrust = (float) Math.sin(miningProgress * Math.PI);
					poseStack.translate(0.0F, 0.02F * thrust, 0.16F * thrust);
				}
				return true;
			}
		});
	}




	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);

		if (Screen.hasShiftDown()) {
			tooltip.add(Component.translatable("tooltip.create_nomad.chainsaw.description_1").withStyle(ChatFormatting.WHITE));
			tooltip.add(Component.translatable("tooltip.create_nomad.chainsaw.description_2").withStyle(ChatFormatting.GOLD));
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
		if (mined && durabilityConsumedByInitialBreak > 0
				&& tryConsumeBacktankAir(player, BACKTANK_AIR_COST_PER_LOG)) {
			stack.setDamageValue(Math.max(0, stack.getDamageValue() - durabilityConsumedByInitialBreak));
		}

		if (!isLog(state) || !isBaseLog(level, pos)) {
			return mined;
		}

		Set<BlockPos> connectedLogs = getConnectedLogs(level, pos);
		if (!looksLikeNaturalTree(level, pos, connectedLogs)) {
			return mined;
		}

		for (BlockPos connectedLog : connectedLogs) {
			if (level.getBlockState(connectedLog).isAir()) {
				continue;
			}

			int durabilityBeforeDestroy = stack.getDamageValue();
			boolean destroyed = level.destroyBlock(connectedLog, true, player);
			if (!destroyed) {
				continue;
			}

			int durabilityConsumed = stack.getDamageValue() - durabilityBeforeDestroy;
			if (tryConsumeBacktankAir(player, BACKTANK_AIR_COST_PER_LOG) && durabilityConsumed > 0) {
				stack.setDamageValue(Math.max(0, stack.getDamageValue() - durabilityConsumed));
			}
		}

		return mined;
	}

	@Override
	public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
		return false;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}

	private static Set<BlockPos> getConnectedLogs(Level level, BlockPos startPos) {
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(startPos);

		while (!queue.isEmpty() && visited.size() < MAX_TREE_LOGS) {
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
						if (!visited.contains(neighborPos) && isLog(level.getBlockState(neighborPos))) {
							queue.add(neighborPos);
						}
					}
				}
			}
		}

		visited.remove(startPos);
		return visited;
	}

	private static boolean isBaseLog(Level level, BlockPos pos) {
		return !isLog(level.getBlockState(pos.below()));
	}

	private static boolean looksLikeNaturalTree(Level level, BlockPos startPos, Set<BlockPos> connectedLogs) {
		if (hasNaturalLeavesNearby(level, startPos)) {
			return true;
		}
		for (BlockPos connectedLog : connectedLogs) {
			if (hasNaturalLeavesNearby(level, connectedLog)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasNaturalLeavesNearby(Level level, BlockPos centerPos) {
		for (int xOffset = -LEAF_SCAN_RANGE; xOffset <= LEAF_SCAN_RANGE; xOffset++) {
			for (int yOffset = 0; yOffset <= LEAF_SCAN_HEIGHT; yOffset++) {
				for (int zOffset = -LEAF_SCAN_RANGE; zOffset <= LEAF_SCAN_RANGE; zOffset++) {
					BlockPos scanPos = centerPos.offset(xOffset, yOffset, zOffset);
					BlockState scanState = level.getBlockState(scanPos);
					if (!scanState.is(BlockTags.LEAVES)) {
						continue;
					}

					if (scanState.hasProperty(LeavesBlock.PERSISTENT) && !scanState.getValue(LeavesBlock.PERSISTENT)) {
						return true;
					}
				}
			}
		}
		return false;
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

	private static boolean isLog(BlockState state) {
		return state.is(BlockTags.LOGS) || state.is(BlockTags.CRIMSON_STEMS) || state.is(BlockTags.WARPED_STEMS);
	}
}
