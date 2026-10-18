package com.sumirelabs.sum1rem0d.game;

import java.util.Map;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.*;

public final class OpticalFiberBlock extends Block implements SimpleWaterloggedBlock {
    public static final IntegerProperty POWER=BlockStateProperties.POWER;
    public static final BooleanProperty WATERLOGGED=BlockStateProperties.WATERLOGGED;
    public static final Map<Direction,BooleanProperty> CONNECTIONS=Map.of(Direction.NORTH,BlockStateProperties.NORTH,Direction.SOUTH,BlockStateProperties.SOUTH,Direction.EAST,BlockStateProperties.EAST,Direction.WEST,BlockStateProperties.WEST,Direction.UP,BlockStateProperties.UP,Direction.DOWN,BlockStateProperties.DOWN);
    //? if <26.3 {
    private static final com.mojang.serialization.MapCodec<OpticalFiberBlock> CODEC=simpleCodec(OpticalFiberBlock::new);
    @Override public com.mojang.serialization.MapCodec<OpticalFiberBlock> codec(){return CODEC;}
    //?}
    public OpticalFiberBlock(Properties props){super(props);var state=stateDefinition.any().setValue(POWER,0).setValue(WATERLOGGED,false);for(var p:CONNECTIONS.values())state=state.setValue(p,false);registerDefaultState(state);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(POWER,WATERLOGGED);for(var p:CONNECTIONS.values())builder.add(p);}
    public BlockState connections(BlockGetter level,BlockPos pos,BlockState state){
        for(var entry:CONNECTIONS.entrySet()){
            var next=pos.relative(entry.getKey()); boolean loaded=!(level instanceof Level live)||live.hasChunkAt(next);
            var other=loaded?level.getBlockState(next):Blocks.AIR.defaultBlockState();
            state=state.setValue(entry.getValue(),other.getBlock() instanceof OpticalFiberBlock || other.isSignalSource());
        }
        return state;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return connections(context.getLevel(),context.getClickedPos(),defaultBlockState().setValue(WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER)));}
    @Override protected FluidState getFluidState(BlockState state){return state.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(state);}
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighbor,BlockState other,RandomSource random){
        if(state.getValue(WATERLOGGED))ticks.scheduleTick(pos,Fluids.WATER,Fluids.WATER.getTickDelay(level));
        return connections(level,pos,state);
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){if(!level.isClientSide()&&!old.is(this))level.scheduleTick(pos,this,1);}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block source,Orientation orientation,boolean moved){if(!level.isClientSide()&&!OpticalFiberNetwork.updating())level.scheduleTick(pos,this,1);}
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moved){super.affectNeighborsAfterRemoval(state,level,pos,moved);OpticalFiberNetwork.invalidate(level);level.updateNeighborsAt(pos,this);for(var d:Direction.values())if(level.hasChunkAt(pos.relative(d)))level.updateNeighborsAt(pos.relative(d),this);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){OpticalFiberNetwork.update(level,pos);}
    @Override protected boolean isSignalSource(BlockState state){return true;}
    @Override protected int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction){return OpticalFiberNetwork.reading()||!Rules.on("optical_fiber")?0:state.getValue(POWER);}
    @Override protected int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction){return getSignal(state,level,pos,direction);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        VoxelShape shape=Block.box(5,5,5,11,11,11);
        for(var d:Direction.values())if(state.getValue(CONNECTIONS.get(d)))shape=Shapes.or(shape,switch(d){case NORTH->Block.box(6,6,0,10,10,5);case SOUTH->Block.box(6,6,11,10,10,16);case WEST->Block.box(0,6,6,5,10,10);case EAST->Block.box(11,6,6,16,10,10);case DOWN->Block.box(6,0,6,10,5,10);case UP->Block.box(6,11,6,10,16,10);});
        return shape;
    }
}
