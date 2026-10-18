package com.sumirelabs.sum1rem0d.game;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** A deliberate right click travels to an existing, safely reachable lodestone. */
public final class LodestoneTravel {
 private LodestoneTravel(){}
 public static InteractionResult use(ServerPlayer player,ItemStack stack){
  if(!Rules.on("lodestone_travel")||!stack.is(Items.COMPASS))return null;
  var tracker=stack.get(DataComponents.LODESTONE_TRACKER);if(tracker==null||tracker.target().isEmpty())return null;
  var target=tracker.target().get();var level=player.level().getServer().getLevel(target.dimension());
  if(level==null||(!Rules.b("lodestone_travel","cross_dimension")&&level!=player.level()))return InteractionResult.FAIL;
  if(player.getCooldowns().isOnCooldown(stack))return InteractionResult.FAIL;
  if(!level.hasChunkAt(target.pos())){if(!Rules.b("lodestone_travel","load_target"))return InteractionResult.FAIL;level.getChunk(target.pos());}
  if(!level.getBlockState(target.pos()).is(Blocks.LODESTONE))return InteractionResult.FAIL;
  int radius=Rules.i("lodestone_travel","safe_radius");
  for(int distance=0;distance<=radius;distance++)for(int y=0;y<=2;y++)for(int x=-distance;x<=distance;x++)for(int z=-distance;z<=distance;z++){
   if(Math.max(Math.abs(x),Math.abs(z))!=distance)continue;
   BlockPos feet=target.pos().offset(x,1+y,z);
   if(!level.hasChunkAt(feet)||!level.getWorldBorder().isWithinBounds(feet)||!level.getFluidState(feet).isEmpty()||!level.getFluidState(feet.above()).isEmpty())continue;
   if(level.getBlockState(feet.below()).getCollisionShape(level,feet.below()).isEmpty())continue;
   var box=player.getBoundingBox().move(feet.getX()+.5-player.getX(),feet.getY()-player.getY(),feet.getZ()+.5-player.getZ());
   if(!level.noCollision(player,box))continue;
   if(player.teleportTo(level,feet.getX()+.5,feet.getY(),feet.getZ()+.5,Set.of(),player.getYRot(),player.getXRot(),false)){
    player.fallDistance=0;player.getCooldowns().addCooldown(stack,Rules.i("lodestone_travel","cooldown_ticks"));return InteractionResult.SUCCESS;
   }
  }
  return InteractionResult.FAIL;
 }
}
