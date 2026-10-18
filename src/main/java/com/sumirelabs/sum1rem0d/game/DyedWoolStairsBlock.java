package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if <26.3 {
import com.mojang.serialization.MapCodec;
//?}

/** Vanilla stair state/shape with a saved dyed-color block entity. */
public final class DyedWoolStairsBlock extends StairBlock implements EntityBlock {
    //? if <26.3 {
    private static final MapCodec<DyedWoolStairsBlock> CODEC = simpleCodec(DyedWoolStairsBlock::new);
    @Override public MapCodec<DyedWoolStairsBlock> codec() { return CODEC; }
    //?}
    public DyedWoolStairsBlock(Properties properties) { super(net.minecraft.world.level.block.Blocks.OAK_STAIRS.defaultBlockState(), properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TextileEntity("mixed_wool_stairs", pos, state); }
    @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean data) {
        return level.getBlockEntity(pos) instanceof DyedBlockEntity entity ? entity.item() : DyeTextiles.stack("mixed_wool_stairs", 0xFFFFFF);
    }
}
