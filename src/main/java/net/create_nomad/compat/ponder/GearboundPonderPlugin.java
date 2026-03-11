package net.create_nomad.compat.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.create_nomad.GearboundMod;
import net.create_nomad.init.GearboundModItems;
import net.minecraft.resources.ResourceLocation;

public class GearboundPonderPlugin implements PonderPlugin {
	@Override
	public String getModId() {
		return GearboundMod.MODID;
	}

		@Override
		public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
			helper.forComponents(
				GearboundModItems.BROWN_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.BLACK_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.BLUE_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.CYAN_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.GRAY_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.GREEN_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.LIGHT_BLUE_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.LIGHT_GRAY_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.LIME_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.MAGENTA_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.ORANGE_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.PINK_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.PURPLE_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.RED_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.WHITE_BRASS_BACKPACK_ITEM.getId(),
				GearboundModItems.YELLOW_BRASS_BACKPACK_ITEM.getId()
			)
			.addStoryBoard("backpack_auto_hotbar", GearboundPonderScenes::backpackAutoHotbar)
			.addStoryBoard("backpack_auto_hotbar_pg2", GearboundPonderScenes::backpackColorVariants);
		}
}
