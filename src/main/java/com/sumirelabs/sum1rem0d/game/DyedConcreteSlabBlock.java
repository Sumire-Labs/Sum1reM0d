package com.sumirelabs.sum1rem0d.game;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Concrete slab shape with the same persistent RGB component used by other dyed blocks. */
public final class DyedConcreteSlabBlock extends SlabBlock implements EntityBlock {
    //? if <26.3 {
    private static final MapCodec<DyedConcreteSlabBlock> CODEC = simpleCodec(DyedConcreteSlabBlock::new);
    @Override public MapCodec<DyedConcreteSlabBlock> codec() { return CODEC; }
    //?}
    public DyedConcreteSlabBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TextileEntity("mixed_concrete_slab", pos, state); }
    @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean data) {
        return level.getBlockEntity(pos) instanceof DyedBlockEntity entity ? entity.item() : DyeTextiles.stack("mixed_concrete_slab", 0xFFFFFF);
    }
}
