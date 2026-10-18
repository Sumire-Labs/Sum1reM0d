package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;

/** A real waterlogged block: water may enter or be bucketed without extinguishing it. */
public final class GlowstoneTorchBlock extends Block implements SimpleWaterloggedBlock {
    public static final IntegerProperty LIGHT = IntegerProperty.create("light",0,15);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final boolean wall;
    //? if <26.3 {
    @Override public com.mojang.serialization.MapCodec<GlowstoneTorchBlock> codec() { return simpleCodec(p -> new GlowstoneTorchBlock(false,p)); }
    //?}
    public GlowstoneTorchBlock(boolean wall, Properties props) {
        super(props); this.wall=wall;
        registerDefaultState(stateDefinition.any().setValue(LIGHT,15).setValue(WATERLOGGED,false).setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(LIGHT,WATERLOGGED,FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state=defaultBlockState().setValue(LIGHT,Rules.on("glowstone_torch")?Rules.i("glowstone_torch","light_level"):0)
            .setValue(WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).is(net.minecraft.tags.FluidTags.WATER));
        if (!wall) return state;
        for(var direction:context.getNearestLookingDirections()) if(direction.getAxis().isHorizontal()) {
            var candidate=state.setValue(FACING,direction.getOpposite());
            if(candidate.canSurvive(context.getLevel(),context.getClickedPos())) return candidate;
        }
        return null;
    }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        Direction support=wall?state.getValue(FACING).getOpposite():Direction.DOWN;
        if(!wall)return Block.canSupportCenter(level,pos.below(),Direction.UP);
        return level.getBlockState(pos.relative(support)).isFaceSturdy(level,pos.relative(support),support.getOpposite());
    }
    @Override protected FluidState getFluidState(BlockState state) { return state.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(state); }
    @Override public boolean canPlaceLiquid(net.minecraft.world.entity.LivingEntity entity,BlockGetter level,BlockPos pos,BlockState state,Fluid fluid) {
        return !state.getValue(WATERLOGGED)&&fluid.is(net.minecraft.tags.FluidTags.WATER);
    }
    @Override public boolean placeLiquid(LevelAccessor level,BlockPos pos,BlockState state,FluidState fluid) {
        if(state.getValue(WATERLOGGED)||!fluid.is(net.minecraft.tags.FluidTags.WATER))return false;
        if(!level.isClientSide()) {
            level.setBlock(pos,state.setValue(WATERLOGGED,true),3);
            level.scheduleTick(pos,Fluids.WATER,Fluids.WATER.getTickDelay(level));
        }
        return true;
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighbor,BlockState other,RandomSource random) {
        if(state.getValue(WATERLOGGED)) ticks.scheduleTick(pos,Fluids.WATER,Fluids.WATER.getTickDelay(level));
        return canSurvive(state,level,pos)?state:state.getValue(WATERLOGGED)?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState();
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved) { if(!level.isClientSide()&&!old.is(this))level.scheduleTick(pos,this,20); }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        int light=Rules.on("glowstone_torch")?Rules.i("glowstone_torch","light_level"):0;
        if(state.getValue(LIGHT)!=light)level.setBlock(pos,state.setValue(LIGHT,light),3);
        // Previously placed dry torches may be surrounded by settled water with no pending fluid tick.
        // Ask the neighboring water to flow naturally; do not pull water upward or invent it in air.
        if(!state.getValue(WATERLOGGED))for(var direction:Direction.values()) {
            var neighbor=pos.relative(direction);
            if(!level.hasChunkAt(neighbor))continue;
            var fluid=level.getFluidState(neighbor);
            if(fluid.is(net.minecraft.tags.FluidTags.WATER))level.scheduleTick(neighbor,fluid.getType(),1);
        }
        level.scheduleTick(pos,this,20);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if(state.getValue(LIGHT)>0 && Rules.b("glowstone_torch","particles")) level.addParticle(new DustParticleOptions(0xFFE18B,.65f),pos.getX()+.5,pos.getY()+.75,pos.getZ()+.5,0,0,0);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        if(!wall)return Block.box(6,0,6,10,12,10);
        return switch(state.getValue(FACING)) { case NORTH -> Block.box(6,3,10,10,15,16);case SOUTH -> Block.box(6,3,0,10,15,6);case EAST -> Block.box(0,3,6,6,15,10);default -> Block.box(10,3,6,16,15,10); };
    }
}
