package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class DeathBoxBlock extends Block implements EntityBlock {
    //? if <26.3 {
    private static final com.mojang.serialization.MapCodec<DeathBoxBlock> CODEC = simpleCodec(DeathBoxBlock::new);
    @Override public com.mojang.serialization.MapCodec<DeathBoxBlock> codec() { return CODEC; }
    //?}
    public DeathBoxBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DeathBoxEntity(pos, state); }
    @Override protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return useWithoutItem(state, level, pos, player, hit);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) DeathBoxContent.release(server, pos);
        return InteractionResult.SUCCESS;
    }
}
