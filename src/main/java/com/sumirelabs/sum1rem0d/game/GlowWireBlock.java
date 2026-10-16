package com.sumirelabs.sum1rem0d.game;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.*;
public final class GlowWireBlock extends Block {
 public static final IntegerProperty POWER=BlockStateProperties.POWER;
 public static final Map<Direction,EnumProperty<RedstoneSide>> SIDES=Map.of(Direction.NORTH,BlockStateProperties.NORTH_REDSTONE,Direction.EAST,BlockStateProperties.EAST_REDSTONE,Direction.SOUTH,BlockStateProperties.SOUTH_REDSTONE,Direction.WEST,BlockStateProperties.WEST_REDSTONE);
 private static final VoxelShape SHAPE=Block.box(0,0,0,16,1,16);
 public GlowWireBlock(Properties props){super(props);var state=stateDefinition.any().setValue(POWER,0);for(var p:SIDES.values())state=state.setValue(p,RedstoneSide.SIDE);registerDefaultState(state);}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(POWER);for(var p:SIDES.values())builder.add(p);}
 public BlockPos connection(BlockGetter level,BlockPos pos,Direction direction){
  var n=pos.relative(direction);if(level instanceof Level live && !live.hasChunkAt(n))return null;if(level.getBlockState(n).is(this))return n;
  if(!level.getBlockState(pos.above()).isRedstoneConductor(level,pos.above()) && level.getBlockState(n).isFaceSturdy(level,n,Direction.UP) && level.getBlockState(n.above()).is(this))return n.above();
  if(!level.getBlockState(n).isRedstoneConductor(level,n) && level.getBlockState(n.below()).is(this))return n.below();return null;
 }
 public BlockState connections(BlockGetter level,BlockPos pos,BlockState state){
  int count=0;Direction only=null;
  for(var dir:Direction.Plane.HORIZONTAL){var neighbor=pos.relative(dir);var target=connection(level,pos,dir);if(level instanceof Level live && !live.hasChunkAt(neighbor)){state=state.setValue(SIDES.get(dir),RedstoneSide.NONE);continue;}var other=level.getBlockState(neighbor);boolean source=other.isSignalSource() && !other.is(Blocks.REDSTONE_WIRE);
   if(other.getBlock() instanceof RepeaterBlock)source=other.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis()==dir.getAxis();
   var side=target!=null?(target.getY()>pos.getY()?RedstoneSide.UP:RedstoneSide.SIDE):source?RedstoneSide.SIDE:RedstoneSide.NONE;
   state=state.setValue(SIDES.get(dir),side);if(side!=RedstoneSide.NONE){count++;only=dir;}
  }
  if(count==0)for(var p:SIDES.values())state=state.setValue(p,RedstoneSide.SIDE);
  else if(count==1)state=state.setValue(SIDES.get(only.getOpposite()),RedstoneSide.SIDE);
  return state;
 }
 @Override public BlockState getStateForPlacement(BlockPlaceContext context){return connections(context.getLevel(),context.getClickedPos(),defaultBlockState());}
 @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){var below=pos.below();return level.getBlockState(below).isFaceSturdy(level,below,Direction.UP) || level.getBlockState(below).is(Blocks.HOPPER);}
 @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
 @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighbor,BlockState other,RandomSource random){return !canSurvive(state,level,pos)?Blocks.AIR.defaultBlockState():connections(level,pos,state);}
 @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){if(!level.isClientSide() && !old.is(this))level.scheduleTick(pos,this,1);}
 @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block source,Orientation orientation,boolean moved){
  if(level.isClientSide())return;if(!canSurvive(state,level,pos)){level.destroyBlock(pos,true);return;}
  if(GlowstoneWiring.updating()){GlowstoneWiring.changedDuringUpdate(source);return;}level.scheduleTick(pos,this,1);
 }
 @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moved){super.affectNeighborsAfterRemoval(state,level,pos,moved);level.updateNeighborsAt(pos,this);for(var d:Direction.values())if(level.hasChunkAt(pos.relative(d)))level.updateNeighborsAt(pos.relative(d),this);}
 @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){GlowstoneWiring.update(level,pos,false);}
 @Override protected boolean isSignalSource(BlockState state){return true;}
 @Override protected int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction){
  if(GlowstoneWiring.reading() || !Rules.on("glow_wire") || direction==Direction.DOWN || level.getBlockState(pos.relative(direction.getOpposite())).is(Blocks.REDSTONE_WIRE))return 0;
  return direction==Direction.UP || state.getValue(SIDES.get(direction.getOpposite())).isConnected()?state.getValue(POWER):0;
 }
 @Override protected int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction){return getSignal(state,level,pos,direction);}
 @Override protected ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state,boolean data){return new ItemStack(Items.GLOWSTONE_DUST);}
}
