package net.create_nomad.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.init.CreateNomadModItems;
import net.create_nomad.util.BackpackDataUtils;
import net.create_nomad.util.BackpackInventoryRules;
import net.create_nomad.util.BackpackItemAssociations;

import java.util.List;

@EventBusSubscriber(modid = CreateNomadMod.MODID)
public class BackpackMagnetUpgradeHandler {
    private static final double MAGNET_RANGE = 6.0D;

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

        List<ItemEntity> nearbyItems = player.level().getEntitiesOfClass(
                ItemEntity.class,
                player.getBoundingBox().inflate(MAGNET_RANGE),
                itemEntity -> itemEntity.isAlive() && !itemEntity.hasPickUpDelay() && !itemEntity.getItem().isEmpty());

        boolean changed = false;
        for (ItemEntity itemEntity : nearbyItems) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            Vec3 offset = player.position().add(0, 0.5, 0).subtract(itemEntity.position());
            if (offset.lengthSqr() > 0.0001) {
                Vec3 pull = offset.normalize().scale(0.35);
                itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().add(pull));
                itemEntity.hurtMarked = true;
            }

            ItemStack remaining = insertIntoBackpackStorage(backpackInventory, stack.copy());
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
            BackpackDataUtils.saveHandlerToItem(backpackInventory, backpackStack, serverLevel);
        }
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
}
