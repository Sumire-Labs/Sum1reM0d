package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Only outward-hinged, matching doors form a pair; updates never chain through a row. */
public final class DoubleDoors {
    private static final ThreadLocal<Boolean> SYNCING = ThreadLocal.withInitial(() -> false);
    private DoubleDoors() {}

    public static BlockPos partner(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof DoorBlock)) return null;
        BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        var facing = state.getValue(DoorBlock.FACING);
        var hinge = state.getValue(DoorBlock.HINGE);
        var next = lower.relative(hinge == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
        if (!level.hasChunkAt(next) || !level.hasChunkAt(next.above())) return null;
        var other = level.getBlockState(next); var top = level.getBlockState(next.above());
        return other.is(state.getBlock()) && other.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
            && other.getValue(DoorBlock.FACING) == facing && other.getValue(DoorBlock.HINGE) != hinge
            && top.is(state.getBlock()) && top.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? next : null;
    }

    public static void changed(Level level, BlockPos pos, BlockState before, BlockState after) {
        if (level.isClientSide() || SYNCING.get() || !Rules.on("double_doors") || !(after.getBlock() instanceof DoorBlock)
            || !before.is(after.getBlock()) || before.getValue(DoorBlock.OPEN).equals(after.getValue(DoorBlock.OPEN))) return;
        var target = partner(level, pos, after);
        if (target == null) return;
        SYNCING.set(true);
        try {
            var state = level.getBlockState(target);
            ((DoorBlock) state.getBlock()).setOpen(null, level, state, target, after.getValue(DoorBlock.OPEN));
        } finally { SYNCING.set(false); }
    }

    public static boolean partnerPowered(Level level, BlockPos pos) {
        if (!Rules.on("double_doors")) return false;
        var target = partner(level, pos, level.getBlockState(pos));
        return target != null && (level.hasNeighborSignal(target) || level.hasNeighborSignal(target.above()));
    }
}
