package com.sumirelabs.sum1rem0d.game;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Model-only grass state; retain the vanilla snowy variant without changing world blocks. */
public final class VisualGrassBlock extends Block {
    public VisualGrassBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.SNOWY, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.SNOWY); }
}
