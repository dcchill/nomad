package net.create_nomad.item;

import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.create_nomad.procedures.GrapplingHookItemInHandTickProcedure;
import net.create_nomad.procedures.GrapplingHookRightclickedProcedure;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class GrapplingHookItem extends Item {
	private static final int BACKTANK_AIR_COST_PER_USE = 1;

	public GrapplingHookItem() {
		super(new Item.Properties().stacksTo(1).durability(512));
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(itemstack, level, tooltip, flag);

		if (Screen.hasShiftDown()) {
			tooltip.add(Component.translatable("tooltip.create_nomad.grappling_hook.description_1").withStyle(ChatFormatting.WHITE));
			tooltip.add(Component.translatable("tooltip.create_nomad.grappling_hook.description_2").withStyle(ChatFormatting.GOLD));
		} else {
			tooltip.add(Component.translatable("tooltip.create_nomad.shift_for_info",
					Component.translatable("key.keyboard.left.shift").withStyle(ChatFormatting.YELLOW))
					.withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack stack = entity.getItemInHand(hand);
		boolean grappleStarted = GrapplingHookRightclickedProcedure.execute(entity, stack);
		if (grappleStarted && !world.isClientSide && !tryConsumeBacktankAir(entity, BACKTANK_AIR_COST_PER_USE)) {
			stack.hurtAndBreak(1, entity, LivingEntity.getSlotForHand(hand));
		}
		return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		GrapplingHookItemInHandTickProcedure.execute(entity);
	}

	private static boolean tryConsumeBacktankAir(Player player, int airCost) {
		if (airCost <= 0 || player.isCreative()) {
			return true;
		}

		List<ItemStack> backtanksWithAir = BacktankUtil.getAllWithAir(player);
		if (backtanksWithAir.isEmpty()) {
			return false;
		}

		ItemStack backtank = backtanksWithAir.get(0);
		if (BacktankUtil.getAir(backtank) < airCost) {
			return false;
		}

		BacktankUtil.consumeAir(player, backtank, airCost);
		return true;
	}
}
