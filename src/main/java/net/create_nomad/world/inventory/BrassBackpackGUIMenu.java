package net.create_nomad.world.inventory;

import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

import net.create_nomad.CreateNomadMod;
import net.create_nomad.init.CreateNomadModMenus;
import net.create_nomad.util.BackpackDataUtils;
import net.create_nomad.util.BackpackInventoryRules;
import net.create_nomad.util.BackpackItemAssociations;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class BrassBackpackGUIMenu extends AbstractContainerMenu implements CreateNomadModMenus.MenuAccessor {
    private static final TagKey<Item> BACKPACK_UPGRADES_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "backpack_upgrades"));
    private static final TagKey<Item> CREATE_PACKAGES_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("create", "packages"));
    private static final int[] PACKAGE_INPUT_SLOTS = {0, 1, 2, 3};
    private static final ResourceLocation PACKAGER_UPGRADE_ID = ResourceLocation.fromNamespaceAndPath(CreateNomadMod.MODID, "packager_upgrade");

    public final Level world;
    public final Player entity;

    private ContainerLevelAccess access = ContainerLevelAccess.NULL;
    private IItemHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<>();

    private boolean bound = false;
    private Supplier<Boolean> boundItemMatcher = null;
    private Entity boundEntity = null;
    private BlockEntity boundBlockEntity = null;

    private ItemStack boundStack = ItemStack.EMPTY;

    public BrassBackpackGUIMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(CreateNomadModMenus.BRASS_BACKPACK_GUI.get(), id);

        this.entity = inv.player;
        this.world = inv.player.level();
        this.internal = new ItemStackHandler(BackpackInventoryRules.TOTAL_SLOT_COUNT);

        BlockPos pos = null;

        if (extraData != null) {
            pos = extraData.readBlockPos();
            access = ContainerLevelAccess.create(world, pos);
        }

        // ===============================
        // Binding Logic
        // ===============================

        if (pos != null) {

            // Bound to item
            if (extraData.readableBytes() == 1) {

                byte source = extraData.readByte();
                ItemStack stack;

                if (source == 0 || source == 1) {
                    byte hand = source;
                    stack = hand == 0
                            ? entity.getMainHandItem()
                            : entity.getOffhandItem();

                    this.boundItemMatcher = () ->
                            stack == (hand == 0
                                    ? entity.getMainHandItem()
                                    : entity.getOffhandItem());
                } else {
                    stack = findFirstCuriosBackpack(entity);
                    this.boundItemMatcher = () -> stack == findFirstCuriosBackpack(entity);
                }

                this.boundStack = stack;

                this.internal = new ItemStackHandler(BackpackInventoryRules.TOTAL_SLOT_COUNT);
                this.bound = true;

                loadFromItem(stack);
            }

            // Bound to entity
            else if (extraData.readableBytes() > 1) {

                extraData.readByte();
                boundEntity = world.getEntity(extraData.readVarInt());

                if (boundEntity != null) {
                    IItemHandler cap = boundEntity.getCapability(Capabilities.ItemHandler.ENTITY);
                    if (cap != null) {
                        this.internal = cap;
                        this.bound = true;
                    }
                }
            }

            // Bound to block
            else {

                boundBlockEntity = world.getBlockEntity(pos);

                if (boundBlockEntity instanceof BaseContainerBlockEntity base) {
                    this.internal = new InvWrapper(base);
                    this.bound = true;
                }
            }
        }

        // ===============================
        // Backpack Slots (36)
        // ===============================

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {

                int index = col + row * 9;
                int xPos = 8 + col * 18;
                int yPos = 8 + row * 18;

                this.customSlots.put(index, this.addSlot(
                        new SlotItemHandler(internal, index, xPos, yPos) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return BackpackInventoryRules.canStoreInBackpack(stack);
                            }
                        }
                ));
            }
        }
        // Upgrade Slots (3)
        int[] upgradeSlotY = {17, 35, 53};
        for (int i = 0; i < BackpackInventoryRules.UPGRADE_SLOT_COUNT; i++) {
            int slotIndex = BackpackInventoryRules.UPGRADE_SLOT_START + i;
            int yPos = upgradeSlotY[i];
            this.customSlots.put(slotIndex, this.addSlot(new SlotItemHandler(internal, slotIndex, 179, yPos) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(BACKPACK_UPGRADES_TAG);
                }
            }));
        }

        // ===============================
        // Player Inventory
        // ===============================

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv,
                        col + (row + 1) * 9,
                        8 + col * 18,
                        106 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv,
                    col,
                    8 + col * 18,
                    164));
        }
    }

    // ===============================
    // Persistence
    // ===============================

    private void loadFromItem(ItemStack stack) {

        if (internal instanceof ItemStackHandler handler && world instanceof ServerLevel serverLevel) {
            ItemStackHandler loaded = BackpackDataUtils.loadHandlerFromItem(stack, serverLevel, handler.getSlots());
            for (int i = 0; i < handler.getSlots(); i++) {
                handler.setStackInSlot(i, loaded.getStackInSlot(i));
            }
        }
    }

    private void saveToItem() {

        if (!boundStack.isEmpty() && internal instanceof ItemStackHandler handler && world instanceof ServerLevel serverLevel) {
            BackpackDataUtils.saveHandlerToItem(handler, boundStack, serverLevel);
        }
    }

    // ===============================
    // Validity
    // ===============================

    @Override
    public boolean stillValid(Player player) {

        if (bound) {

            if (boundItemMatcher != null)
                return boundItemMatcher.get();

            else if (boundBlockEntity != null)
                return AbstractContainerMenu.stillValid(
                        access,
                        player,
                        boundBlockEntity.getBlockState().getBlock()
                );

            else if (boundEntity != null)
                return boundEntity.isAlive();
        }

        return true;
    }

    // ===============================
    // Quick Move
    // ===============================

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {

        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {

            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < BackpackInventoryRules.TOTAL_SLOT_COUNT) {
                if (!this.moveItemStackTo(itemstack1, BackpackInventoryRules.TOTAL_SLOT_COUNT, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                if (itemstack1.is(BACKPACK_UPGRADES_TAG)) {
                    if (!this.moveItemStackTo(itemstack1, BackpackInventoryRules.UPGRADE_SLOT_START, BackpackInventoryRules.TOTAL_SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 0, BackpackInventoryRules.STORAGE_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty())
                slot.setByPlayer(ItemStack.EMPTY);
            else
                slot.setChanged();
        }

        return itemstack;
    }

    // ===============================
    // On Close
    // ===============================

    @Override
    public void removed(Player playerIn) {

        super.removed(playerIn);

        if (bound && !boundStack.isEmpty()) {
            saveToItem();
            return;
        }

        if (!bound && playerIn instanceof ServerPlayer serverPlayer) {

            for (int i = 0; i < internal.getSlots(); ++i) {

                ItemStack stack = internal.getStackInSlot(i);

                if (!stack.isEmpty()) {

                    playerIn.getInventory().placeItemBackInInventory(stack);

                    if (internal instanceof IItemHandlerModifiable ihm)
                        ihm.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (boundBlockEntity != null && !world.isClientSide()) {
            boundBlockEntity.setChanged();
            BlockState state = boundBlockEntity.getBlockState();
            world.sendBlockUpdated(boundBlockEntity.getBlockPos(), state, state, 3);
        }
    }

    @Override
    public Map<Integer, Slot> getSlots() {
        return Collections.unmodifiableMap(customSlots);
    }

    @Override
    public Map<String, Object> getMenuState() {
        return Collections.emptyMap();
    }

    public boolean hasPackagerUpgradeInstalled() {
        for (int slot = BackpackInventoryRules.UPGRADE_SLOT_START; slot < BackpackInventoryRules.TOTAL_SLOT_COUNT; slot++) {
            ItemStack upgradeStack = this.getSlot(slot).getItem();
            if (!upgradeStack.isEmpty() && PACKAGER_UPGRADE_ID.equals(BuiltInRegistries.ITEM.getKey(upgradeStack.getItem()))) {
                return true;
            }
        }
        return false;
    }


    public void packageInputSlotsIntoRandomCreatePackage(ServerPlayer player) {
        if (world.isClientSide()) {
            return;
        }

        if (!hasPackagerUpgradeInstalled()) {
            return;
        }

        HolderSet.Named<Item> packageItems = player.registryAccess()
                .lookupOrThrow(Registries.ITEM)
                .get(CREATE_PACKAGES_TAG)
                .orElse(null);

        if (packageItems == null || packageItems.size() == 0) {
            return;
        }

        ItemStack[] contents = new ItemStack[PACKAGE_INPUT_SLOTS.length];
        boolean hasAnyItem = false;

        for (int i = 0; i < PACKAGE_INPUT_SLOTS.length; i++) {
            int slot = PACKAGE_INPUT_SLOTS[i];
            ItemStack stack = internal.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                hasAnyItem = true;
                contents[i] = stack.copy();
            } else {
                contents[i] = ItemStack.EMPTY;
            }
        }

        if (!hasAnyItem) {
            return;
        }

        Item chosenPackage = packageItems.get(world.random.nextInt(packageItems.size())).value();
        ItemStack packagedStack = new ItemStack(chosenPackage);
        DataComponentType<?> packageContentsComponent = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("create", "package_contents"));
        if (packageContentsComponent != null) {
            @SuppressWarnings("unchecked")
            DataComponentType<ItemContainerContents> typedComponent = (DataComponentType<ItemContainerContents>) packageContentsComponent;
            packagedStack.set(typedComponent, ItemContainerContents.fromItems(java.util.List.of(contents)));
        } else {
            return;
        }

        if (!(internal instanceof IItemHandlerModifiable modifiable)) {
            return;
        }

        for (int slot : PACKAGE_INPUT_SLOTS) {
            modifiable.setStackInSlot(slot, ItemStack.EMPTY);
        }

        player.getInventory().placeItemBackInInventory(packagedStack);
        broadcastChanges();

        if (bound && !boundStack.isEmpty()) {
            saveToItem();
        }
    }

    private static ItemStack findFirstCuriosBackpack(Player player) {
        IItemHandler curiosInventory = CreateNomadMod.CuriosApiHelper.getCuriosInventory(player);

        if (curiosInventory == null) {
            return ItemStack.EMPTY;
        }

        for (int i = 0; i < curiosInventory.getSlots(); i++) {
            ItemStack stack = curiosInventory.getStackInSlot(i);
            if (BackpackItemAssociations.isBackpackItem(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}
