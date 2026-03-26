package net.create_nomad.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.init.CreateNomadModItems;
import net.create_nomad.util.BackpackDataUtils;
import net.create_nomad.util.BackpackInventoryRules;
import net.create_nomad.util.BackpackItemAssociations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = CreateNomadMod.MODID)
public class BackpackMagnetUpgradeHandler {
    private static final double MAGNET_RANGE = 6.0D;
    private static final double AUTO_INSERT_RANGE = 1.35D;

    private BackpackMagnetUpgradeHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (player.tickCount % 5 != 0) {
            return;
        }

        processWornBackpackMagnet(player, serverLevel);
        processPlacedBackpackMagnetsNearPlayer(player, serverLevel);
    }

    private static void processWornBackpackMagnet(Player player, ServerLevel serverLevel) {
        ItemStack backpackStack = findFirstCuriosBackpack(player);
        if (backpackStack.isEmpty()) {
            return;
        }

        ItemStackHandler backpackInventory = BackpackDataUtils.loadHandlerFromItem(
                backpackStack,
                serverLevel,
                BackpackInventoryRules.TOTAL_SLOT_COUNT);

        if (!hasMagnetUpgrade(backpackInventory)) {
            return;
        }

        boolean changed = pullItemsTowardsAndInsert(
                serverLevel,
                player.position().add(0, 0.5, 0),
                () -> {
                },
                stack -> insertIntoBackpackStorage(backpackInventory, stack));

        if (changed) {
            BackpackDataUtils.saveHandlerToItem(backpackInventory, backpackStack, serverLevel);
        }
    }

    private static void processPlacedBackpackMagnetsNearPlayer(Player player, ServerLevel serverLevel) {
        BlockPos playerPos = player.blockPosition();
        int blockRange = (int) Math.ceil(MAGNET_RANGE);
        Set<BlockPos> processedPositions = new HashSet<>();

        for (int x = -blockRange; x <= blockRange; x++) {
            for (int y = -blockRange; y <= blockRange; y++) {
                for (int z = -blockRange; z <= blockRange; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (!processedPositions.add(pos.immutable())) {
                        continue;
                    }

                    BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
                    if (!(blockEntity instanceof Container container) || !isBackpackBlockEntity(blockEntity)) {
                        continue;
                    }

                    if (!hasMagnetUpgrade(container)) {
                        continue;
                    }

                    Vec3 target = Vec3.atCenterOf(pos).add(0, -0.15, 0);
                    boolean changed = pullItemsTowardsAndInsert(
                            serverLevel,
                            target,
                            container::setChanged,
                            stack -> insertIntoBackpackStorage(container, stack));

                    if (changed) {
                        blockEntity.setChanged();
                    }
                }
            }
        }
    }

    private static boolean pullItemsTowardsAndInsert(ServerLevel level, Vec3 target, Runnable markDirty, Inserter inserter) {
        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(target, target).inflate(MAGNET_RANGE),
                itemEntity -> itemEntity.isAlive() && !itemEntity.hasPickUpDelay() && !itemEntity.getItem().isEmpty());

        boolean changed = false;
        for (ItemEntity itemEntity : nearbyItems) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty() || !BackpackInventoryRules.canStoreInBackpack(stack)) {
                continue;
            }

            Vec3 offset = target.subtract(itemEntity.position());
            double distanceSqr = offset.lengthSqr();
            if (distanceSqr > 0.0001) {
                Vec3 pull = offset.normalize().scale(0.35);
                itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().add(pull));
                itemEntity.hurtMarked = true;
            }

            if (distanceSqr > AUTO_INSERT_RANGE * AUTO_INSERT_RANGE) {
                continue;
            }

            ItemStack remaining = inserter.insert(stack.copy());
            int inserted = stack.getCount() - remaining.getCount();
            if (inserted <= 0) {
                continue;
            }

            changed = true;
            if (remaining.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remaining);
            }
        }

        if (changed) {
            markDirty.run();
        }
        return changed;
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

    private static boolean hasMagnetUpgrade(ItemStackHandler backpackInventory) {
        for (int slot = BackpackInventoryRules.UPGRADE_SLOT_START; slot < BackpackInventoryRules.TOTAL_SLOT_COUNT; slot++) {
            if (backpackInventory.getStackInSlot(slot).is(CreateNomadModItems.MAGNET_UPGRADE.get())) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasMagnetUpgrade(Container container) {
        for (int slot = BackpackInventoryRules.UPGRADE_SLOT_START; slot < BackpackInventoryRules.TOTAL_SLOT_COUNT; slot++) {
            if (container.getItem(slot).is(CreateNomadModItems.MAGNET_UPGRADE.get())) {
                return true;
            }
        }

        return false;
    }

    private static ItemStack insertIntoBackpackStorage(ItemStackHandler backpackInventory, ItemStack stack) {
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < BackpackInventoryRules.STORAGE_SLOT_COUNT; slot++) {
            ItemStack slotStack = backpackInventory.getStackInSlot(slot);
            if (!slotStack.isEmpty() && !ItemStack.isSameItemSameComponents(slotStack, remainder)) {
                continue;
            }

            int max = Math.min(backpackInventory.getSlotLimit(slot), remainder.getMaxStackSize());
            int moved;
            if (slotStack.isEmpty()) {
                moved = Math.min(max, remainder.getCount());
                ItemStack inserted = remainder.copyWithCount(moved);
                backpackInventory.setStackInSlot(slot, inserted);
            } else {
                int space = max - slotStack.getCount();
                if (space <= 0) {
                    continue;
                }
                moved = Math.min(space, remainder.getCount());
                slotStack.grow(moved);
            }

            remainder.shrink(moved);
            if (remainder.isEmpty())
                return ItemStack.EMPTY;
        }

        return remainder;
    }

    private static ItemStack insertIntoBackpackStorage(Container container, ItemStack stack) {
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < BackpackInventoryRules.STORAGE_SLOT_COUNT; slot++) {
            ItemStack slotStack = container.getItem(slot);
            if (!slotStack.isEmpty() && !ItemStack.isSameItemSameComponents(slotStack, remainder)) {
                continue;
            }

            int max = Math.min(container.getMaxStackSize(), remainder.getMaxStackSize());
            int moved;
            if (slotStack.isEmpty()) {
                moved = Math.min(max, remainder.getCount());
                ItemStack inserted = remainder.copyWithCount(moved);
                container.setItem(slot, inserted);
            } else {
                int space = max - slotStack.getCount();
                if (space <= 0) {
                    continue;
                }
                moved = Math.min(space, remainder.getCount());
                slotStack.grow(moved);
            }

            remainder.shrink(moved);
            if (remainder.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        return remainder;
    }

    private static boolean isBackpackBlockEntity(BlockEntity blockEntity) {
        String simpleName = blockEntity.getClass().getSimpleName();
        return simpleName.endsWith("BrassBackpackBlockEntity");
    }

    @FunctionalInterface
    private interface Inserter {
        ItemStack insert(ItemStack stack);
    }
}
