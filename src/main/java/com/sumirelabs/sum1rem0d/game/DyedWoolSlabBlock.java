package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if <26.3 {
import com.mojang.serialization.MapCodec;
//?}

/** Vanilla slab state/geometry with a saved dyed-color block entity. */
public final class DyedWoolSlabBlock extends SlabBlock implements EntityBlock {
    //? if <26.3 {
    private static final MapCodec<DyedWoolSlabBlock> CODEC = simpleCodec(DyedWoolSlabBlock::new);
    @Override public MapCodec<DyedWoolSlabBlock> codec() { return CODEC; }
    //?}
    public DyedWoolSlabBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TextileEntity("mixed_wool_slab", pos, state); }
    @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean data) {
        return level.getBlockEntity(pos) instanceof DyedBlockEntity entity ? entity.item() : DyeTextiles.stack("mixed_wool_slab", 0xFFFFFF);
    }
}
