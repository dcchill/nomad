package net.create_nomad.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.init.CreateNomadModItems;
import net.create_nomad.util.BackpackDataUtils;
import net.create_nomad.util.BackpackInventoryRules;
import net.create_nomad.util.BackpackItemAssociations;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = CreateNomadMod.MODID)
public class BackpackFallUpgradeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("BackpackFallUpgradeHandler");
    private static final Set<UUID> playersWithWaterPlaced = new HashSet<>();
    private static final Map<UUID, Double> playerFallDistance = new java.util.HashMap<>();

    private BackpackFallUpgradeHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        UUID playerId = player.getUUID();
        
        // Track fall distance
        if (!player.onGround() && player.getDeltaMovement().y < 0) {
            playerFallDistance.merge(playerId, Math.abs(player.getDeltaMovement().y), Double::sum);
        } else {
            playerFallDistance.remove(playerId);
            if (playersWithWaterPlaced.contains(playerId)) {
                playersWithWaterPlaced.remove(playerId);
            }
            return;
        }

        // Get accumulated fall distance
        double fallDistance = playerFallDistance.getOrDefault(playerId, 0.0);
        
        // Need at least 10 blocks of fall to trigger
        if (fallDistance < 10.0) {
            return;
        }

        // Find the backpack the player is wearing using Curios API
        ItemStack backpackStack = findEquippedBackpack(player);
        if (backpackStack.isEmpty()) {
            LOGGER.debug("No backpack found for player {}", player.getName().getString());
            return;
        }
        
        if (!BackpackItemAssociations.isBackpackItem(backpackStack)) {
            LOGGER.debug("Backpack {} is not recognized as backpack item", backpackStack.getItem());
            return;
        }

        // Load backpack inventory
        ItemStackHandler backpackInventory = BackpackDataUtils.loadHandlerFromItem(
                backpackStack, serverLevel, BackpackInventoryRules.TOTAL_SLOT_COUNT);

        // Check if FallUpgrade is installed in upgrade slots
        boolean hasFallUpgrade = false;
        for (int slot = BackpackInventoryRules.UPGRADE_SLOT_START; slot < BackpackInventoryRules.TOTAL_SLOT_COUNT; slot++) {
            if (backpackInventory.getStackInSlot(slot).is(CreateNomadModItems.FALL_UPGRADE.get())) {
                hasFallUpgrade = true;
                LOGGER.debug("Found FallUpgrade in slot {}", slot);
                break;
            }
        }

        if (!hasFallUpgrade) {
            LOGGER.debug("No FallUpgrade found in backpack");
            return;
        }

        // Check if player has a water bucket in storage slots
        int waterBucketSlot = -1;
        for (int slot = 0; slot < BackpackInventoryRules.STORAGE_SLOT_COUNT; slot++) {
            ItemStack stack = backpackInventory.getStackInSlot(slot);
            if (stack.is(Items.WATER_BUCKET)) {
                waterBucketSlot = slot;
                LOGGER.debug("Found water bucket in slot {}", slot);
                break;
            }
        }

        if (waterBucketSlot < 0) {
            LOGGER.debug("No water bucket found in backpack");
            return;
        }

        // Check if player is VERY close to the ground (within 3 blocks)
        BlockPos playerPos = player.blockPosition();
        int distanceToGround = 0;
        for (int i = 1; i <= 10; i++) {
            if (serverLevel.getBlockState(playerPos.below(i)).isSolid()) {
                distanceToGround = i;
                break;
            }
        }

        // Only place water when within 3 blocks of the ground (almost landing)
        if (distanceToGround > 3 || distanceToGround == 0) {
            return;
        }

        // Check if player is crouching (shift key)
        if (!player.isCrouching()) {
            LOGGER.debug("Player not crouching, not placing water");
            return;
        }

        // Calculate fall damage (Minecraft formula: fallDistance - 3)
        float damage = (float) (fallDistance - 3.0);
        float health = player.getHealth();
        float armorReduction = getArmorReduction(player);
        float effectiveDamage = damage * (1.0f - armorReduction);

        LOGGER.debug("Fall check: fallDistance={}, distanceToGround={}, damage={}, health={}, armorReduction={}, effectiveDamage={}", 
            fallDistance, distanceToGround, damage, health, armorReduction, effectiveDamage);

        // Activate for any fall that would cause significant damage (more than 3 hearts / 6 HP)
        if (effectiveDamage < 6.0f) {
            LOGGER.debug("Fall damage {} too low to trigger (need 6+)", effectiveDamage);
            return;
        }

        // Don't place water multiple times for same fall
        if (playersWithWaterPlaced.contains(playerId)) {
            LOGGER.debug("Water already placed for this fall");
            return;
        }

        // Try to place water under the player to break the fall
        BlockPos waterPos = playerPos.below();

        LOGGER.debug("Attempting to place water at {}", waterPos);

        // Check if we can place water below the player
        if (canPlaceWater(serverLevel, waterPos)) {
            // Place water
            serverLevel.setBlock(waterPos, Blocks.WATER.defaultBlockState(), 3);
            LOGGER.info("Placed water at {} for player {} (fall distance: {}, ground distance: {})", waterPos, player.getName().getString(), fallDistance, distanceToGround);

            // Replace water bucket with empty bucket in the same slot
            backpackInventory.setStackInSlot(waterBucketSlot, new ItemStack(Items.BUCKET));

            // Save backpack inventory
            BackpackDataUtils.saveHandlerToItem(backpackInventory, backpackStack, serverLevel);

            // Mark player as having water placed
            playersWithWaterPlaced.add(playerId);

            // Reset fall distance tracking
            playerFallDistance.remove(playerId);

            // Schedule water pickup after a short delay (20 ticks = 1 second)
            CreateNomadMod.queueServerWork(20, () -> pickupWater(serverLevel, player, waterPos, backpackStack));
        } else {
            LOGGER.debug("Cannot place water at {} - invalid position", waterPos);
        }
    }

    private static float getArmorReduction(Player player) {
        // Simplified armor calculation - Minecraft's actual formula is more complex
        int totalArmor = player.getArmorValue();
        return Math.min(0.8f, totalArmor / 20.0f);
    }

    private static boolean canPlaceWater(ServerLevel level, BlockPos pos) {
        // Check if the block below is solid (can support water)
        BlockPos belowWater = pos.below();
        boolean hasSolidBelow = level.getBlockState(belowWater).isSolid();
        
        // Check if the water position can be replaced
        boolean canReplace = level.getBlockState(pos).canBeReplaced() || level.getBlockState(pos).isAir();
        
        LOGGER.debug("canPlaceWater: pos={}, hasSolidBelow={}, canReplace={}", pos, hasSolidBelow, canReplace);
        
        // Allow placing water if there's a solid block below OR if we're close to ground
        if (!hasSolidBelow) {
            // Check if there's any solid block within 5 blocks below
            for (int i = 1; i <= 5; i++) {
                if (level.getBlockState(pos.below(i)).isSolid()) {
                    hasSolidBelow = true;
                    break;
                }
            }
        }
        
        return hasSolidBelow && canReplace;
    }

    private static void pickupWater(ServerLevel level, Player player, BlockPos waterPos, ItemStack backpackStack) {
        // Check if player is still alive and in the same dimension
        if (player.isAlive() && player.level() == level) {
            // Check if water is still there
            if (level.getBlockState(waterPos).getBlock() instanceof LiquidBlock) {
                // Remove the water
                level.setBlock(waterPos, Blocks.AIR.defaultBlockState(), 3);

                // Reload backpack inventory
                ItemStackHandler backpackInventory = BackpackDataUtils.loadHandlerFromItem(
                        backpackStack, level, BackpackInventoryRules.TOTAL_SLOT_COUNT);

                // Try to find the empty bucket slot and replace it with water bucket
                boolean placed = false;
                for (int slot = 0; slot < BackpackInventoryRules.STORAGE_SLOT_COUNT; slot++) {
                    ItemStack stack = backpackInventory.getStackInSlot(slot);
                    if (stack.is(Items.BUCKET) && stack.getCount() == 1) {
                        backpackInventory.setStackInSlot(slot, new ItemStack(Items.WATER_BUCKET));
                        placed = true;
                        break;
                    }
                }

                // If no empty bucket found, try to add water bucket to inventory
                if (!placed) {
                    ItemStack waterBucket = new ItemStack(Items.WATER_BUCKET);
                    addStackToBackpack(backpackInventory, waterBucket, -1);
                }

                // Save backpack inventory
                BackpackDataUtils.saveHandlerToItem(backpackInventory, backpackStack, level);
            }
        }

        // Clean up player tracking
        if (player != null) {
            playersWithWaterPlaced.remove(player.getUUID());
        }
    }

    private static ItemStack findEquippedBackpack(Player player) {
        // Use the same approach as other handlers in this project
        net.neoforged.neoforge.items.IItemHandler curiosInventory = CreateNomadMod.CuriosApiHelper.getCuriosInventory(player);
        
        if (curiosInventory == null) {
            LOGGER.debug("Curios inventory is null for player {}", player.getName().getString());
            return ItemStack.EMPTY;
        }
        
        LOGGER.debug("Curios inventory found with {} slots", curiosInventory.getSlots());
        
        for (int i = 0; i < curiosInventory.getSlots(); i++) {
            ItemStack stack = curiosInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                LOGGER.debug("Curios slot {}: {}", i, stack.getItem());
            }
            if (BackpackItemAssociations.isBackpackItem(stack)) {
                LOGGER.debug("Found backpack in curios slot {}: {}", i, stack.getItem());
                return stack;
            }
        }
        
        LOGGER.debug("No backpack found in Curios inventory");
        return ItemStack.EMPTY;
    }

    private static void addStackToBackpack(ItemStackHandler inventory, ItemStack stack, int preferredSlot) {
        // Try to stack with existing items first
        for (int slot = 0; slot < BackpackInventoryRules.STORAGE_SLOT_COUNT; slot++) {
            ItemStack existing = inventory.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                int canAdd = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(canAdd);
                stack.shrink(canAdd);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }

        // Find empty slot
        int slotToUse = preferredSlot >= 0 && preferredSlot < BackpackInventoryRules.STORAGE_SLOT_COUNT ? preferredSlot : -1;
        if (slotToUse >= 0 && inventory.getStackInSlot(slotToUse).isEmpty()) {
            inventory.setStackInSlot(slotToUse, stack);
            return;
        }

        // Find any empty slot
        for (int slot = 0; slot < BackpackInventoryRules.STORAGE_SLOT_COUNT; slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                inventory.setStackInSlot(slot, stack);
                return;
            }
        }
    }
}
