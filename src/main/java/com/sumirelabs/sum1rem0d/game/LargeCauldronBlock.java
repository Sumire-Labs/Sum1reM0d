package com.sumirelabs.sum1rem0d.game;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A save-safe connected cauldron footprint; each cell persists its own 0..3 fluid level in blockstate. */
public final class LargeCauldronBlock extends AbstractCauldronBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);
    public static final BooleanProperty Z_AXIS = BooleanProperty.create("z_axis");
    public static final EnumProperty<Contents> CONTENTS = EnumProperty.create("contents", Contents.class);
    private static final ThreadLocal<Boolean> REPLACING_FOOTPRINT = ThreadLocal.withInitial(() -> false);
    private final int depth;
    private final Map<Integer, VoxelShape> footprintShapes = new ConcurrentHashMap<>();
    //? if <26.3 {
    private final MapCodec<LargeCauldronBlock> CODEC;
    @Override public MapCodec<LargeCauldronBlock> codec() { return CODEC; }
    //?}

    public enum Contents implements StringRepresentable {
        EMPTY("empty"), WATER("water"), LAVA("lava"), POWDER_SNOW("powder_snow");
        private final String name;
        Contents(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public LargeCauldronBlock(int depth, BlockBehaviour.Properties properties) {
        super(properties, CauldronInteractions.EMPTY);
        if (depth != 1 && depth != 2) throw new IllegalArgumentException("Cauldron depth must be 1 or 2");
        this.depth = depth;
        //? if <26.3 {
        this.CODEC = simpleCodec(p -> new LargeCauldronBlock(depth, p));
        //?}
        registerDefaultState(stateDefinition.any().setValue(PART, 0).setValue(Z_AXIS, false).setValue(CONTENTS, Contents.EMPTY)
                .setValue(LEVEL, 0));
    }

    public int depth() { return depth; }
    @Override public boolean isFull(BlockState state) { return state.getValue(LEVEL) >= 3; }
    @Override protected double getContentHeight(BlockState state) { return 6.0 / 16.0 + state.getValue(LEVEL) * (3.0 / 16.0); }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return footprintShape(state);
    }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return footprintShape(state);
    }

    private VoxelShape footprintShape(BlockState state) {
        int part = state.getValue(PART);
        boolean zAxis = depth == 1 && state.getValue(Z_AXIS);
        int key = part | (zAxis ? 4 : 0);
        return footprintShapes.computeIfAbsent(key, ignored -> createFootprintShape(part, zAxis));
    }

    private VoxelShape createFootprintShape(int part, boolean zAxis) {
        int columns = depth == 2 || !zAxis ? 2 : 1;
        int rows = depth == 2 || zAxis ? 2 : 1;
        int cellX = zAxis ? 0 : part % 2;
        int cellZ = depth == 2 ? part / 2 : zAxis ? part : 0;
        boolean west = cellX == 0, east = cellX == columns - 1;
        boolean north = cellZ == 0, south = cellZ == rows - 1;
        int floorX0 = west ? 2 : 0, floorX1 = east ? 14 : 16;
        int floorZ0 = north ? 2 : 0, floorZ1 = south ? 14 : 16;
        VoxelShape shape = Shapes.empty();
        // The basin floor spans seams between parts; only the footprint perimeter retains walls.
        shape = Shapes.or(shape, Block.box(floorX0, 0, floorZ0, floorX1, 4, floorZ1));
        if (west) shape = Shapes.or(shape, Block.box(0, 3, 0, 2, 16, 16));
        if (east) shape = Shapes.or(shape, Block.box(14, 3, 0, 16, 16, 16));
        if (north) shape = Shapes.or(shape, Block.box(floorX0, 3, 0, floorX1, 16, 2));
        if (south) shape = Shapes.or(shape, Block.box(floorX0, 3, 14, floorX1, 16, 16));
        if (west && north) {
            shape = Shapes.or(shape, Block.box(0, 0, 0, 4, 3, 2));
            shape = Shapes.or(shape, Block.box(0, 0, 2, 2, 3, 4));
        }
        if (east && north) {
            shape = Shapes.or(shape, Block.box(12, 0, 0, 16, 3, 2));
            shape = Shapes.or(shape, Block.box(14, 0, 2, 16, 3, 4));
        }
        if (west && south) {
            shape = Shapes.or(shape, Block.box(0, 0, 14, 4, 3, 16));
            shape = Shapes.or(shape, Block.box(0, 0, 12, 2, 3, 14));
        }
        if (east && south) {
            shape = Shapes.or(shape, Block.box(12, 0, 14, 16, 3, 16));
            shape = Shapes.or(shape, Block.box(14, 0, 12, 16, 3, 14));
        }
        return shape;
    }

    /** True when this cauldron cell belongs to a water-filled large footprint. */
    public boolean isWater(BlockState state) { return state.getBlock() == this && state.getValue(CONTENTS) == Contents.WATER; }
    public boolean isWater(Level level, BlockPos pos) { return availableWater(level, pos) > 0; }

    /** Total usable water units across this cell's complete 2x1 or 2x2 footprint. */
    public int availableWater(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return 0;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != this) return 0;
        Footprint footprint = read(level, root(pos, state.getValue(PART), state));
        return footprint != null && footprint.contents() == Contents.WATER ? footprint.total() : 0;
    }

    /** Removes persisted water units, keeping every segment's content and level state consistent. */
    public boolean consumeWater(Level level, BlockPos pos, int units) {
        if (level.isClientSide() || units <= 0 || !level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != this) return false;
        BlockPos root = root(pos, state.getValue(PART), state);
        Footprint footprint = read(level, root);
        if (footprint == null || footprint.contents() != Contents.WATER || footprint.total() < units) return false;
        write(level, root, footprint.total() - units, Contents.WATER);
        return true;
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, Z_AXIS, CONTENTS, LEVEL);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!WorldMobility.enabled("large_cauldron")) return null;
        BlockPos root = context.getClickedPos();
        Level level = context.getLevel();
        boolean zAxis = depth == 1 && context.getHorizontalDirection().getAxis() == Direction.Axis.Z;
        for (int z = 0; z < depth; z++) for (int x = 0; x < 2; x++) {
            BlockPos cell = cell(root, zAxis, x, z);
            if (!level.hasChunkAt(cell)) return null;
            if (!cell.equals(root) && !level.getBlockState(cell).canBeReplaced(context)) return null;
        }
        return defaultBlockState().setValue(Z_AXIS, zAxis);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide()) return;
        for (int z = 0; z < depth; z++) for (int x = 0; x < 2; x++) {
            int part = x + z * 2;
            if (part == 0) continue;
            BlockPos cell = cell(pos, state.getValue(Z_AXIS), x, z);
            level.setBlock(cell, defaultBlockState().setValue(PART, part).setValue(Z_AXIS, state.getValue(Z_AXIS)), 3);
        }
        if (depth == 1 && level instanceof ServerLevel server && WorldMobility.enabled("large_cauldron") && Rules.b("large_cauldron", "auto_merge")) {
            tryMergeWithAdjacent(server, pos);
        }
    }

    @Override protected void affectNeighborsAfterRemoval(BlockState oldState, ServerLevel level, BlockPos pos, boolean moved) {
        if (oldState.getBlock() == this && !moved && !REPLACING_FOOTPRINT.get()) {
            BlockPos root = root(pos, oldState.getValue(PART), oldState);
            boolean previous = REPLACING_FOOTPRINT.get(); REPLACING_FOOTPRINT.set(true);
            try {
                for (int z = 0; z < depth; z++) for (int x = 0; x < 2; x++) {
                    int part = x + z * 2;
                    if (part == oldState.getValue(PART)) continue;
                    BlockPos cell = cell(root, oldState.getValue(Z_AXIS), x, z);
                    BlockState state = level.getBlockState(cell);
                    if (state.getBlock() == this && state.getValue(PART) == part) level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
                }
            } finally { REPLACING_FOOTPRINT.set(previous); }
        }
        super.affectNeighborsAfterRemoval(oldState, level, pos, moved);
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.WATER_BUCKET)) return addBucket(stack, state, level, pos, player, hand, Contents.WATER);
        if (stack.is(Items.LAVA_BUCKET)) return addBucket(stack, state, level, pos, player, hand, Contents.LAVA);
        if (stack.is(Items.POWDER_SNOW_BUCKET)) return addBucket(stack, state, level, pos, player, hand, Contents.POWDER_SNOW);
        if (stack.is(Items.BUCKET)) return takeBucket(stack, state, level, pos, player, hand);
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    private InteractionResult addBucket(ItemStack input, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, Contents contents) {
        if (!WorldMobility.enabled("large_cauldron")) return InteractionResult.FAIL;
        int part = state.getValue(PART); BlockPos root = root(pos, part, state);
        Footprint footprint = read(level, root);
        if (footprint == null || footprint.total() + 3 > footprint.capacity()
                || footprint.contents() != Contents.EMPTY && footprint.contents() != contents) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        write(level, root, footprint.total() + 3, contents);
        Item result = Items.BUCKET;
        player.setItemInHand(hand, ItemUtils.createFilledResult(input, player, new ItemStack(result)));
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }

    private InteractionResult takeBucket(ItemStack input, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        int part = state.getValue(PART); BlockPos root = root(pos, part, state);
        Footprint footprint = read(level, root);
        if (footprint == null || footprint.total() < 3 || footprint.contents() == Contents.EMPTY) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Contents contents = footprint.contents();
        Item result = switch (contents) { case WATER -> Items.WATER_BUCKET; case LAVA -> Items.LAVA_BUCKET; case POWDER_SNOW -> Items.POWDER_SNOW_BUCKET; default -> Items.AIR; };
        if (result == Items.AIR) return InteractionResult.FAIL;
        write(level, root, footprint.total() - 3, contents);
        player.setItemInHand(hand, ItemUtils.createFilledResult(input, player, new ItemStack(result)));
        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }

    private record Footprint(List<BlockPos> cells, Contents contents, int total, int capacity) {}
    private Footprint read(Level level, BlockPos root) {
        List<BlockPos> cells = new ArrayList<>(2 * depth); Contents common = Contents.EMPTY; int total = 0;
        for (int z = 0; z < depth; z++) for (int x = 0; x < 2; x++) {
            BlockPos cell = cell(root, depth == 1 && level.getBlockState(root).getValue(Z_AXIS), x, z);
            if (!level.hasChunkAt(cell)) return null;
            BlockState current = level.getBlockState(cell); int expected = x + z * 2;
            if (current.getBlock() != this || current.getValue(PART) != expected) return null;
            Contents cellContents = current.getValue(CONTENTS); int amount = current.getValue(LEVEL);
            if (amount > 0 && common != Contents.EMPTY && common != cellContents) return null;
            if (amount > 0) common = cellContents;
            total += amount; cells.add(cell);
        }
        return new Footprint(List.copyOf(cells), common, total, cells.size() * 3);
    }

    private void write(Level level, BlockPos root, int amount, Contents contents) {
        Footprint footprint = read(level, root); if (footprint == null) return;
        boolean previous = REPLACING_FOOTPRINT.get(); REPLACING_FOOTPRINT.set(true);
        try {
            int remaining = Math.max(0, Math.min(footprint.capacity(), amount));
            Contents stored = remaining == 0 ? Contents.EMPTY : contents;
            for (int index = 0; index < footprint.cells().size(); index++) {
                BlockPos cell = footprint.cells().get(index); int levelAmount = Math.min(3, remaining); remaining -= levelAmount;
                BlockState next = level.getBlockState(cell).setValue(CONTENTS, stored).setValue(LEVEL, levelAmount);
                level.setBlock(cell, next, 3);
            }
        } finally { REPLACING_FOOTPRINT.set(previous); }
    }

    private BlockPos root(BlockPos pos, int part, BlockState state) {
        return depth == 1 ? state.getValue(Z_AXIS) ? pos.offset(0, 0, -part) : pos.offset(-part, 0, 0)
                : pos.offset(-(part % 2), 0, -(part / 2));
    }

    private BlockPos cell(BlockPos root, boolean zAxis, int x, int z) {
        if (depth == 1) return root.offset(zAxis ? 0 : x, 0, zAxis ? x : 0);
        return root.offset(x, 0, z);
    }

    /** Converts ordinary vanilla cauldrons when an adjacent 2x1 or 2x2 footprint is completed. */
    public static void tryMergeVanilla(Level world, BlockPos placedPos, BlockState placedState) {
        if (!(world instanceof ServerLevel level) || REPLACING_FOOTPRINT.get() || !WorldMobility.enabled("large_cauldron")
                || !Rules.b("large_cauldron", "auto_merge") || !(placedState.getBlock() instanceof AbstractCauldronBlock)
                || placedState.getBlock() instanceof LargeCauldronBlock) return;

        LargeCauldronBlock huge = MobilityContent.hugeCauldron();
        if (huge != null) {
            for (int rootX = 0; rootX <= 1; rootX++) for (int rootZ = 0; rootZ <= 1; rootZ++) {
                BlockPos root = placedPos.offset(-rootX, 0, -rootZ);
                List<BlockPos> cells = new ArrayList<>(4); List<Contents> types = new ArrayList<>(4); int[] amounts = new int[4];
                boolean complete = true, includesVanilla = false; Contents common = Contents.EMPTY;
                for (int z = 0; z < 2 && complete; z++) for (int x = 0; x < 2; x++) {
                    BlockPos cell = root.offset(x, 0, z);
                    if (!level.hasChunkAt(cell)) { complete = false; break; }
                    BlockState current = level.getBlockState(cell);
                    if (!(current.getBlock() instanceof AbstractCauldronBlock cauldron)) { complete = false; break; }
                    Contents content; int amount;
                    if (cauldron instanceof LargeCauldronBlock large) {
                        if (large.depth() != 1) { complete = false; break; }
                        content = current.getValue(CONTENTS); amount = current.getValue(LEVEL);
                    } else {
                        includesVanilla = true;
                        content = nativeContents(current);
                        amount = nativeAmount(current, content);
                    }
                    if (amount > 0 && common != Contents.EMPTY && common != content) { complete = false; break; }
                    if (amount > 0) common = content;
                    int index = x + z * 2; cells.add(cell); types.add(content); amounts[index] = amount;
                }
                if (!complete || !includesVanilla || !validSmallGroups(level, root)) continue;
                if (applyFootprint(level, huge, root, false, types, amounts)) return;
            }
        }

        LargeCauldronBlock large = MobilityContent.largeCauldron();
        if (large == null) return;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = placedPos.relative(direction);
            if (!level.hasChunkAt(neighbor)) continue;
            BlockState firstState = level.getBlockState(placedPos), secondState = level.getBlockState(neighbor);
            if (!isVanillaCauldron(firstState) || !isVanillaCauldron(secondState)) continue;
            Contents firstType = nativeContents(firstState), secondType = nativeContents(secondState);
            int firstAmount = nativeAmount(firstState, firstType), secondAmount = nativeAmount(secondState, secondType);
            if (firstAmount > 0 && secondAmount > 0 && firstType != secondType) continue;
            Contents common = firstAmount > 0 ? firstType : secondAmount > 0 ? secondType : Contents.EMPTY;
            BlockPos root = direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? neighbor : placedPos;
            boolean zAxis = direction.getAxis() == Direction.Axis.Z;
            List<Contents> types = List.of(common, common);
            int[] amounts = direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE
                    ? new int[] {secondAmount, firstAmount} : new int[] {firstAmount, secondAmount};
            if (applyFootprint(level, large, root, zAxis, types, amounts)) return;
        }
    }

    private static boolean isVanillaCauldron(BlockState state) {
        return state.getBlock() instanceof AbstractCauldronBlock && !(state.getBlock() instanceof LargeCauldronBlock);
    }

    private static Contents nativeContents(BlockState state) {
        if (state.is(Blocks.WATER_CAULDRON)) return Contents.WATER;
        if (state.is(Blocks.LAVA_CAULDRON)) return Contents.LAVA;
        if (state.is(Blocks.POWDER_SNOW_CAULDRON)) return Contents.POWDER_SNOW;
        return Contents.EMPTY;
    }

    private static int nativeAmount(BlockState state, Contents content) {
        if (content == Contents.LAVA) return 3;
        if (content == Contents.EMPTY || !state.hasProperty(BlockStateProperties.LEVEL_CAULDRON)) return 0;
        return Math.max(0, Math.min(3, state.getValue(BlockStateProperties.LEVEL_CAULDRON)));
    }

    private static boolean validSmallGroups(ServerLevel level, BlockPos root) {
        for (int z = 0; z < 2; z++) for (int x = 0; x < 2; x++) {
            BlockPos pos = root.offset(x, 0, z); BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof LargeCauldronBlock large) || large.depth() != 1) continue;
            boolean axisZ = state.getValue(Z_AXIS); int part = state.getValue(PART);
            int mateX = x + (axisZ ? 0 : part == 0 ? 1 : -1);
            int mateZ = z + (axisZ ? part == 0 ? 1 : -1 : 0);
            if (mateX < 0 || mateX > 1 || mateZ < 0 || mateZ > 1) return false;
            BlockState mate = level.getBlockState(root.offset(mateX, 0, mateZ));
            if (mate.getBlock() != large || mate.getValue(Z_AXIS) != axisZ || mate.getValue(PART) != 1 - part) return false;
        }
        return true;
    }

    private static boolean applyFootprint(ServerLevel level, LargeCauldronBlock target, BlockPos root, boolean zAxis,
            List<Contents> types, int[] amounts) {
        boolean previous = REPLACING_FOOTPRINT.get(); REPLACING_FOOTPRINT.set(true);
        try {
            int expected = target.depth() * 2;
            if (types.size() != expected || amounts.length != expected) return false;
            for (int index = 0; index < expected; index++) {
                int x = index % 2, z = index / 2;
                BlockState converted = target.defaultBlockState().setValue(PART,index).setValue(Z_AXIS,zAxis)
                        .setValue(CONTENTS,types.get(index)).setValue(LEVEL,amounts[index]);
                level.setBlock(target.cell(root,zAxis,x,z),converted,3);
            }
            return true;
        } finally { REPLACING_FOOTPRINT.set(previous); }
    }

    private void tryMergeWithAdjacent(ServerLevel level, BlockPos placedRoot) {
        LargeCauldronBlock other = MobilityContent.largeCauldron();
        if (other == null || other.depth() != 1) return;
        boolean zAxis = level.getBlockState(placedRoot).getValue(Z_AXIS);
        for (int perpendicular : new int[] {-1, 1}) {
            BlockPos otherRoot = zAxis ? placedRoot.offset(perpendicular, 0, 0) : placedRoot.offset(0, 0, perpendicular);
            BlockPos otherPart = zAxis ? otherRoot.south() : otherRoot.east();
            BlockState first = level.getBlockState(otherRoot), second = level.getBlockState(otherPart);
            if (first.getBlock() != other || second.getBlock() != other || first.getValue(PART) != 0 || second.getValue(PART) != 1
                    || first.getValue(Z_AXIS) != zAxis || second.getValue(Z_AXIS) != zAxis) continue;
            Footprint a = read(level, placedRoot), b = other.read(level, otherRoot);
            if (a == null || b == null || a.contents() != Contents.EMPTY && b.contents() != Contents.EMPTY && a.contents() != b.contents()) continue;
            Contents contents = a.contents() == Contents.EMPTY ? b.contents() : a.contents();
            int[] amounts = new int[4];
            // Rebuild arrays in row-major 2x2 order, keeping each old cell's own fluid amount.
            for (int part = 0; part < 2; part++) {
                int currentIndex = zAxis ? (perpendicular < 0 ? 1 : 0) + part * 2 : part + (perpendicular < 0 ? 1 : 0) * 2;
                int otherIndex = zAxis ? (perpendicular < 0 ? 0 : 1) + part * 2 : part + (perpendicular < 0 ? 0 : 1) * 2;
                amounts[currentIndex] = level.getBlockState(cell(placedRoot,zAxis,part,0)).getValue(LEVEL);
                amounts[otherIndex] = level.getBlockState(cell(otherRoot,zAxis,part,0)).getValue(LEVEL);
            }
            BlockPos mergedRoot = zAxis ? (perpendicular < 0 ? otherRoot : placedRoot) : (perpendicular < 0 ? otherRoot : placedRoot);
            boolean previous = REPLACING_FOOTPRINT.get(); REPLACING_FOOTPRINT.set(true);
            try {
                for (int z = 0; z < 2; z++) for (int x = 0; x < 2; x++) {
                    int index=x+z*2; BlockPos cell=mergedRoot.offset(x,0,z);
                    BlockState upgraded=other.defaultBlockState().setValue(PART,index).setValue(Z_AXIS,false).setValue(CONTENTS,contents).setValue(LEVEL,amounts[index]);
                    level.setBlock(cell,upgraded,3);
                }
            } finally { REPLACING_FOOTPRINT.set(previous); }
            return;
        }
    }
}
