package net.create_nomad.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.create_nomad.world.inventory.BrassBackpackGUIMenu;
import net.create_nomad.util.BackpackDataUtils;
import net.create_nomad.util.BackpackInventoryRules;

import io.netty.buffer.Unpooled;

import java.util.function.Supplier;

public class BrassBackpackItem extends BlockItem {
    private static final Pattern ITEM_ID_PATTERN = Pattern.compile("([a-z0-9_.-]+:[a-z0-9_/.-]+)");

    private final DyeColor color;

    public BrassBackpackItem(Supplier<? extends Block> blockSupplier, DyeColor color) {
        super(blockSupplier.get(), new Properties().stacksTo(1));
        this.color = color;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.create_nomad.backpack.description_1").withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("tooltip.create_nomad.backpack.description_2").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.create_nomad.shift_for_info",
                    Component.translatable("key.keyboard.left.shift").withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

	@Override
	public Component getName(ItemStack stack) {
	    String name = color.getName().replace("_", " ");
	
	    String[] words = name.split(" ");
	    StringBuilder formatted = new StringBuilder();
	
	    for (String word : words) {
	        formatted.append(Character.toUpperCase(word.charAt(0)))
	                 .append(word.substring(1))
	                 .append(" ");
	    }
	
	    return Component.literal(formatted.toString().trim() + " Brass Backpack");
	}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherHandStack = player.getItemInHand(otherHand);

        if (player.isShiftKeyDown())
            return InteractionResultHolder.pass(stack);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel && isClipboard(otherHandStack)) {
            ItemStackHandler backpackInventory = BackpackDataUtils.loadHandlerFromItem(stack, serverLevel, BackpackInventoryRules.TOTAL_SLOT_COUNT);
            Map<ResourceLocation, Integer> requested = parseRequestedItemsFromClipboard(otherHandStack);

            if (requested.isEmpty()) {
                player.displayClientMessage(Component.literal("No item requests found on clipboard."), true);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            int extractedStacks = pullRequestedItemsToPlayerInventory(player.getInventory(), backpackInventory, requested);
            BackpackDataUtils.saveHandlerToItem(backpackInventory, stack, serverLevel);
            player.displayClientMessage(Component.literal("Pulled " + extractedStacks + " stack(s) from backpack."), true);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {

            serverPlayer.openMenu(new MenuProvider() {

                @Override
                public Component getDisplayName() {
                    return getName(stack);
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

    private static boolean isClipboard(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getPath().contains("clipboard");
    }

    private static Map<ResourceLocation, Integer> parseRequestedItemsFromClipboard(ItemStack clipboardStack) {
        Map<ResourceLocation, Integer> requests = new LinkedHashMap<>();
        CustomData customData = clipboardStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return requests;
        }

        scanClipboardTag(customData.copyTag(), requests);
        return requests;
    }

    private static void scanClipboardTag(Tag tag, Map<ResourceLocation, Integer> requests) {
        if (tag instanceof CompoundTag compound) {
            Optional<ResourceLocation> itemId = parseItemId(compound.getString("item"));
            if (itemId.isPresent()) {
                int amount = readAmount(compound);
                requests.merge(itemId.get(), amount, Integer::sum);
            }

            for (String key : compound.getAllKeys()) {
                scanClipboardTag(compound.get(key), requests);
            }
            return;
        }

        if (tag instanceof ListTag listTag) {
            for (Tag child : listTag) {
                scanClipboardTag(child, requests);
            }
            return;
        }

        String text = tag.getAsString();
        Matcher matcher = ITEM_ID_PATTERN.matcher(text);
        while (matcher.find()) {
            parseItemId(matcher.group(1)).ifPresent(id -> requests.merge(id, 1, Integer::sum));
        }
    }

    private static Optional<ResourceLocation> parseItemId(String raw) {
        if (raw == null || raw.isBlank() || !raw.contains(":")) {
            return Optional.empty();
        }

        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return Optional.empty();
        }

        return Optional.of(id);
    }

    private static int readAmount(CompoundTag tag) {
        if (tag.contains("count", Tag.TAG_INT)) {
            return Math.max(1, tag.getInt("count"));
        }
        if (tag.contains("amount", Tag.TAG_INT)) {
            return Math.max(1, tag.getInt("amount"));
        }
        if (tag.contains("required", Tag.TAG_INT)) {
            return Math.max(1, tag.getInt("required"));
        }
        return 1;
    }

    private static int pullRequestedItemsToPlayerInventory(Inventory playerInventory, ItemStackHandler backpackInventory, Map<ResourceLocation, Integer> requests) {
        int movedStacks = 0;
        for (Map.Entry<ResourceLocation, Integer> request : requests.entrySet()) {
            int remaining = request.getValue();
            if (remaining <= 0) {
                continue;
            }

            for (int slot = 0; slot < BackpackInventoryRules.STORAGE_SLOT_COUNT && remaining > 0; slot++) {
                ItemStack stored = backpackInventory.getStackInSlot(slot);
                ResourceLocation storedId = BuiltInRegistries.ITEM.getKey(stored.getItem());
                if (stored.isEmpty() || !request.getKey().equals(storedId)) {
                    continue;
                }

                int moveCount = Math.min(remaining, stored.getCount());
                ItemStack toMove = stored.copyWithCount(moveCount);
                playerInventory.add(toMove);
                int inserted = moveCount - toMove.getCount();
                if (inserted <= 0) {
                    continue;
                }

                stored.shrink(inserted);
                backpackInventory.setStackInSlot(slot, stored);
                remaining -= inserted;
                movedStacks++;
            }
        }

        return movedStacks;
    }
}
