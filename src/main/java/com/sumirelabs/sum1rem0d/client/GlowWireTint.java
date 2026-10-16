package com.sumirelabs.sum1rem0d.client;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
public final class GlowWireTint implements BlockTintSource {
 public static final GlowWireTint INSTANCE=new GlowWireTint();private GlowWireTint(){}
 public int color(BlockState state){return 0xFFFFD45A;}
}
