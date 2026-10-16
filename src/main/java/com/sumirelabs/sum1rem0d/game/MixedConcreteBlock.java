package com.sumirelabs.sum1rem0d.game;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class MixedConcreteBlock extends BaseEntityBlock {
 //? if <26.3 {
 /*public static final MapCodec<MixedConcreteBlock> CODEC=simpleCodec(MixedConcreteBlock::new);
 @Override public MapCodec<MixedConcreteBlock> codec(){return CODEC;}
 *///?}
 public MixedConcreteBlock(Properties props){super(props);}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MixedConcreteEntity(pos,state);}
 @Override protected ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state,boolean includeData){return level.getBlockEntity(pos) instanceof MixedConcreteEntity e?e.item():MixedConcrete.stack(0xFFFFFF);}
}
