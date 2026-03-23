package net.create_nomad.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.create_nomad.init.CreateNomadModItems;

public class GrapplingHookItemInHandTickProcedure {

    private static final String CUSTOM_MODEL_DATA_TAG = "CustomModelData";

    public static void execute(Entity entity) {
        if (!(entity instanceof Player player))
            return;

        boolean clientSide = player.level().isClientSide();

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean holdingMain = mainHand.getItem() == CreateNomadModItems.GRAPPLING_HOOK.get();
        boolean holdingOff = offHand.getItem() == CreateNomadModItems.GRAPPLING_HOOK.get();

        boolean grappling = player.getPersistentData().getBoolean("gearbound_grappling");

		if (holdingMain) {
		    if (grappling) {
		        mainHand.getOrCreateTag().putInt(CUSTOM_MODEL_DATA_TAG, 1);
		    } else {
		        if (mainHand.hasTag()) mainHand.getTag().remove(CUSTOM_MODEL_DATA_TAG);
		    }
		}
		
		if (holdingOff) {
		    if (grappling) {
		        offHand.getOrCreateTag().putInt(CUSTOM_MODEL_DATA_TAG, 1);
		    } else {
		        if (offHand.hasTag()) offHand.getTag().remove(CUSTOM_MODEL_DATA_TAG);
		    }
		}

        if (!holdingMain && !holdingOff) {
            player.getPersistentData().putBoolean("gearbound_grappling", false);
            player.getPersistentData().putDouble("gearbound_grapple_progress", 0.0);
            return;
        }

        if (!grappling)
            return;

        double progress = player.getPersistentData().getDouble("gearbound_grapple_progress");
        player.getPersistentData().putDouble("gearbound_grapple_progress", Math.min(progress + 0.10, 1.0));

        double x = player.getPersistentData().getDouble("gearbound_anchor_x");
        double y = player.getPersistentData().getDouble("gearbound_anchor_y");
        double z = player.getPersistentData().getDouble("gearbound_anchor_z");

        Vec3 anchor = new Vec3(x, y, z);
        Vec3 playerPos = player.position();

        Vec3 direction = anchor.subtract(playerPos);
        double distance = direction.length();

        if (distance < 1.5) {
            player.getPersistentData().putBoolean("gearbound_grappling", false);
            player.getPersistentData().putDouble("gearbound_grapple_progress", 0.0);
            if (holdingMain) {
                updateHookModel(mainHand, false);
            }
            if (holdingOff) {
                updateHookModel(offHand, false);
            }
            return;
        }

        if (clientSide)
            return;
		Vec3 pull = direction.normalize();
		
		// reduce vertical strength
		pull = new Vec3(pull.x, pull.y * 0.75, pull.z);
		
		double pullStrength = 0.25;
		
		player.setDeltaMovement(player.getDeltaMovement().add(pull.scale(pullStrength)));
        player.hurtMarked = true;
    }

    private static void updateHookModel(ItemStack stack, boolean fired) {
        if (fired) {
            stack.getOrCreateTag().putInt(CUSTOM_MODEL_DATA_TAG, 1);
        } else {
            if (stack.hasTag()) stack.getTag().remove(CUSTOM_MODEL_DATA_TAG);
        }
    }
}
