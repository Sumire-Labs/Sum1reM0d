package com.sumirelabs.sum1rem0d.game;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;

public final class WorldSurvivalRules {
 private static final net.minecraft.resources.Identifier PATH_SPEED=ModContent.id("path_speed");
 private static final List<EquipmentSlot> ARMOR=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
 private WorldSurvivalRules(){}
 private static JsonObject data(WorldState world,ServerPlayer player){var p=world.player(player);if(p.survival==null)p.survival=new JsonObject();return p.survival;}
 public static void tickPlayer(ServerPlayer player,WorldState world,long tick){
  var meta=data(world,player);long clock=player.level().getOverworldClockTime();
  if(Rules.on("noon_undress")&&Gameplay.eligible(player)&&meta.has("clock")){
   long before=meta.get("clock").getAsLong(),day=Math.floorDiv(clock,24000),trigger=day*24000+Rules.i("noon_undress","time");
   if(before<trigger&&clock>=trigger&&clock>=before&&(!meta.has("undressedDay")||meta.get("undressedDay").getAsLong()!=day)){
    meta.addProperty("undressedDay",day);
    for(var slot:ARMOR){var item=player.getItemBySlot(slot);if(item.isEmpty())continue;player.setItemSlot(slot,ItemStack.EMPTY);if(!player.getInventory().add(item)){if(Rules.b("noon_undress","drop_if_full"))player.drop(item,false, net.minecraft.util.Prediction.SERVER_ONLY);else player.setItemSlot(slot,item);}}
    player.inventoryMenu.broadcastFullState();
   }
  }
  meta.addProperty("clock",clock);
  var feet=BlockPos.containing(player.getX(),player.getY()-0.05,player.getZ());var floor=player.level().getBlockState(feet);
  var speed=player.getAttribute(Attributes.MOVEMENT_SPEED);boolean fast=Gameplay.eligible(player)&&Rules.on("path_speed")&&Rules.matches(floor,Rules.list("path_speed","target_blocks"));
  if(speed!=null){var old=speed.getModifier(PATH_SPEED);double desired=Rules.on("path_speed")?Rules.n("path_speed","multiplier")-1:0;if(old!=null&&(!fast||old.amount()!=desired))speed.removeModifier(PATH_SPEED);if(fast&&!speed.hasModifier(PATH_SPEED))speed.addTransientModifier(new AttributeModifier(PATH_SPEED,desired,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
  if(!Gameplay.eligible(player))return;
  if(Rules.on("stonecutter_hazard")&&tick%Rules.i("stonecutter_hazard","interval")==0&&floor.is(Blocks.STONECUTTER))player.hurtServer(player.level(),player.damageSources().generic(),(float)Rules.n("stonecutter_hazard","damage"));
  if(Rules.on("nether_heat")&&player.level().dimension()==Level.NETHER&&tick%Rules.i("nether_heat","interval")==0){
   var enchant=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_PROTECTION);
   boolean protectedArmor=ARMOR.stream().anyMatch(slot->EnchantmentHelper.getItemEnchantmentLevel(enchant,player.getItemBySlot(slot))>=Rules.i("nether_heat","required_level"));
   if(!protectedArmor&&!(Rules.b("nether_heat","allow_fire_resistance")&&player.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)))player.hurtServer(player.level(),player.damageSources().generic(),(float)Rules.n("nether_heat","damage"));
  }
 }
 public static void sulfurContact(Entity entity,boolean geyser){
  if(!(entity instanceof ServerPlayer player)||!Gameplay.eligible(player)||!Rules.on("sulfur_dirt")||!Rules.b("sulfur_dirt",geyser?"geysers":"water"))return;
  if(!geyser&&!player.isInWater())return;
  var world=WorldState.get(player.level().getServer());var meta=data(world,player);String key=geyser?"geyserDirtyAt":"sulfurDirtyAt";long tick=player.level().getGameTime();
  if(!geyser)meta.addProperty("sulfurWaterUntil",tick+20);
  if(meta.has(key)&&tick>=meta.get(key).getAsLong()&&tick-meta.get(key).getAsLong()<Rules.i("sulfur_dirt","interval"))return;
  meta.addProperty(key,tick);world.player(player).cleanliness=Math.max(0,world.player(player).cleanliness-Rules.n("sulfur_dirt",geyser?"geyser_amount":"amount"));
 }
 public static boolean cleanWater(ServerPlayer player,WorldState state){var meta=data(state,player);return !Rules.on("sulfur_dirt")||!Rules.b("sulfur_dirt","water")||!meta.has("sulfurWaterUntil")||meta.get("sulfurWaterUntil").getAsLong()<player.level().getGameTime();}
 public static void tickServer(MinecraftServer server){
  if(!Rules.on("endrod_steam")||server.overworld().getGameTime()%Rules.i("endrod_steam","interval")!=0)return;
  var emitted=new HashSet<String>();int radius=Rules.i("endrod_steam","radius");
  for(var player:server.getPlayerList().getPlayers())for(var pos:BlockPos.betweenClosed(player.blockPosition().offset(-radius,-radius,-radius),player.blockPosition().offset(radius,radius,radius))){
   var level=player.level();if(!level.hasChunkAt(pos))continue;var block=level.getBlockState(pos);if(!block.is(Blocks.END_ROD)||!emitted.add(level.dimension().toString()+pos.asLong()))continue;
   var facing=block.getValue(EndRodBlock.FACING);double x=pos.getX()+.5+facing.getStepX()*.55,y=pos.getY()+.5+facing.getStepY()*.55,z=pos.getZ()+.5+facing.getStepZ()*.55;
   //? if >=26.2 {
   ParticleOptions particle=new GeyserParticleOptions(ParticleTypes.GEYSER_PLUME,Rules.i("endrod_steam","height"));
   //?} else {
   /*ParticleOptions particle=ParticleTypes.CLOUD;
   *///?}
   level.sendParticles(particle,x,y,z,Rules.i("endrod_steam","count"),.03,.03,.03,.02);
  }
 }
 public static void infestation(ServerLevel level,BlockPos pos){
  int minimum=Rules.i("infested_swarm","minimum"),maximum=Math.max(minimum,Rules.i("infested_swarm","maximum"));int count=minimum+level.getRandom().nextInt(maximum-minimum+1);
  for(int i=0;i<count;i++){var fish=EntityTypes.SILVERFISH.create(level,EntitySpawnReason.TRIGGERED);if(fish!=null){fish.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,level.getRandom().nextFloat()*360,0);level.addFreshEntity(fish);}}
 }
}
