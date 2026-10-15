package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

public final class FletchingMenu extends AbstractContainerMenu {
    public static final MenuType<FletchingMenu> TYPE = new MenuType<>(FletchingMenu::new, FeatureFlags.DEFAULT_FLAGS);
    private final SimpleContainer input = new SimpleContainer(1), output = new SimpleContainer(27);
    private final ContainerLevelAccess access;
    private int pending;
    private Item pendingItem = Items.AIR;
    public FletchingMenu(int id, Inventory inventory) { this(id, inventory, ContainerLevelAccess.NULL); }
    public FletchingMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(TYPE, id); this.access = access;
        addSlot(new Slot(input, 0, 80, 18) {
            @Override public boolean mayPlace(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(Rules.s("fletching", "input_item")); }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(output, row * 9 + column, 8 + column * 18, 50 + row * 18) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public void onTake(Player player, ItemStack stack) { super.onTake(player, stack); fill(); }
        });
        addStandardInventorySlots(inventory, 8, 120);
    }
    public boolean canCraft() { return Rules.on("fletching") && pending == 0 && output.isEmpty() && input.getItem(0).getCount() >= Rules.i("fletching", "input_count") && BuiltInRegistries.ITEM.getKey(input.getItem(0).getItem()).toString().equals(Rules.s("fletching", "input_item")); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || !canCraft()) return false;
        var id = Rules.id(Rules.s("fletching", "output_item")); if (id == null) return false;
        pendingItem = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR); if (pendingItem == Items.AIR) return false;
        input.getItem(0).shrink(Rules.i("fletching", "input_count")); pending = Rules.i("fletching", "output_count"); fill(); broadcastChanges(); return true;
    }
    private void fill() {
        for (int slot = 0; slot < output.getContainerSize() && pending > 0; slot++) if (output.getItem(slot).isEmpty()) {
            int count = Math.min(pending, pendingItem.getDefaultInstance().getMaxStackSize()); output.setItem(slot, new ItemStack(pendingItem, count)); pending -= count;
        }
    }
    @Override public boolean stillValid(Player player) { return Rules.on("fletching") && stillValid(access, player, Blocks.FLETCHING_TABLE); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 28) { if (!moveItemStackTo(stack, 28, slots.size(), true)) return ItemStack.EMPTY; }
        else if (slots.getFirst().mayPlace(stack)) { if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY; }
        else if (index < 55) { if (!moveItemStackTo(stack, 55, slots.size(), false)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(stack, 28, 55, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, original); fill(); return original;
    }
    @Override public void removed(Player player) {
        super.removed(player); if (player.level().isClientSide()) return;
        clearContainer(player, input); clearContainer(player, output);
        while (pending > 0) {
            int count = Math.min(pending, pendingItem.getDefaultInstance().getMaxStackSize()); var stack = new ItemStack(pendingItem, count);
            if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY); pending -= count;
        }
    }
}
