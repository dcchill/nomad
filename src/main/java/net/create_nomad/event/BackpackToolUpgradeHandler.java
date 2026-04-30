package net.create_nomad.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.init.CreateNomadModItems;
import net.create_nomad.util.BackpackDataUtils;
import net.create_nomad.util.BackpackInventoryRules;
import net.create_nomad.util.BackpackItemAssociations;

@EventBusSubscriber(modid = CreateNomadMod.MODID)
public class BackpackToolUpgradeHandler {
    private BackpackToolUpgradeHandler() {
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel) || player.isShiftKeyDown()) {
            return;
        }

        BackpackRef backpackRef = findFirstBackpackWithToolUpgrade(player, serverLevel);
        if (backpackRef == null) {
            return;
        }

        BlockState state = player.level().getBlockState(event.getPos());
        int selected = player.getInventory().selected;
        ItemStack current = player.getInventory().getItem(selected);
        if (isSuitableTool(current, state)) {
            return;
        }

        int toolSlot = findBestToolSlot(backpackRef.inventory(), state);
        if (toolSlot < 0) {
            return;
        }
        boolean hasInfinityUpgrade = hasInfinityUpgrade(backpackRef.inventory());
        if (!current.isEmpty() && !BackpackInventoryRules.canStoreInBackpack(current, hasInfinityUpgrade, hasInfinityUpgrade)) {
            return;
        }

        ItemStack backpackTool = backpackRef.inventory().getStackInSlot(toolSlot);
        ItemStack replaced = current.copy();

        player.getInventory().setItem(selected, backpackTool.copy());
        backpackRef.inventory().setStackInSlot(toolSlot, replaced);

        BackpackDataUtils.saveHandlerToItem(backpackRef.inventory(), backpackRef.backpackStack(), serverLevel);
        player.getInventory().setChanged();
    }

    private static BackpackRef findFirstBackpackWithToolUpgrade(Player player, ServerLevel level) {
        IItemHandler curios = CreateNomadMod.CuriosApiHelper.getCuriosInventory(player);
        if (curios != null) {
            for (int i = 0; i < curios.getSlots(); i++) {
                ItemStack stack = curios.getStackInSlot(i);
                BackpackRef ref = toToolUpgradeBackpack(stack, level);
                if (ref != null) {
                    return ref;
                }
            }
        }

        int selectedSlot = player.getInventory().selected;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (slot == selectedSlot) {
                continue;
            }

            ItemStack stack = player.getInventory().getItem(slot);
            BackpackRef ref = toToolUpgradeBackpack(stack, level);
            if (ref != null) {
                return ref;
            }
        }

        return null;
    }

    private static boolean hasInfinityUpgrade(ItemStackHandler inventory) {
        for (int slot = BackpackInventoryRules.UPGRADE_SLOT_START; slot < BackpackInventoryRules.TOTAL_SLOT_COUNT; slot++) {
            if (inventory.getStackInSlot(slot).is(CreateNomadModItems.INFINITY_UPGRADE.get())) {
                return true;
            }
        }

        return false;
    }

    private static BackpackRef toToolUpgradeBackpack(ItemStack stack, ServerLevel level) {
        if (!BackpackItemAssociations.isBackpackItem(stack)) {
            return null;
        }

        ItemStackHandler inventory = BackpackDataUtils.loadHandlerFromItem(stack, level, BackpackInventoryRules.TOTAL_SLOT_COUNT);
        for (int slot = BackpackInventoryRules.UPGRADE_SLOT_START; slot < BackpackInventoryRules.TOTAL_SLOT_COUNT; slot++) {
            if (inventory.getStackInSlot(slot).is(CreateNomadModItems.TOOL_UPGRADE.get())) {
                return new BackpackRef(stack, inventory);
            }
        }

        return null;
    }

    private static int findBestToolSlot(ItemStackHandler backpackInventory, BlockState state) {
        ToolType preferredType = preferredToolFor(state);

        int fallback = -1;
        for (int slot = 0; slot < BackpackInventoryRules.STORAGE_SLOT_COUNT; slot++) {
            ItemStack candidate = backpackInventory.getStackInSlot(slot);
            if (candidate.isEmpty()) {
                continue;
            }

            if (!isSuitableTool(candidate, state)) {
                continue;
            }

            if (fallback < 0) {
                fallback = slot;
            }

            if (preferredType == ToolType.ANY || matchesToolType(candidate, preferredType)) {
                return slot;
            }
        }

        return fallback;
    }

    private static boolean isSuitableTool(ItemStack stack, BlockState state) {
        return !stack.isEmpty() && (stack.isCorrectToolForDrops(state) || stack.getDestroySpeed(state) > 1.0F);
    }

    private static ToolType preferredToolFor(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return ToolType.PICKAXE;
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return ToolType.AXE;
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return ToolType.SHOVEL;
        }
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            return ToolType.HOE;
        }
        return ToolType.ANY;
    }

    private static boolean matchesToolType(ItemStack stack, ToolType type) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = itemId == null ? "" : itemId.getPath();

        return switch (type) {
            case PICKAXE -> path.contains("pickaxe");
            case AXE -> path.endsWith("_axe");
            case SHOVEL -> path.contains("shovel") || path.contains("spade");
            case HOE -> path.endsWith("_hoe");
            case ANY -> true;
        };
    }

    private record BackpackRef(ItemStack backpackStack, ItemStackHandler inventory) {
    }

    private enum ToolType {
        PICKAXE,
        AXE,
        SHOVEL,
        HOE,
        ANY
    }
}
