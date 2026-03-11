package net.create_nomad.init;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;

import net.create_nomad.GearboundMod;
import net.create_nomad.block.BlackBrassBackpackBlock;
import net.create_nomad.block.BlueBrassBackpackBlock;
import net.create_nomad.block.CyanBrassBackpackBlock;
import net.create_nomad.block.GrayBrassBackpackBlock;
import net.create_nomad.block.GreenBrassBackpackBlock;
import net.create_nomad.block.LightBlueBrassBackpackBlock;
import net.create_nomad.block.LightGrayBrassBackpackBlock;
import net.create_nomad.block.LimeBrassBackpackBlock;
import net.create_nomad.block.MagentaBrassBackpackBlock;
import net.create_nomad.block.OrangeBrassBackpackBlock;
import net.create_nomad.block.PinkBrassBackpackBlock;
import net.create_nomad.block.PurpleBrassBackpackBlock;
import net.create_nomad.block.RedBrassBackpackBlock;
import net.create_nomad.block.WhiteBrassBackpackBlock;
import net.create_nomad.block.YellowBrassBackpackBlock;
import net.create_nomad.block.entity.BlackBrassBackpackBlockEntity;
import net.create_nomad.block.entity.BlueBrassBackpackBlockEntity;
import net.create_nomad.block.entity.CyanBrassBackpackBlockEntity;
import net.create_nomad.block.entity.GrayBrassBackpackBlockEntity;
import net.create_nomad.block.entity.GreenBrassBackpackBlockEntity;
import net.create_nomad.block.entity.LightBlueBrassBackpackBlockEntity;
import net.create_nomad.block.entity.LightGrayBrassBackpackBlockEntity;
import net.create_nomad.block.entity.LimeBrassBackpackBlockEntity;
import net.create_nomad.block.entity.MagentaBrassBackpackBlockEntity;
import net.create_nomad.block.entity.OrangeBrassBackpackBlockEntity;
import net.create_nomad.block.entity.PinkBrassBackpackBlockEntity;
import net.create_nomad.block.entity.PurpleBrassBackpackBlockEntity;
import net.create_nomad.block.entity.RedBrassBackpackBlockEntity;
import net.create_nomad.block.entity.WhiteBrassBackpackBlockEntity;
import net.create_nomad.block.entity.YellowBrassBackpackBlockEntity;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class GearboundModDyedBackpackHooks {
	private static boolean deferredRegistersBound = false;

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GearboundMod.MODID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, GearboundMod.MODID);

	public static final DeferredBlock<Block> BLACK_BRASS_BACKPACK = BLOCKS.register("black_brass_backpack", BlackBrassBackpackBlock::new);
	public static final DeferredBlock<Block> BLUE_BRASS_BACKPACK = BLOCKS.register("blue_brass_backpack", BlueBrassBackpackBlock::new);
	public static final DeferredBlock<Block> CYAN_BRASS_BACKPACK = BLOCKS.register("cyan_brass_backpack", CyanBrassBackpackBlock::new);
	public static final DeferredBlock<Block> GRAY_BRASS_BACKPACK = BLOCKS.register("gray_brass_backpack", GrayBrassBackpackBlock::new);
	public static final DeferredBlock<Block> GREEN_BRASS_BACKPACK = BLOCKS.register("green_brass_backpack", GreenBrassBackpackBlock::new);
	public static final DeferredBlock<Block> LIGHT_BLUE_BRASS_BACKPACK = BLOCKS.register("light_blue_brass_backpack", LightBlueBrassBackpackBlock::new);
	public static final DeferredBlock<Block> LIGHT_GRAY_BRASS_BACKPACK = BLOCKS.register("light_gray_brass_backpack", LightGrayBrassBackpackBlock::new);
	public static final DeferredBlock<Block> LIME_BRASS_BACKPACK = BLOCKS.register("lime_brass_backpack", LimeBrassBackpackBlock::new);
	public static final DeferredBlock<Block> MAGENTA_BRASS_BACKPACK = BLOCKS.register("magenta_brass_backpack", MagentaBrassBackpackBlock::new);
	public static final DeferredBlock<Block> ORANGE_BRASS_BACKPACK = BLOCKS.register("orange_brass_backpack", OrangeBrassBackpackBlock::new);
	public static final DeferredBlock<Block> PINK_BRASS_BACKPACK = BLOCKS.register("pink_brass_backpack", PinkBrassBackpackBlock::new);
	public static final DeferredBlock<Block> PURPLE_BRASS_BACKPACK = BLOCKS.register("purple_brass_backpack", PurpleBrassBackpackBlock::new);
	public static final DeferredBlock<Block> RED_BRASS_BACKPACK = BLOCKS.register("red_brass_backpack", RedBrassBackpackBlock::new);
	public static final DeferredBlock<Block> WHITE_BRASS_BACKPACK = BLOCKS.register("white_brass_backpack", WhiteBrassBackpackBlock::new);
	public static final DeferredBlock<Block> YELLOW_BRASS_BACKPACK = BLOCKS.register("yellow_brass_backpack", YellowBrassBackpackBlock::new);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> BLACK_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("black_brass_backpack", BLACK_BRASS_BACKPACK, BlackBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> BLUE_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("blue_brass_backpack", BLUE_BRASS_BACKPACK, BlueBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> CYAN_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("cyan_brass_backpack", CYAN_BRASS_BACKPACK, CyanBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> GRAY_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("gray_brass_backpack", GRAY_BRASS_BACKPACK, GrayBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> GREEN_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("green_brass_backpack", GREEN_BRASS_BACKPACK, GreenBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> LIGHT_BLUE_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("light_blue_brass_backpack", LIGHT_BLUE_BRASS_BACKPACK, LightBlueBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> LIGHT_GRAY_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("light_gray_brass_backpack", LIGHT_GRAY_BRASS_BACKPACK, LightGrayBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> LIME_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("lime_brass_backpack", LIME_BRASS_BACKPACK, LimeBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> MAGENTA_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("magenta_brass_backpack", MAGENTA_BRASS_BACKPACK, MagentaBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ORANGE_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("orange_brass_backpack", ORANGE_BRASS_BACKPACK, OrangeBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> PINK_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("pink_brass_backpack", PINK_BRASS_BACKPACK, PinkBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> PURPLE_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("purple_brass_backpack", PURPLE_BRASS_BACKPACK, PurpleBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> RED_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("red_brass_backpack", RED_BRASS_BACKPACK, RedBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> WHITE_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("white_brass_backpack", WHITE_BRASS_BACKPACK, WhiteBrassBackpackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> YELLOW_BRASS_BACKPACK_BLOCK_ENTITY = blockEntity("yellow_brass_backpack", YELLOW_BRASS_BACKPACK, YellowBrassBackpackBlockEntity::new);


	private GearboundModDyedBackpackHooks() {
	}

	public static void register(IEventBus modEventBus) {
		if (deferredRegistersBound)
			return;

		BLOCKS.register(modEventBus);
		BLOCK_ENTITIES.register(modEventBus);
		deferredRegistersBound = true;
	}

	private static DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> blockEntity(String name, DeferredHolder<Block, Block> block,
			BlockEntityType.BlockEntitySupplier<?> supplier) {
		return BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}

	private static void registerCapability(RegisterCapabilitiesEvent event, String blockEntityId) {
		BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath(GearboundMod.MODID, blockEntityId))
				.ifPresent(type -> event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side)));
	}


	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		registerCapability(event, "black_brass_backpack");
		registerCapability(event, "blue_brass_backpack");
		registerCapability(event, "cyan_brass_backpack");
		registerCapability(event, "gray_brass_backpack");
		registerCapability(event, "green_brass_backpack");
		registerCapability(event, "light_blue_brass_backpack");
		registerCapability(event, "light_gray_brass_backpack");
		registerCapability(event, "lime_brass_backpack");
		registerCapability(event, "magenta_brass_backpack");
		registerCapability(event, "orange_brass_backpack");
		registerCapability(event, "pink_brass_backpack");
		registerCapability(event, "purple_brass_backpack");
		registerCapability(event, "red_brass_backpack");
		registerCapability(event, "white_brass_backpack");
		registerCapability(event, "yellow_brass_backpack");
	}
}