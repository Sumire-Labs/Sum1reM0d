package com.sumirelabs.sum1rem0d.client;
import com.sumirelabs.sum1rem0d.game.MixedConcreteEntity;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
public final class MixedConcreteTint implements BlockTintSource {
 public int color(BlockState state){return 0xFFFFFFFF;}
 public int colorInWorld(BlockState state,BlockAndTintGetter level,BlockPos pos){return level.getBlockEntity(pos) instanceof MixedConcreteEntity entity?0xFF000000|entity.color():0xFFFFFFFF;}
}
