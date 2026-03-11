package net.create_nomad.event;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.item.TrackPackItem;
import net.create_nomad.network.BackpackRefillHighlightMessage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber(modid = CreateNomadMod.MODID)
public class TrackPackRailRefillHandler {
    private TrackPackRailRefillHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide())
            return;

        Inventory inventory = player.getInventory();
        boolean changed = false;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty() || !TrackPackItem.isCreateTrack(candidate))
                continue;

            int needed = candidate.getMaxStackSize() - candidate.getCount();
            if (needed <= 0)
                continue;

            int moved = refillFromAnyTrackPack(inventory, slot, candidate, needed);
            if (moved > 0) {
                if (slot < 9 && player instanceof ServerPlayer serverPlayer)
                    PacketDistributor.sendToPlayer(serverPlayer, new BackpackRefillHighlightMessage(slot, true));
                changed = true;
            }
        }

        if (changed)
            inventory.setChanged();
    }

    private static int refillFromAnyTrackPack(Inventory inventory, int ignoredSlot, ItemStack trackStack, int needed) {
        int remaining = needed;

        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            if (slot == ignoredSlot)
                continue;

            ItemStack possiblePack = inventory.getItem(slot);
            if (!(possiblePack.getItem() instanceof TrackPackItem))
                continue;

            int moved = TrackPackItem.transferMatchingTracksToStack(possiblePack, trackStack, remaining);
            if (moved <= 0)
                continue;

            remaining -= moved;
        }

        return needed - remaining;
    }
}
