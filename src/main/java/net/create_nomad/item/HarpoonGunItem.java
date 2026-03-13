package net.create_nomad.item;

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
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.model.HumanoidModel;

import net.create_nomad.init.CreateNomadModItems;
import net.create_nomad.entity.HarpoonEntity;
import net.create_nomad.item.renderer.HarpoonGunItemRenderer;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;

public class HarpoonGunItem extends Item implements GeoItem {
	private static final String RELOAD_TICKS_TAG = "harpoonReloadTicks";
	private static final String HAS_AMMO_TAG = "hasHarpoonAmmo";
	private static final int RELOAD_TICKS = 12;
	private static final int FIRE_COOLDOWN_TICKS = 12;
	private static final float SHOT_POWER = 3.8f;
	private static final double SHOT_DAMAGE = 18;
	private static final int SHOT_KNOCKBACK = 2;
	private static final int SHOT_PIERCING = 8;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	public String animationprocedure = "empty";

	public HarpoonGunItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
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
		if (this.animationprocedure.equals("empty")) {
			if (event.getAnimatable() instanceof ItemStack stack && hasCachedAmmo(stack)) {
				event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
			} else {
				event.getController().setAnimation(RawAnimation.begin().thenLoop("idle_no_harpoon"));
			}
			return PlayState.CONTINUE;
		}
		return PlayState.STOP;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack gunStack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(this)) {
			return InteractionResultHolder.fail(gunStack);
		}

		if (!hasAmmo(player)) {
			return InteractionResultHolder.fail(gunStack);
		}

		if (!level.isClientSide) {
			HarpoonEntity projectile = new HarpoonEntity(net.create_nomad.init.CreateNomadModEntities.HARPOON.get(), player, level, gunStack);
			projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, SHOT_POWER, 0.0F);
			projectile.setBaseDamage(SHOT_DAMAGE);
			projectile.setKnockback(SHOT_KNOCKBACK);
			projectile.setPierceLevel((byte) SHOT_PIERCING);
			projectile.pickup = HarpoonEntity.Pickup.DISALLOWED;
			level.addFreshEntity(projectile);
			consumeAmmo(player);
		}

		CustomData.update(DataComponents.CUSTOM_DATA, gunStack, tag -> {
			tag.putString("geckoAnim", "fired");
			tag.putInt(RELOAD_TICKS_TAG, RELOAD_TICKS);
		});
		player.getCooldowns().addCooldown(this, FIRE_COOLDOWN_TICKS);
		return InteractionResultHolder.sidedSuccess(gunStack, level.isClientSide());
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
		super.inventoryTick(stack, level, entity, slotId, isSelected);
		if (!(entity instanceof Player player)) {
			return;
		}

		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(HAS_AMMO_TAG, hasAmmo(player)));

		int reloadTicks = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(RELOAD_TICKS_TAG);
		if (reloadTicks <= 0) {
			return;
		}

		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			int remaining = Math.max(0, tag.getInt(RELOAD_TICKS_TAG) - 1);
			tag.putInt(RELOAD_TICKS_TAG, remaining);
			if (remaining == 0 && hasAmmo(player)) {
				tag.putString("geckoAnim", "reload");
			}
		});
	}

	private static boolean hasCachedAmmo(ItemStack gunStack) {
		return gunStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(HAS_AMMO_TAG);
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
