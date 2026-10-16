package com.sumirelabs.sum1rem0d.game;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class DyedWoolBlock extends BaseEntityBlock {
 //? if <26.3 {
 /*private static final com.mojang.serialization.MapCodec<DyedWoolBlock> CODEC=simpleCodec(DyedWoolBlock::new);
 @Override public com.mojang.serialization.MapCodec<DyedWoolBlock> codec(){return CODEC;}
 *///?}
 public DyedWoolBlock(Properties props){super(props);}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new TextileEntity("mixed_wool",pos,state);}
 @Override protected ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state,boolean data){return level.getBlockEntity(pos) instanceof DyedBlockEntity e?e.item():DyeTextiles.stack("mixed_wool",0xFFFFFF);}
}
