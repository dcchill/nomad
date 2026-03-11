package net.create_nomad.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;

import net.create_nomad.init.CreateNomadModBlocks;
import net.create_nomad.world.inventory.BrassBackpackGUIMenu;

import io.netty.buffer.Unpooled;
import java.util.function.Supplier;

public class BrownBrassBackpackItemItem extends BlockItem {
	private final Component displayName;

	public BrownBrassBackpackItemItem() {
		this(CreateNomadModBlocks.BROWN_BRASS_BACKPACK, "Brown Brass Backpack");
	}

	public BrownBrassBackpackItemItem(Supplier<? extends Block> blockSupplier, String displayName) {
		super(blockSupplier.get(), new Properties().stacksTo(1));
		this.displayName = Component.literal(displayName);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.isShiftKeyDown())
			return InteractionResultHolder.pass(stack);

		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			serverPlayer.openMenu(new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return displayName;
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player menuPlayer) {
					FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
					buf.writeBlockPos(menuPlayer.blockPosition());
					buf.writeByte(hand == InteractionHand.MAIN_HAND ? 0 : 1);
					return new BrassBackpackGUIMenu(id, inventory, buf);
				}
			});
		}

		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);

		if (Screen.hasShiftDown()) {
			tooltip.add(Component.translatable("tooltip.gearbound.backpack.description_1").withStyle(ChatFormatting.WHITE));
			tooltip.add(Component.translatable("tooltip.gearbound.backpack.description_2").withStyle(ChatFormatting.GOLD));
		} else {
			tooltip.add(Component.translatable("tooltip.gearbound.shift_for_info", Component.translatable("key.keyboard.left.shift").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

@Override
public InteractionResult useOn(UseOnContext context) {
	if (context.getPlayer() == null)
		return super.useOn(context);

	if (!context.getPlayer().isShiftKeyDown())
		return InteractionResult.PASS;

	return super.useOn(context);
}
}