package net.create_nomad.compat.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.create_nomad.CreateNomadMod;
import net.create_nomad.init.CreateNomadModItems;
import net.minecraft.resources.ResourceLocation;

public class CreateNomadPonderPlugin implements PonderPlugin {
	@Override
	public String getModId() {
		return CreateNomadMod.MODID;
	}

		@Override
		public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
			helper.forComponents(
				CreateNomadModItems.BROWN_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.BLACK_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.BLUE_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.CYAN_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.GRAY_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.GREEN_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.LIGHT_BLUE_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.LIGHT_GRAY_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.LIME_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.MAGENTA_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.ORANGE_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.PINK_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.PURPLE_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.RED_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.WHITE_BRASS_BACKPACK_ITEM.getId(),
				CreateNomadModItems.YELLOW_BRASS_BACKPACK_ITEM.getId()
			)
			.addStoryBoard("backpack_auto_hotbar", CreateNomadPonderScenes::backpackAutoHotbar)
			.addStoryBoard("backpack_auto_hotbar_pg2", CreateNomadPonderScenes::backpackColorVariants);
		}
}
