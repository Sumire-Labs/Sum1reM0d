package com.sumirelabs.sum1rem0d.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Stores full ItemStacks, including enchantments and custom components, in chunk data. */
public final class DeathBoxEntity extends BlockEntity {
    private final List<ItemStack> contents = new ArrayList<>();
    public DeathBoxEntity(BlockPos pos, BlockState state) { super(DeathBoxContent.type(), pos, state); }
    public void fill(List<ItemStack> items) { contents.clear(); for (var item : items) if (!item.isEmpty()) contents.add(item.copy()); setChanged(); }
    public List<ItemStack> takeContents() { var result = List.copyOf(contents); contents.clear(); setChanged(); return result; }
    public List<ItemStack> contents() { return contents.stream().map(ItemStack::copy).toList(); }
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.store("contents", ItemStack.CODEC.listOf(), contents); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); contents.clear(); contents.addAll(in.read("contents", ItemStack.CODEC.listOf()).orElse(List.of())); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) for (var stack : takeContents())
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack);
    }
}
