package com.sumirelabs.sum1rem0d.client;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import com.sumirelabs.sum1rem0d.game.GlowWireBlock;
import com.sumirelabs.sum1rem0d.game.Rules;
public final class GlowWireTint implements BlockTintSource {
 public static final GlowWireTint INSTANCE=new GlowWireTint();private GlowWireTint(){}
 public int color(BlockState state){return switch(((GlowWireBlock)state.getBlock()).feature()){
  case "sugar_wire"->Rules.b("sugar_wire","signal_tint")?signalColor(state.getValue(GlowWireBlock.POWER),Rules.i("sugar_wire","minimum_brightness"),0xFFF4EFE6):0xFFF4EFE6;
  case "gunpowder_wire"->Rules.b("gunpowder_wire","signal_tint")?signalColor(state.getValue(GlowWireBlock.POWER),Rules.i("gunpowder_wire","minimum_brightness"),0xFF62666B):0xFF62666B;
  default->Rules.b("glow_wire","signal_tint")?signalColor(state.getValue(GlowWireBlock.POWER),Rules.i("glow_wire","minimum_brightness")):0xFFFFD45A;
 };}
 /** Keep the original gold hue and texture; vary only its brightness with actual output. */
 static int signalColor(int power,int minimumPercent){
  return signalColor(power,minimumPercent,0xFFFFD45A);
 }
 static int signalColor(int power,int minimumPercent,int color){
  double floor=Math.clamp(minimumPercent,0,100)/100.0;
  double brightness=floor+(1-floor)*Math.clamp(power,0,15)/15.0;
  return 0xFF000000|((int)Math.round(((color>>16)&255)*brightness)<<16)|((int)Math.round(((color>>8)&255)*brightness)<<8)|(int)Math.round((color&255)*brightness);
 }
}
