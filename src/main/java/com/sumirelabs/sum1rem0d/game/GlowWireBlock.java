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
 public static final EnumProperty<Direction> ATTACHMENT=EnumProperty.create("attachment",Direction.class);
 public static final IntegerProperty POWER=BlockStateProperties.POWER;
 public static final Map<Direction,EnumProperty<RedstoneSide>> SIDES=Map.of(Direction.NORTH,BlockStateProperties.NORTH_REDSTONE,Direction.EAST,BlockStateProperties.EAST_REDSTONE,Direction.SOUTH,BlockStateProperties.SOUTH_REDSTONE,Direction.WEST,BlockStateProperties.WEST_REDSTONE);
 public String feature(){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this).getPath();}
 public Item dust(){return switch(feature()){case "sugar_wire"->Items.SUGAR;case "gunpowder_wire"->Items.GUNPOWDER;default->Items.GLOWSTONE_DUST;};}
 private static final VoxelShape SHAPE=Block.box(0,0,0,16,1,16);
 public GlowWireBlock(Properties props){super(props);var state=stateDefinition.any().setValue(POWER,0).setValue(ATTACHMENT,Direction.UP);for(var p:SIDES.values())state=state.setValue(p,RedstoneSide.SIDE);registerDefaultState(state);}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(POWER,ATTACHMENT);for(var p:SIDES.values())builder.add(p);}
 public static Direction planeDirection(Direction face,Direction logical){
  if(face==Direction.UP)return logical;
  if(face==Direction.DOWN)return logical==Direction.NORTH?Direction.SOUTH:logical==Direction.SOUTH?Direction.NORTH:logical;
  if(logical==Direction.NORTH)return Direction.DOWN;if(logical==Direction.SOUTH)return Direction.UP;
  Direction east=switch(face){case NORTH->Direction.EAST;case SOUTH->Direction.WEST;case EAST->Direction.SOUTH;default->Direction.NORTH;};return logical==Direction.EAST?east:east.getOpposite();
 }
 public Iterable<Direction> directions(BlockGetter level,BlockPos pos){var current=level.getBlockState(pos);if(!current.is(this))return List.of();var face=current.getValue(ATTACHMENT);var result=new ArrayList<Direction>();for(var local:Direction.Plane.HORIZONTAL)result.add(planeDirection(face,local));return result;}
 public BlockPos connection(BlockGetter level,BlockPos pos,Direction direction){
  var state=level.getBlockState(pos);return state.is(this)?connection(level,pos,direction,state):null;
 }
 private BlockPos connection(BlockGetter level,BlockPos pos,Direction direction,BlockState state){
  var n=pos.relative(direction);if(level instanceof Level live && !live.hasChunkAt(n))return null;if(level.getBlockState(n).is(this)&&level.getBlockState(n).getValue(ATTACHMENT)==state.getValue(ATTACHMENT))return n;
  if(state.getValue(ATTACHMENT)!=Direction.UP)return null;
  if(!level.getBlockState(pos.above()).isRedstoneConductor(level,pos.above()) && level.getBlockState(n).isFaceSturdy(level,n,Direction.UP) && level.getBlockState(n.above()).is(this))return n.above();
  if(!level.getBlockState(n).isRedstoneConductor(level,n) && level.getBlockState(n.below()).is(this))return n.below();return null;
 }
 public BlockState connections(BlockGetter level,BlockPos pos,BlockState state){
  int count=0;Direction only=null;
  for(var local:Direction.Plane.HORIZONTAL){var dir=planeDirection(state.getValue(ATTACHMENT),local);var neighbor=pos.relative(dir);var target=connection(level,pos,dir,state);if(level instanceof Level live && !live.hasChunkAt(neighbor)){state=state.setValue(SIDES.get(local),RedstoneSide.NONE);continue;}var other=level.getBlockState(neighbor);boolean source=other.isSignalSource() && !other.is(Blocks.REDSTONE_WIRE) && !(other.getBlock() instanceof GlowWireBlock);
   if(other.getBlock() instanceof RepeaterBlock)source=other.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis()==dir.getAxis();
   var side=target!=null?(target.getY()>pos.getY()?RedstoneSide.UP:RedstoneSide.SIDE):source?RedstoneSide.SIDE:RedstoneSide.NONE;
   state=state.setValue(SIDES.get(local),side);if(side!=RedstoneSide.NONE){count++;only=local;}
  }
  if(count==0)for(var p:SIDES.values())state=state.setValue(p,RedstoneSide.SIDE);
  else if(count==1)state=state.setValue(SIDES.get(only.getOpposite()),RedstoneSide.SIDE);
  return state;
 }
 @Override public BlockState getStateForPlacement(BlockPlaceContext context){var face=context.getClickedFace();if(face!=Direction.UP&&!Rules.b(feature(),"wall_ceiling"))return null;return connections(context.getLevel(),context.getClickedPos(),defaultBlockState().setValue(ATTACHMENT,face));}
 @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){var face=state.getValue(ATTACHMENT);var below=pos.relative(face.getOpposite());return level.getBlockState(below).isFaceSturdy(level,below,face) || face==Direction.UP&&level.getBlockState(below).is(Blocks.HOPPER);}
 @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return switch(state.getValue(ATTACHMENT)){case UP->SHAPE;case DOWN->Block.box(0,15,0,16,16,16);case NORTH->Block.box(0,0,15,16,16,16);case SOUTH->Block.box(0,0,0,16,16,1);case EAST->Block.box(0,0,0,1,16,16);case WEST->Block.box(15,0,0,16,16,16);};}
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
  if(GlowstoneWiring.reading() || !Rules.on(feature()) || direction==state.getValue(ATTACHMENT).getOpposite() || level.getBlockState(pos.relative(direction.getOpposite())).is(Blocks.REDSTONE_WIRE))return 0;
  if(direction==state.getValue(ATTACHMENT))return state.getValue(POWER);
  for(var local:Direction.Plane.HORIZONTAL)if(planeDirection(state.getValue(ATTACHMENT),local)==direction.getOpposite())return state.getValue(SIDES.get(local)).isConnected()?state.getValue(POWER):0;return 0;
 }
 @Override protected int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction){return getSignal(state,level,pos,direction);}
 @Override protected ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state,boolean data){return new ItemStack(dust());}
}
