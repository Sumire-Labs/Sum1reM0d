package com.sumirelabs.sum1rem0d.game;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Concrete stair shape with the same persistent RGB component used by other dyed blocks. */
public final class DyedConcreteStairsBlock extends StairBlock implements EntityBlock {
    //? if <26.3 {
    private static final MapCodec<DyedConcreteStairsBlock> CODEC = simpleCodec(DyedConcreteStairsBlock::new);
    @Override public MapCodec<DyedConcreteStairsBlock> codec() { return CODEC; }
    //?}
    public DyedConcreteStairsBlock(Properties properties) {
        super(BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("white_concrete")).defaultBlockState(), properties);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TextileEntity("mixed_concrete_stairs", pos, state); }
    @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean data) {
        return level.getBlockEntity(pos) instanceof DyedBlockEntity entity ? entity.item() : DyeTextiles.stack("mixed_concrete_stairs", 0xFFFFFF);
    }
}
