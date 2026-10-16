package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Sky-only disposable placement fixture, owned by RemainingLiveVerification. */
public final class MixedColorFixture {
 private static final List<BlockPos> occupied=new ArrayList<>();
 public static final int RGB=0x13579B;
 public static BlockPos position(int index){return new BlockPos(-2+index*2,245,4);}
 public static void setup(ServerPlayer player){
  var level=player.level();
  for(int x=-3;x<=3;x++)for(int z=3;z<=6;z++)for(int y=244;y<=246;y++)ServerVerification.require(level.getBlockState(new BlockPos(x,y,z)).isAir(),"Empty mixed-color fixture space");
  for(int x=-3;x<=3;x++)for(int z=3;z<=6;z++)for(int y=244;y<=246;y++)occupied.add(new BlockPos(x,y,z));
  for(int x=-3;x<=3;x++)for(int z=3;z<=6;z++)level.setBlock(new BlockPos(x,244,z),Blocks.STONE.defaultBlockState(),3);
  for(int i=0;i<3;i++){
   var stack=i==0?MixedConcrete.stack(RGB):DyeTextiles.stack(i==1?"mixed_wool":"mixed_bed",RGB);
   player.setItemInHand(InteractionHand.MAIN_HAND,stack.copy());var pos=position(i);
   ((BlockItem)stack.getItem()).useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false)));
   ServerVerification.require(level.getBlockEntity(pos) instanceof DyedBlockEntity entity&&entity.color()==RGB,"Real BlockItem placement preserves RGB "+i);
   player.getInventory().setItem(9+i,stack.copy());
  }
  player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
  var wire=(GlowWireBlock)ExtraContent.block("glow_wire");
  for(int x=-2;x<=1;x++)level.setBlock(new BlockPos(x,245,6),wire.defaultBlockState(),3);
  level.setBlock(new BlockPos(-2,245,5),wire.defaultBlockState(),3);
  for(int x=-2;x<=1;x++){var pos=new BlockPos(x,245,6);level.setBlock(pos,wire.connections(level,pos,level.getBlockState(pos)),3);}
  var corner=new BlockPos(-2,245,5);level.setBlock(corner,wire.connections(level,corner,level.getBlockState(corner)),3);
  // Render all six native extended-body orientations without triggering placement deletion.
  var facings=Direction.values();
  for(int i=0;i<facings.length;i++)level.setBlock(new BlockPos(-2+(i%3)*2,246,i<3?3:6),ExtraContent.block("piston_base").defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,facings[i]),3);
 }
 public static void restore(ServerPlayer player){for(var pos:occupied)player.level().setBlock(pos,Blocks.AIR.defaultBlockState(),3);occupied.clear();}
}
