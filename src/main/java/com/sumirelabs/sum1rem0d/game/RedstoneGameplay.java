package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

public final class RedstoneGameplay {
    private static final ThreadLocal<Boolean> SYNTHETIC = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> UPDATING = ThreadLocal.withInitial(() -> false);
    private RedstoneGameplay() {}
    public static int raw(java.util.function.IntSupplier query){boolean previous=SYNTHETIC.get();SYNTHETIC.set(true);try{return query.getAsInt();}finally{SYNTHETIC.set(previous);}}
    public static boolean part(String key) { return Rules.on("redstone_inversion") && Rules.b("redstone_inversion", key); }
    public static int signal(BlockState state, BlockGetter level, BlockPos pos, Direction direction, int original, boolean direct) {
        if(GlowstoneWiring.reading() && state.getBlock() instanceof RedstoneWireBlock)return 0;
        if(state.getBlock() instanceof GlowWireBlock)return original;
        if (SYNTHETIC.get() || !Rules.on("redstone_inversion")) return original;
        Block block = state.getBlock();
        // These components reverse their actual state/connected network instead.
        if (block instanceof RedstoneWireBlock || block instanceof RedstoneTorchBlock || block instanceof RepeaterBlock) return original;
        String part = block instanceof PoweredBlock ? "blocks" : block instanceof LeverBlock ? "levers" : block instanceof ButtonBlock ? "buttons"
            : block instanceof BasePressurePlateBlock ? "plates" : block instanceof ComparatorBlock ? "comparators" : block instanceof ObserverBlock ? "observers" : "other_sources";
        if (!Rules.b("redstone_inversion", part) || !state.isSignalSource()) return original;
        // Some direct-signal implementations delegate to BlockState.getSignal,
        // which has already passed through our weak-signal hook. Read the raw
        // value under the guard so it is inverted exactly once.
        if (direct) {
            SYNTHETIC.set(true);
            try { original = state.getDirectSignal(level, pos, direction); }
            finally { SYNTHETIC.set(false); }
        }
        if (block instanceof DiodeBlock) return direction == state.getValue(BlockStateProperties.HORIZONTAL_FACING) ? (original > 0 ? 0 : 15) : 0;
        BlockState on = state;
        if (state.hasProperty(BlockStateProperties.POWERED)) on = on.setValue(BlockStateProperties.POWERED, true);
        if (state.hasProperty(BlockStateProperties.POWER)) on = on.setValue(BlockStateProperties.POWER, 15);
        int possible;
        SYNTHETIC.set(true);
        try { possible = direct ? on.getDirectSignal(level, pos, direction) : on.getSignal(level, pos, direction); }
        finally { SYNTHETIC.set(false); }
        return possible == 0 ? original : original > 0 ? 0 : 15;
    }
    public static Set<BlockPos> wire(Level level, BlockPos start) {
        if (UPDATING.get() || level.isClientSide()) return Set.of();
        UPDATING.set(true);
        try {
            var wire = (RedstoneWireBlock) Blocks.REDSTONE_WIRE;
            Set<BlockPos> network = new LinkedHashSet<>(); ArrayDeque<BlockPos> queue = new ArrayDeque<>(); queue.add(start.immutable());
            int limit = Rules.i("redstone_inversion", "wire_limit");
            boolean powered = false;
            while (!queue.isEmpty() && network.size() < limit) {
                BlockPos pos = queue.removeFirst();
                if (network.contains(pos) || !level.hasChunkAt(pos) || !level.getBlockState(pos).is(Blocks.REDSTONE_WIRE)) continue;
                network.add(pos);
                if (wire.getBlockSignal(level, pos) > 0) powered = true;
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos neighbor = pos.relative(direction);
                    if (level.getBlockState(neighbor).is(Blocks.REDSTONE_WIRE)) queue.add(neighbor);
                    else {
                        if (!level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above()) && level.getBlockState(neighbor.above()).is(Blocks.REDSTONE_WIRE)) queue.add(neighbor.above());
                        if (!level.getBlockState(neighbor).isRedstoneConductor(level, neighbor) && level.getBlockState(neighbor.below()).is(Blocks.REDSTONE_WIRE)) queue.add(neighbor.below());
                    }
                }
            }
            int power = powered ? 0 : 15;
            List<BlockPos> changed = new ArrayList<>();
            for (BlockPos pos : network) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.REDSTONE_WIRE) && state.getValue(RedstoneWireBlock.POWER) != power) { level.setBlock(pos, state.setValue(RedstoneWireBlock.POWER, power), 2); changed.add(pos); }
            }
            for (BlockPos pos : changed) { level.updateNeighborsAt(pos, Blocks.REDSTONE_WIRE); for (Direction d : Direction.values()) level.updateNeighborsAt(pos.relative(d), Blocks.REDSTONE_WIRE); }
            return network;
        } finally { UPDATING.set(false); }
    }
}
