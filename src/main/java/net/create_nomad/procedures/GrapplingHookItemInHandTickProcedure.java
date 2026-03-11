package net.create_nomad.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec3;

import net.create_nomad.init.GearboundModItems;

public class GrapplingHookItemInHandTickProcedure {

    private static final CustomModelData FIRED_MODEL_DATA = new CustomModelData(1);

    public static void execute(Entity entity) {
        if (!(entity instanceof Player player))
            return;

        boolean clientSide = player.level().isClientSide();

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean holdingMain = mainHand.getItem() == GearboundModItems.GRAPPLING_HOOK.get();
        boolean holdingOff = offHand.getItem() == GearboundModItems.GRAPPLING_HOOK.get();

        boolean grappling = player.getPersistentData().getBoolean("gearbound_grappling");

		if (holdingMain) {
		    if (grappling) {
		        mainHand.set(DataComponents.CUSTOM_MODEL_DATA, FIRED_MODEL_DATA);
		    } else {
		        mainHand.remove(DataComponents.CUSTOM_MODEL_DATA);
		    }
		}
		
		if (holdingOff) {
		    if (grappling) {
		        offHand.set(DataComponents.CUSTOM_MODEL_DATA, FIRED_MODEL_DATA);
		    } else {
		        offHand.remove(DataComponents.CUSTOM_MODEL_DATA);
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
            stack.set(DataComponents.CUSTOM_MODEL_DATA, FIRED_MODEL_DATA);
        } else {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }
}
