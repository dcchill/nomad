package net.create_nomad.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.init.CreateNomadModItems;
import net.create_nomad.util.BackpackDataUtils;
import net.create_nomad.util.BackpackInventoryRules;
import net.create_nomad.util.BackpackItemAssociations;

@EventBusSubscriber(modid = CreateNomadMod.MODID)
public class BackpackInvulnerableUpgradeHandler {
    private BackpackInvulnerableUpgradeHandler() {
    }

    @SubscribeEvent
    public static void onItemInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (!BackpackItemAssociations.isBackpackItem(stack) || !(itemEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        DamageSource damageSource = event.getSource();
        if (damageSource == null || !isFireOrCactusDamage(damageSource)) {
            return;
        }

        ItemStackHandler backpackInventory = BackpackDataUtils.loadHandlerFromItem(stack, serverLevel, BackpackInventoryRules.TOTAL_SLOT_COUNT);
        for (int slot = BackpackInventoryRules.UPGRADE_SLOT_START; slot < BackpackInventoryRules.TOTAL_SLOT_COUNT; slot++) {
            if (backpackInventory.getStackInSlot(slot).is(CreateNomadModItems.INVULNERABLE_UPGRADE.get())) {
                event.setInvulnerable(true);
                return;
            }
        }
    }

    private static boolean isFireOrCactusDamage(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE) || source.is(net.minecraft.world.damagesource.DamageTypes.CACTUS);
    }
}
