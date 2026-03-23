package net.create_nomad.item;

import com.simibubi.create.content.equipment.armor.BacktankUtil;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.GeoItem;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.model.HumanoidModel;

import net.create_nomad.init.CreateNomadModItems;
import net.create_nomad.entity.HarpoonEntity;
import net.create_nomad.item.renderer.HarpoonGunItemRenderer;

import java.util.function.Consumer;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

public class HarpoonGunItem extends Item implements GeoItem {
	private static final String ACTION_TICKS_TAG = "harpoonActionTicks";
	private static final String LOADED_TAG = "harpoonLoaded";
	private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation RELOAD_ANIMATION = RawAnimation.begin().thenPlay("reload");
	private static final RawAnimation FIRED_ANIMATION = RawAnimation.begin().thenPlay("fired");
	private static final int RELOAD_TICKS = 20;
	private static final int FIRE_COOLDOWN_TICKS = 10;
	private static final int BACKTANK_AIR_COST_PER_SHOT = 10;
	private static final float SHOT_POWER = 3.8f;
	private static final double SHOT_DAMAGE = 6;
	private static final int SHOT_KNOCKBACK = 0;
	private static final int SHOT_PIERCING = 8;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	public String animationprocedure = "empty";

	public HarpoonGunItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
		SingletonGeoAnimatable.registerSyncedAnimatable(this);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}

	@Override
	public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
		consumer.accept(new GeoRenderProvider() {
			private HarpoonGunItemRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
				if (this.renderer == null)
					this.renderer = new HarpoonGunItemRenderer();
				return this.renderer;
			}
		});
	}

	public static final EnumProxy<HumanoidModel.ArmPose> ARM_POSE = new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) (model, entity, arm) -> {
		if (arm == HumanoidArm.LEFT) {
			model.leftArm.xRot = -45F + model.head.xRot;
		} else {
			model.rightArm.xRot = -45F + model.head.xRot;
		}
	});

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		super.initializeClient(consumer);
		consumer.accept(new IClientItemExtensions() {
			@Override
			public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
				if (!itemStack.isEmpty()) {
					if (entityLiving.getUsedItemHand() == hand) {
						return (HumanoidModel.ArmPose) ARM_POSE.getValue();
					}
				}
				return HumanoidModel.ArmPose.EMPTY;
			}

			public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
				int i = arm == HumanoidArm.RIGHT ? 1 : -1;
				poseStack.translate(i * 0.56F, -0.52F, -0.72F);
				if (player.getUseItem() == itemInHand) {
					poseStack.translate(0.05, 0.05, 0.05);
				}
				return true;
			}
		});
	}

	private PlayState idlePredicate(AnimationState event) {
		event.getController().setAnimation(IDLE_ANIMATION);
		return PlayState.CONTINUE;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack gunStack = player.getItemInHand(hand);
		boolean loaded = isLoaded(gunStack);

		if (getActionTicks(gunStack) > 0) {
			return InteractionResultHolder.fail(gunStack);
		}

		// Reload
		if (!loaded) {
			if (!hasAmmo(player)) {
				return InteractionResultHolder.fail(gunStack);
			}

			if (!level.isClientSide) {
				consumeAmmo(player);

				CustomData.update(DataComponents.CUSTOM_DATA, gunStack, tag -> {
					tag.putBoolean(LOADED_TAG, true);
					tag.putInt(ACTION_TICKS_TAG, RELOAD_TICKS);
				});

				if (level instanceof ServerLevel serverLevel) {
					triggerAnim(player, GeoItem.getOrAssignId(gunStack, serverLevel), "reloadController", "reload");
				}
			}

			return InteractionResultHolder.sidedSuccess(gunStack, level.isClientSide());
		}

		// Fire
		if (!tryConsumeBacktankAir(player, BACKTANK_AIR_COST_PER_SHOT)) {
			return InteractionResultHolder.fail(gunStack);
		}

		if (!level.isClientSide) {
			int multishot = net.minecraft.world.item.enchantment.EnchantmentHelper
				.getItemEnchantmentLevel(
					level.registryAccess()
						.registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
						.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.MULTISHOT),
					gunStack
				);

			int shots = multishot > 0 ? 3 : 1;

			for (int i = 0; i < shots; i++) {
				float spread = 0;
				if (shots == 3) {
					spread = (i - 1) * 10f; // -10, 0, +10 degrees
				}

				HarpoonEntity projectile = new HarpoonEntity(
					net.create_nomad.init.CreateNomadModEntities.HARPOON.get(), player, level, gunStack);

				projectile.shootFromRotation(
					player,
					player.getXRot(),
					player.getYRot() + spread,
					0.0F,
					SHOT_POWER,
					0.0F
				);

				int powerLevel = net.minecraft.world.item.enchantment.EnchantmentHelper
					.getItemEnchantmentLevel(
						level.registryAccess()
							.registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
							.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER),
						gunStack
					);

				double damage = SHOT_DAMAGE + (powerLevel * 1.5) + 0.5;

				projectile.setBaseDamage(damage);
				projectile.setKnockback(SHOT_KNOCKBACK);
				projectile.setHarpoonPierceLevel(SHOT_PIERCING);
				projectile.pickup = HarpoonEntity.Pickup.DISALLOWED;

				level.addFreshEntity(projectile);
			}

			spawnSteamPuff((ServerLevel) level, player);
		}

		CustomData.update(DataComponents.CUSTOM_DATA, gunStack, tag -> {
			tag.putBoolean(LOADED_TAG, false);
			tag.putInt(ACTION_TICKS_TAG, FIRE_COOLDOWN_TICKS);
		});

		if (level instanceof ServerLevel serverLevel) {
			triggerAnim(player, GeoItem.getOrAssignId(gunStack, serverLevel), "fireController", "fired");
		}

		return InteractionResultHolder.sidedSuccess(gunStack, level.isClientSide());
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
		super.inventoryTick(stack, level, entity, slotId, isSelected);
		if (!(entity instanceof Player)) {
			return;
		}

		if (level.isClientSide) {
			return;
		}

		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			int remaining = Math.max(0, tag.getInt(ACTION_TICKS_TAG) - 1);
			tag.putInt(ACTION_TICKS_TAG, remaining);
		});
	}

	public static boolean isLoaded(ItemStack gunStack) {
		CustomData customData = gunStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		if (!customData.copyTag().contains(LOADED_TAG)) {
			return true;
		}
		return customData.copyTag().getBoolean(LOADED_TAG);
	}

	private static int getActionTicks(ItemStack gunStack) {
		return gunStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(ACTION_TICKS_TAG);
	}

	private static boolean hasAmmo(Player player) {
		if (player.getAbilities().instabuild) {
			return true;
		}

		for (ItemStack stack : player.getInventory().offhand) {
			if (stack.is(CreateNomadModItems.HARPOON_ITEM.get()) && !stack.isEmpty()) {
				return true;
			}
		}

		for (ItemStack stack : player.getInventory().items) {
			if (stack.is(CreateNomadModItems.HARPOON_ITEM.get()) && !stack.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private static void consumeAmmo(Player player) {
		if (player.getAbilities().instabuild) {
			return;
		}

		for (ItemStack stack : player.getInventory().offhand) {
			if (stack.is(CreateNomadModItems.HARPOON_ITEM.get()) && !stack.isEmpty()) {
				stack.shrink(1);
				return;
			}
		}

		for (ItemStack stack : player.getInventory().items) {
			if (stack.is(CreateNomadModItems.HARPOON_ITEM.get()) && !stack.isEmpty()) {
				stack.shrink(1);
				return;
			}
		}
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

	private static void spawnSteamPuff(ServerLevel level, Player player) {
		double x = player.getX() + player.getLookAngle().x * 0.8;
		double y = player.getEyeY() - 0.2 + player.getLookAngle().y * 0.8;
		double z = player.getZ() + player.getLookAngle().z * 0.8;
		level.sendParticles(ParticleTypes.CLOUD, x, y, z, 10, 0.08, 0.08, 0.08, 0.03);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar data) {
		AnimationController idleController = new AnimationController(this, "idleController", 0, this::idlePredicate);
		AnimationController reloadController = new AnimationController(this, "reloadController", 0, state -> PlayState.STOP)
			.triggerableAnim("reload", RELOAD_ANIMATION);
		AnimationController fireController = new AnimationController(this, "fireController", 0, state -> PlayState.STOP)
			.triggerableAnim("fired", FIRED_ANIMATION);

		data.add(idleController);
		data.add(reloadController);
		data.add(fireController);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
