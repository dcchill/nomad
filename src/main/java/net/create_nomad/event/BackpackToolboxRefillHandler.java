package net.create_nomad.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.network.BackpackRefillHighlightMessage;
import net.create_nomad.util.BackpackItemAssociations;
import net.create_nomad.util.BackpackInventoryRules;
import net.create_nomad.init.CreateNomadModItems;

@EventBusSubscriber(modid = CreateNomadMod.MODID)
public class BackpackToolboxRefillHandler {
    private static final TagKey<Item> BACKPACKS_TAG = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "backpacks"));
    private static final String BINDING_TAG = "gearboundBackpackBinding";
    private static final int HOTBAR_SLOTS = 9;
    private static final int REFILL_RADIUS = 32;

    private BackpackToolboxRefillHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide())
            return;

        if (player.tickCount % 10 != 0)
            return;

        Inventory inventory = player.getInventory();
        Container boundBackpack = getBoundBackpack(player);
        if (boundBackpack == null)
            return;

        boolean changed = false;
        for (int slot = 0; slot < HOTBAR_SLOTS; slot++) {
            ItemStack current = inventory.getItem(slot);
            if (current.isEmpty())
                continue;

            if (BackpackItemAssociations.isBackpackItem(current))
                continue;

            int needed = current.getMaxStackSize() - current.getCount();
            if (needed <= 0)
                continue;

            ItemStack extracted = extractMatching(boundBackpack, current, needed);
            if (extracted.isEmpty())
                continue;

            current.grow(extracted.getCount());
            if (player instanceof ServerPlayer serverPlayer)
                PacketDistributor.sendToPlayer(serverPlayer, new BackpackRefillHighlightMessage(slot, false));
            changed = true;
        }

        if (changed)
            inventory.setChanged();
    }

    private static Container getBoundBackpack(Player player) {
        var bindingTag = player.getPersistentData().getCompound(BINDING_TAG);
        if (bindingTag.isEmpty())
            return null;

        String dimensionId = bindingTag.getString("dimension");
        ResourceLocation location = ResourceLocation.tryParse(dimensionId);
        if (location == null)
            return null;

        ResourceKey<Level> levelKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location);
        if (!player.level().dimension().equals(levelKey))
            return null;

        BlockPos pos = new BlockPos(bindingTag.getInt("x"), bindingTag.getInt("y"), bindingTag.getInt("z"));
        if (!player.blockPosition().closerThan(pos, REFILL_RADIUS + 0.5))
            return null;

        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof Container container))
            return null;

        ItemStack asItemStack = new ItemStack(blockEntity.getBlockState().getBlock().asItem());
        if (!asItemStack.is(BACKPACKS_TAG))
            return null;

        return container;
    }

    private static ItemStack extractMatching(Container container, ItemStack exactMatchReference, int maxAmount) {
        if (exactMatchReference.isEmpty())
            return ItemStack.EMPTY;

        int remaining = maxAmount;
        ItemStack totalExtracted = ItemStack.EMPTY;

        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack stored = container.getItem(slot);
            if (stored.isEmpty())
                continue;

            boolean matches = ItemStack.isSameItemSameComponents(stored, exactMatchReference);

            if (!matches)
                continue;

            int amount = Math.min(remaining, stored.getCount());
            ItemStack extracted = container.removeItem(slot, amount);
            if (extracted.isEmpty())
                continue;

            container.setChanged();

            if (totalExtracted.isEmpty())
                totalExtracted = extracted;
            else
                totalExtracted.grow(extracted.getCount());

            remaining -= extracted.getCount();
        }

        return totalExtracted;
    }
}
