package net.create_nomad.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

import net.create_nomad.util.BackpackDataUtils;

import net.create_nomad.block.entity.GrayBrassBackpackBlockEntity;

public class GrayBrassBackpackBlock extends Block implements EntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	private static final VoxelShape SHAPE_NORTH = Shapes.or(box(3, 0, 5, 13, 6, 11), box(4, 6, 6, 12, 12, 11), box(2, 1, 6, 14, 4, 10), box(4, 0, 4, 12, 4, 5));
	private static final VoxelShape SHAPE_EAST = Shapes.or(box(5, 0, 3, 11, 6, 13), box(5, 6, 4, 10, 12, 12), box(6, 1, 2, 10, 4, 14), box(11, 0, 4, 12, 4, 12));
	private static final VoxelShape SHAPE_SOUTH = Shapes.or(box(3, 0, 5, 13, 6, 11), box(4, 6, 5, 12, 12, 10), box(2, 1, 6, 14, 4, 10), box(4, 0, 11, 12, 4, 12));
	private static final VoxelShape SHAPE_WEST = Shapes.or(box(5, 0, 3, 11, 6, 13), box(6, 6, 4, 11, 12, 12), box(6, 1, 2, 10, 4, 14), box(4, 0, 4, 5, 4, 12));

	public GrayBrassBackpackBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.WOOL).strength(0.75f, 8f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	private static Direction resolveFacing(BlockState state) {
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
			return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		return Direction.NORTH;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		Direction facing = resolveFacing(state);
		return switch (facing) {
			case EAST -> SHAPE_WEST;
			case SOUTH -> SHAPE_NORTH;
			case WEST -> SHAPE_EAST;
			default -> SHAPE_SOUTH;
		};
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isShiftKeyDown())
			return InteractionResult.PASS;

		if (!world.isClientSide && player instanceof ServerPlayer serverPlayer)
			serverPlayer.openMenu(state.getMenuProvider(world, pos), pos);

		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GrayBrassBackpackBlockEntity(pos, state);
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
		BlockEntity be = world.getBlockEntity(pos);
		return be instanceof MenuProvider provider ? provider : null;
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			BlockEntity be = world.getBlockEntity(pos);

			if (!world.isClientSide && be instanceof GrayBrassBackpackBlockEntity backpack) {
				ItemStack stack = new ItemStack(resolveBackpackItem(state));
				BackpackDataUtils.saveContainerToItem(backpack, stack, world);
				Containers.dropItemStack(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
			}

			super.onRemove(state, world, pos, newState, isMoving);
		}
	}

	private static Item resolveBackpackItem(BlockState state) {
		ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		ResourceLocation itemId = new ResourceLocation(blockId.getNamespace(), blockId.getPath() + "_item");
		Item item = BuiltInRegistries.ITEM.get(itemId);
		return item == Items.AIR ? state.getBlock().asItem() : item;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (level.isClientSide())
			return;

		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof GrayBrassBackpackBlockEntity backpack)
			BackpackDataUtils.loadContainerFromItem(backpack, stack, level);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
		return java.util.Collections.emptyList();
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof GrayBrassBackpackBlockEntity backpack)
			return AbstractContainerMenu.getRedstoneSignalFromContainer(backpack);
		return 0;
	}
}
