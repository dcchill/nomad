package net.create_nomad.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

import net.create_nomad.CreateNomadMod;

@EventBusSubscriber(modid = CreateNomadMod.MODID)
public class BackpackUnbindHandler {
    private static final TagKey<Item> BACKPACKS_TAG = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "backpacks"));
    private static final String BINDING_TAG = "gearboundBackpackBinding";

    private BackpackUnbindHandler() {
    }

    @SubscribeEvent
    public static void onBackpackBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide())
            return;

        ItemStack asItemStack = new ItemStack(event.getState().getBlock().asItem());
        if (!asItemStack.is(BACKPACKS_TAG))
            return;

        ResourceLocation dimension = level.dimension().location();
        int x = event.getPos().getX();
        int y = event.getPos().getY();
        int z = event.getPos().getZ();

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            var bindingTag = player.getPersistentData().getCompound(BINDING_TAG);
            if (bindingTag.isEmpty())
                continue;

            if (!dimension.toString().equals(bindingTag.getString("dimension")))
                continue;

            if (bindingTag.getInt("x") != x || bindingTag.getInt("y") != y || bindingTag.getInt("z") != z)
                continue;

            player.getPersistentData().remove(BINDING_TAG);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Backpack binding cleared (backpack was broken)."),
                    true
            );
        }
    }
}
