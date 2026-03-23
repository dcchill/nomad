package net.create_nomad.init;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

import net.create_nomad.client.tooltip.TrackItemClientTooltip;
import net.create_nomad.item.tooltip.TrackItemTooltip;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateNomadModTooltipComponents {
    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(TrackItemTooltip.class, TrackItemClientTooltip::new);
    }
}
