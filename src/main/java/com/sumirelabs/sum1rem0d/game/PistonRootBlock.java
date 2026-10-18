package com.sumirelabs.sum1rem0d.game;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
public final class PistonRootBlock extends Block {
 private static final java.util.Map<Direction,net.minecraft.world.phys.shapes.VoxelShape> SHAPES=java.util.Map.of(
  Direction.NORTH,Block.box(0,0,4,16,16,16),Direction.SOUTH,Block.box(0,0,0,16,16,12),
  Direction.WEST,Block.box(4,0,0,16,16,16),Direction.EAST,Block.box(0,0,0,12,16,16),
  Direction.UP,Block.box(0,0,0,16,12,16),Direction.DOWN,Block.box(0,4,0,16,16,16));
 // Copied piston predicates read EXTENDED, which this fixed extended-body block does not have.
 //? if >=26.3 {
 public PistonRootBlock(Properties props){super(props.isSuffocating((state,level,pos)->false).isViewBlocking((state,level,pos,box)->false));registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING,Direction.NORTH));}
 //?} else {
 /*public PistonRootBlock(Properties props){super(props.isSuffocating((state,level,pos)->false).isViewBlocking((state,level,pos)->false));registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING,Direction.NORTH));}
 *///?}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(BlockStateProperties.FACING);}
 @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return SHAPES.get(state.getValue(BlockStateProperties.FACING));}
 @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(BlockStateProperties.FACING,context.getNearestLookingDirection().getOpposite());}
 @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity entity,ItemStack stack){
  if(!(level instanceof ServerLevel server) || !Rules.on("piston_base"))return;
  var target=pos.relative(state.getValue(BlockStateProperties.FACING));if(!level.hasChunkAt(target))return;
  var victim=level.getBlockState(target);boolean force=Rules.b("piston_base","force_air");if(victim.isAir() || !force&&Rules.matches(victim,Rules.list("piston_base","excluded_blocks")))return;
  if(entity instanceof Player player && !server.mayInteract(player,target))return;
  if(!force&&!Rules.b("piston_base","destroy_block_entities") && level.getBlockEntity(target)!=null)return;
  if(force){
   if(level.getBlockEntity(target) instanceof net.minecraft.world.Container container)container.clearContent();
   level.removeBlockEntity(target);
   level.setBlock(target,Blocks.AIR.defaultBlockState(),3);
  }else level.removeBlock(target,false);
 }
}
