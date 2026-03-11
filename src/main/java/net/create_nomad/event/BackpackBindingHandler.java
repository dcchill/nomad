package net.create_nomad.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.create_nomad.CreateNomadMod;

@EventBusSubscriber(modid = CreateNomadMod.MODID)
public class BackpackBindingHandler {
    private static final TagKey<Item> BACKPACKS_TAG = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "backpacks"));
    private static final String BINDING_TAG = "gearboundBackpackBinding";

    private BackpackBindingHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBackpack(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown())
            return;

        if (event.getLevel().isClientSide())
            return;

        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (!(blockEntity instanceof net.minecraft.world.Container))
            return;

        ItemStack asItemStack = new ItemStack(blockEntity.getBlockState().getBlock().asItem());
        if (!asItemStack.is(BACKPACKS_TAG))
            return;

        var bindingTag = event.getEntity().getPersistentData().getCompound(BINDING_TAG);
        bindingTag.putString("dimension", event.getLevel().dimension().location().toString());
        bindingTag.putInt("x", event.getPos().getX());
        bindingTag.putInt("y", event.getPos().getY());
        bindingTag.putInt("z", event.getPos().getZ());
        event.getEntity().getPersistentData().put(BINDING_TAG, bindingTag);

        event.getEntity().displayClientMessage(
                net.minecraft.network.chat.Component.literal("Bound to backpack at " + event.getPos().toShortString()),
                true
        );
        event.setCanceled(true);
    }
}
