package net.create_nomad.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.Level;

import net.minecraft.client.gui.screens.Screen;

import net.create_nomad.item.tooltip.TrackItemTooltip;

import java.util.List;
import java.util.Optional;

public class TrackPackItem extends Item {
    private static final String TRACK_ID_TAG = "track_id";
    private static final String TRACK_COUNT_TAG = "track_count";
    private static final int MAX_TRACKS = 1024;

    public TrackPackItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            int inserted = insertTracksFromInventory(pack, player);
            if (inserted > 0) {
                playInsertSound(level, player);
                return InteractionResultHolder.sidedSuccess(pack, level.isClientSide());
            }
            return InteractionResultHolder.pass(pack);
        }

        ItemStack extracted = extractTrackStack(pack);
        if (!extracted.isEmpty()) {
            if (!player.getInventory().add(extracted)) {
                player.drop(extracted, false);
            }
            playRemoveSound(level, player);
            return InteractionResultHolder.sidedSuccess(pack, level.isClientSide());
        }

        return InteractionResultHolder.pass(pack);
    }


    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        ItemStack storedTrack = getStoredTrackStack(stack);
        if (storedTrack.isEmpty()) {
            return Optional.empty();
        }
        int storedCount = getStoredCount(stack);
        return Optional.of(new TrackItemTooltip(storedTrack, storedCount));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int count = getStoredCount(stack);
        if (count <= 0) {
            tooltip.add(Component.translatable("tooltip.create_nomad.track_pack.empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        String trackId = getStoredTrackId(stack);
        tooltip.add(Component.translatable("tooltip.create_nomad.track_pack.contents", count, MAX_TRACKS).withStyle(ChatFormatting.YELLOW));
        if (!trackId.isEmpty()) {
            tooltip.add(Component.literal(trackId).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.create_nomad.track_pack.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private int insertTracksFromInventory(ItemStack pack, Player player) {
        int count = getStoredCount(pack);
        String storedTrackId = getStoredTrackId(pack);
        int inserted = 0;

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);

            if (candidate.isEmpty() || candidate == pack || !isCreateTrack(candidate)) {
                continue;
            }

            String candidateId = getItemId(candidate);
            if (!storedTrackId.isEmpty() && !storedTrackId.equals(candidateId)) {
                continue;
            }

            int canTake = Math.min(candidate.getCount(), MAX_TRACKS - count);
            if (canTake <= 0) {
                break;
            }

            if (storedTrackId.isEmpty()) {
                storedTrackId = candidateId;
            }

            candidate.shrink(canTake);
            count += canTake;
            inserted += canTake;

            if (count >= MAX_TRACKS) {
                break;
            }
        }

        setStoredTrack(pack, storedTrackId, count);
        return inserted;
    }

    private ItemStack extractTrackStack(ItemStack pack) {
        int count = getStoredCount(pack);
        if (count <= 0) {
            return ItemStack.EMPTY;
        }

        String storedTrackId = getStoredTrackId(pack);
        ResourceLocation id = ResourceLocation.tryParse(storedTrackId);
        if (id == null) {
            setStoredTrack(pack, "", 0);
            return ItemStack.EMPTY;
        }

        Item trackItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        if (trackItem == null || trackItem == net.minecraft.world.item.Items.AIR) {
            setStoredTrack(pack, "", 0);
            return ItemStack.EMPTY;
        }

        int extractAmount = Math.min(count, trackItem.getDefaultMaxStackSize());
        ItemStack extracted = new ItemStack(trackItem, extractAmount);

        count -= extractAmount;
        if (count <= 0) {
            setStoredTrack(pack, "", 0);
        } else {
            setStoredTrack(pack, storedTrackId, count);
        }

        return extracted;
    }

    public static boolean isCreateTrack(ItemStack stack) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && "create".equals(id.getNamespace()) && id.getPath().contains("track");
    }

    public static int transferMatchingTracksToStack(ItemStack pack, ItemStack targetStack, int maxAmount) {
        if (maxAmount <= 0 || targetStack.isEmpty() || !isCreateTrack(targetStack)) {
            return 0;
        }

        int storedCount = getStoredCount(pack);
        if (storedCount <= 0) {
            return 0;
        }

        String storedTrackId = getStoredTrackId(pack);
        if (storedTrackId.isEmpty() || !storedTrackId.equals(getItemId(targetStack))) {
            return 0;
        }

        int amount = Math.min(maxAmount, storedCount);
        if (amount <= 0) {
            return 0;
        }

        targetStack.grow(amount);
        storedCount -= amount;

        if (storedCount <= 0) {
            setStoredTrack(pack, "", 0);
        } else {
            setStoredTrack(pack, storedTrackId, storedCount);
        }

        return amount;
    }


    private static ItemStack getStoredTrackStack(ItemStack pack) {
        String storedTrackId = getStoredTrackId(pack);
        ResourceLocation id = ResourceLocation.tryParse(storedTrackId);
        if (id == null) {
            return ItemStack.EMPTY;
        }

        Item trackItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        if (trackItem == null || trackItem == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(trackItem);
    }

    private static String getItemId(ItemStack stack) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private static int getStoredCount(ItemStack stack) {
        CompoundTag tag = getDataTag(stack);
        return Math.max(0, tag.getInt(TRACK_COUNT_TAG));
    }

    private static String getStoredTrackId(ItemStack stack) {
        CompoundTag tag = getDataTag(stack);
        return tag.getString(TRACK_ID_TAG);
    }

    private static void setStoredTrack(ItemStack stack, String trackId, int count) {
        CompoundTag tag = getDataTag(stack);

        if (count <= 0 || trackId.isEmpty()) {
            tag.remove(TRACK_ID_TAG);
            tag.remove(TRACK_COUNT_TAG);
        } else {
            tag.putString(TRACK_ID_TAG, trackId);
            tag.putInt(TRACK_COUNT_TAG, Math.min(count, MAX_TRACKS));
        }

        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    private static CompoundTag getDataTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }

    private static void playInsertSound(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void playRemoveSound(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.8F, 1.0F);
    }
}