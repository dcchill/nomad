package net.create_nomad.procedures;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GrapplingHookRightclickedProcedure {
	private static final String CUSTOM_MODEL_DATA_TAG = "CustomModelData";

	public static boolean execute(Entity entity, ItemStack stack) {
		if (!(entity instanceof Player player))
			return false;

		Level level = player.level();

		Vec3 eyePos = player.getEyePosition(1.0F);
		Vec3 lookVec = player.getViewVector(1.0F);
		Vec3 reachVec = eyePos.add(lookVec.scale(50)); // max range

		HitResult hitResult = level.clip(new ClipContext(
				eyePos,
				reachVec,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				player));

		if (hitResult.getType() == HitResult.Type.BLOCK) {
			BlockHitResult blockHit = (BlockHitResult) hitResult;
			Vec3 target = Vec3.atCenterOf(blockHit.getBlockPos());

			player.getPersistentData().putBoolean("gearbound_grappling", true);
			player.getPersistentData().putDouble("gearbound_grapple_progress", 0.0);
			player.getPersistentData().putDouble("gearbound_anchor_x", target.x);
			player.getPersistentData().putDouble("gearbound_anchor_y", target.y);
			player.getPersistentData().putDouble("gearbound_anchor_z", target.z);

			stack.getOrCreateTag().putInt(CUSTOM_MODEL_DATA_TAG, 1);

			// Play whoosh sound
			level.playSound(
					null,
					player.getX(),
					player.getY(),
					player.getZ(),
					SoundEvents.TRIDENT_THROW,
					SoundSource.PLAYERS,
					1.0F,
					1.2F
			);

			return true;
		} else {
			player.getPersistentData().putBoolean("gearbound_grappling", false);
			player.getPersistentData().putDouble("gearbound_grapple_progress", 0.0);

			if (stack.hasTag()) stack.getTag().remove(CUSTOM_MODEL_DATA_TAG);
		}

		return false;
	}
}