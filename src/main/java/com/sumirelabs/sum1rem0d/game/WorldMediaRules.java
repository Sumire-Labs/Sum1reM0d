package com.sumirelabs.sum1rem0d.game;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.damagesource.DamageSource;

/** Session-only serials prevent repeated playback when a state packet is resent. */
public final class WorldMediaRules {
 private static final class State {String location="";long adSerial,deathSerial,nextAd;int adTicks;String source="";boolean dead;}
 private static final Map<ServerPlayer,State> STATES=new WeakHashMap<>();
 private static State state(ServerPlayer p){return STATES.computeIfAbsent(p,key->new State());}
 private WorldMediaRules(){}
 public static void copySession(ServerPlayer previous,ServerPlayer replacement){
  var data=STATES.remove(previous);
  if(data!=null){data.dead=false;STATES.put(replacement,data);}
 }
 public static void tick(ServerPlayer player,long tick){
  var data=state(player);if(player.isAlive())data.dead=false;if(tick%20!=0)return;
  if(!Rules.on("structure_ads")||player.isSpectator()||!player.isAlive()){data.location="";return;}
  var level=player.level();String location="";var pos=player.blockPosition();
  for(String id:Rules.list("structure_ads","structures")){
   var structure=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOptional(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,Rules.id(id)));
   if(structure.isPresent()){
    //? if >=26.3 {
    var start=level.structureManager().getStructureWithPieceAt(pos.getX(),pos.getY(),pos.getZ(),structure.get());
    //?} else {
    /*var start=level.structureManager().getStructureWithPieceAt(pos,structure.get());
    *///?}
    if(start.isValid()){location=id+":"+start.getChunkPos();break;}
   }
  }
  if(location.isEmpty()&&Rules.b("structure_ads","spawners")){
   int radius=Rules.i("structure_ads","spawner_radius");
   for(var near:BlockPos.betweenClosed(pos.offset(-radius,-radius,-radius),pos.offset(radius,radius,radius)))if(level.hasChunkAt(near)&&level.getBlockState(near).is(Blocks.SPAWNER)){location="spawner:"+near.asLong();break;}
  }
  if(!location.isEmpty()&&!location.equals(data.location)&&tick>=data.nextAd){data.adSerial++;data.adTicks=Rules.i("structure_ads","duration_ticks");data.nextAd=tick+Rules.i("structure_ads","cooldown_ticks");Gameplay.sendState(player);}
  data.location=location;
 }
 public static void died(ServerPlayer player,DamageSource damage){
  var data=state(player);if(data.dead)return;data.dead=true;
  if(!Rules.on("pvp_death_media")||!(damage.getEntity() instanceof ServerPlayer killer)||killer==player)return;
  String source=Rules.s("pvp_death_media","media_source").trim();
  data.deathSerial++;data.source=source;Gameplay.sendState(player);
 }
 public static void append(ServerPlayer player,JsonObject json){var d=state(player);json.addProperty("worldAdSerial",d.adSerial);json.addProperty("worldAdTicks",d.adTicks);json.addProperty("deathMediaSerial",d.deathSerial);json.addProperty("deathMediaSource",d.source);}
}
