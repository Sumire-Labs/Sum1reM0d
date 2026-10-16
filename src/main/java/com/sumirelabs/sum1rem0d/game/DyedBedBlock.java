package com.sumirelabs.sum1rem0d.game;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
public final class DyedBedBlock extends BedBlock implements EntityBlock {
 //? if <26.3 {
 /*private static final com.mojang.serialization.MapCodec<BedBlock> CODEC=simpleCodec(DyedBedBlock::new);
 @Override public com.mojang.serialization.MapCodec<BedBlock> codec(){return CODEC;}
 *///?}
 public DyedBedBlock(Properties props){super(DyeColor.WHITE,props);}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new TextileEntity("mixed_bed",pos,state);}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity who,ItemStack stack){
  super.setPlacedBy(level,pos,state,who,stack);int rgb=DyeTextiles.color(stack);
  for(var p:java.util.List.of(pos,pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING))))if(level.getBlockEntity(p) instanceof DyedBlockEntity entity)entity.color(rgb);
 }
 @Override protected ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state,boolean data){return level.getBlockEntity(pos) instanceof DyedBlockEntity e?e.item():DyeTextiles.stack("mixed_bed",0xFFFFFF);}
}
