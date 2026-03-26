/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.create_nomad.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import net.create_nomad.item.YellowBrassBackpackItemItem;
import net.create_nomad.item.WhiteBrassBackpackItemItem;
import net.create_nomad.item.TrackPackItem;
import net.create_nomad.item.RedBrassBackpackItemItem;
import net.create_nomad.item.PurpleBrassBackpackItemItem;
import net.create_nomad.item.PinkBrassBackpackItemItem;
import net.create_nomad.item.OrangeBrassBackpackItemItem;
import net.create_nomad.item.MagnetUpgradeItem;
import net.create_nomad.item.MagentaBrassBackpackItemItem;
import net.create_nomad.item.LimeBrassBackpackItemItem;
import net.create_nomad.item.LightGrayBrassBackpackItemItem;
import net.create_nomad.item.LightBlueBrassBackpackItemItem;
import net.create_nomad.item.JackhammerItem;
import net.create_nomad.item.HarpoonItemItem;
import net.create_nomad.item.HarpoonGunItem;
import net.create_nomad.item.GreenBrassBackpackItemItem;
import net.create_nomad.item.GrayBrassBackpackItemItem;
import net.create_nomad.item.GrapplingHookItem;
import net.create_nomad.item.CyanBrassBackpackItemItem;
import net.create_nomad.item.ChainsawItem;
import net.create_nomad.item.BrownBrassBackpackItemItem;
import net.create_nomad.item.BlueBrassBackpackItemItem;
import net.create_nomad.item.BlackBrassBackpackItemItem;
import net.create_nomad.item.AndesiteUpgradeItem;
import net.create_nomad.block.display.FilingCabinetDisplayItem;
import net.create_nomad.CreateNomadMod;

public class CreateNomadModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(CreateNomadMod.MODID);
	public static final DeferredItem<Item> BROWN_BRASS_BACKPACK = block(CreateNomadModBlocks.BROWN_BRASS_BACKPACK);
	public static final DeferredItem<Item> BROWN_BRASS_BACKPACK_ITEM = REGISTRY.register("brown_brass_backpack_item", BrownBrassBackpackItemItem::new);
	public static final DeferredItem<Item> GRAPPLING_HOOK = REGISTRY.register("grappling_hook", GrapplingHookItem::new);
	public static final DeferredItem<Item> TRACK_PACK = REGISTRY.register("track_pack", TrackPackItem::new);
	public static final DeferredItem<Item> CHAINSAW = REGISTRY.register("chainsaw", ChainsawItem::new);
	public static final DeferredItem<Item> JACKHAMMER = REGISTRY.register("jackhammer", JackhammerItem::new);
	public static final DeferredItem<Item> BLACK_BRASS_BACKPACK_ITEM = REGISTRY.register("black_brass_backpack_item", BlackBrassBackpackItemItem::new);
	public static final DeferredItem<Item> BLUE_BRASS_BACKPACK_ITEM = REGISTRY.register("blue_brass_backpack_item", BlueBrassBackpackItemItem::new);
	public static final DeferredItem<Item> CYAN_BRASS_BACKPACK_ITEM = REGISTRY.register("cyan_brass_backpack_item", CyanBrassBackpackItemItem::new);
	public static final DeferredItem<Item> GRAY_BRASS_BACKPACK_ITEM = REGISTRY.register("gray_brass_backpack_item", GrayBrassBackpackItemItem::new);
	public static final DeferredItem<Item> GREEN_BRASS_BACKPACK_ITEM = REGISTRY.register("green_brass_backpack_item", GreenBrassBackpackItemItem::new);
	public static final DeferredItem<Item> LIGHT_BLUE_BRASS_BACKPACK_ITEM = REGISTRY.register("light_blue_brass_backpack_item", LightBlueBrassBackpackItemItem::new);
	public static final DeferredItem<Item> LIGHT_GRAY_BRASS_BACKPACK_ITEM = REGISTRY.register("light_gray_brass_backpack_item", LightGrayBrassBackpackItemItem::new);
	public static final DeferredItem<Item> LIME_BRASS_BACKPACK_ITEM = REGISTRY.register("lime_brass_backpack_item", LimeBrassBackpackItemItem::new);
	public static final DeferredItem<Item> MAGENTA_BRASS_BACKPACK_ITEM = REGISTRY.register("magenta_brass_backpack_item", MagentaBrassBackpackItemItem::new);
	public static final DeferredItem<Item> ORANGE_BRASS_BACKPACK_ITEM = REGISTRY.register("orange_brass_backpack_item", OrangeBrassBackpackItemItem::new);
	public static final DeferredItem<Item> PINK_BRASS_BACKPACK_ITEM = REGISTRY.register("pink_brass_backpack_item", PinkBrassBackpackItemItem::new);
	public static final DeferredItem<Item> PURPLE_BRASS_BACKPACK_ITEM = REGISTRY.register("purple_brass_backpack_item", PurpleBrassBackpackItemItem::new);
	public static final DeferredItem<Item> RED_BRASS_BACKPACK_ITEM = REGISTRY.register("red_brass_backpack_item", RedBrassBackpackItemItem::new);
	public static final DeferredItem<Item> WHITE_BRASS_BACKPACK_ITEM = REGISTRY.register("white_brass_backpack_item", WhiteBrassBackpackItemItem::new);
	public static final DeferredItem<Item> YELLOW_BRASS_BACKPACK_ITEM = REGISTRY.register("yellow_brass_backpack_item", YellowBrassBackpackItemItem::new);
	public static final DeferredItem<Item> HARPOON_GUN = REGISTRY.register("harpoon_gun", HarpoonGunItem::new);
	public static final DeferredItem<Item> HARPOON_ITEM = REGISTRY.register("harpoon_item", HarpoonItemItem::new);
	public static final DeferredItem<Item> FILING_CABINET = REGISTRY.register(CreateNomadModBlocks.FILING_CABINET.getId().getPath(), () -> new FilingCabinetDisplayItem(CreateNomadModBlocks.FILING_CABINET.get(), new Item.Properties()));
	public static final DeferredItem<Item> ANDESITE_UPGRADE = REGISTRY.register("andesite_upgrade", AndesiteUpgradeItem::new);
	public static final DeferredItem<Item> MAGNET_UPGRADE = REGISTRY.register("magnet_upgrade", MagnetUpgradeItem::new);

	// Start of user code block custom items
	// End of user code block custom items
	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
	}
}