package com.sumirelabs.sum1rem0d.game;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;

final class WorldSurvivalVerification {
 static void verify(MinecraftServer server,Consumer<String> passed)throws Exception{
  var config=Sum1reM0d.config();var before=config.snapshot();var level=server.overworld();var player=EconomyVerification.actor(server,level,"SumireSurvival");var state=WorldState.get(server);var checkpoint=state.checkpoint();
  try{
   var draft=before.deepCopy();var f=draft.getAsJsonObject("features");
   for(String id:List.of("path_speed","lodestone_travel","golem_upgrade","powered_creeper","bush_seeds"))f.getAsJsonObject(id).addProperty("enabled",true);
   config.update(draft,config.revision());
   var ground=new BlockPos(1980,240,40);ServerVerification.place(level,ground,Blocks.GRAVEL.defaultBlockState());player.setPos(ground.getX()+.5,ground.getY()+1,ground.getZ()+.5);
   double base=player.getAttributeValue(Attributes.MOVEMENT_SPEED);WorldSurvivalRules.tickPlayer(player,state,1);require(player.getAttributeValue(Attributes.MOVEMENT_SPEED)>base,"Gravel grants path speed");
   ServerVerification.place(level,ground,Blocks.STONE.defaultBlockState());WorldSurvivalRules.tickPlayer(player,state,2);require(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED)-base)<1e-8,"Leaving path removes only path modifier");
   var target=ground.offset(8,0,0);ServerVerification.place(level,target,Blocks.LODESTONE.defaultBlockState());ServerVerification.place(level,target.above(),Blocks.AIR.defaultBlockState());ServerVerification.place(level,target.above(2),Blocks.AIR.defaultBlockState());
   var compass=new ItemStack(Items.COMPASS);compass.set(DataComponents.LODESTONE_TRACKER,new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(),target)),true));
   require(LodestoneTravel.use(player,compass)==net.minecraft.world.InteractionResult.SUCCESS&&player.distanceToSqr(target.getX()+.5,target.getY()+1,target.getZ()+.5)<.01,"Lodestone compass safely teleports to existing target");
   require(LodestoneTravel.use(player,compass)==net.minecraft.world.InteractionResult.FAIL&&compass.getCount()==1,"Cooldown refuses duplicate travel without consuming compass");
   var golem=EntityTypes.IRON_GOLEM.create(level,EntitySpawnReason.TRIGGERED);double original=golem.getAttribute(Attributes.MAX_HEALTH).getBaseValue();WorldEntityRules.tick(golem);require(golem.getMaxHealth()==1024,"Native golem health is 1024");
   f.getAsJsonObject("golem_upgrade").addProperty("enabled",false);config.update(draft,config.revision());WorldEntityRules.tick(golem);require(golem.getAttribute(Attributes.MAX_HEALTH).getBaseValue()==original,"Golem OFF restores original base attribute");golem.discard();
   var wirePos=ground.offset(0,1,5);ServerVerification.place(level,wirePos.below(),Blocks.STONE.defaultBlockState());ServerVerification.place(level,wirePos,Blocks.REDSTONE_WIRE.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER,4));
   var creeper=EntityTypes.CREEPER.create(level,EntitySpawnReason.TRIGGERED);creeper.setPos(wirePos.getX()+.5,wirePos.getY(),wirePos.getZ()+.5);WorldEntityRules.tick(creeper);require(creeper.isIgnited(),"Powered wire contact ignites a native creeper");creeper.discard();
   var bush=Blocks.DEAD_BUSH.defaultBlockState();var drops=BlockGameplay.drops(bush,level,player,ItemStack.EMPTY,List.of());require(drops.size()==1&&!drops.getFirst().isEmpty(),"Bush seed selection uses actual registered seeds");
   f.getAsJsonObject("bush_seeds").addProperty("enabled",false);config.update(draft,config.revision());require(BlockGameplay.drops(bush,level,player,ItemStack.EMPTY,List.of()).isEmpty(),"Bush seeds Config OFF preserves native drop result");
   passed.accept("Native path modifier restoration, safe compass travel/cooldown, reversible golem health, powered creeper contact and configurable bush seeds");
   verifyCryingObsidian(server,passed);
   verifyStonecutterAndNetherHeat(server,passed);
   verifyNoonUndress(server,passed);
   verifyInfestedSwarm(server,passed);
   verifyExplosiveDeaths(server,passed);
   verifySulfurContact(server,passed);
   verifyPvPDeathMedia(server,passed);
  }finally{config.update(before,config.revision());state.restore(checkpoint);state.save();player.discard();}
 }

 private static JsonObject disabledFeatures(){var draft=Sum1reM0d.config().snapshot();for(var feature:draft.getAsJsonObject("features").entrySet())feature.getValue().getAsJsonObject().addProperty("enabled",false);return draft;}

 private static void verifyCryingObsidian(MinecraftServer server,Consumer<String> passed)throws Exception{
  var config=Sum1reM0d.config();var before=config.snapshot();var level=server.overworld();var draft=disabledFeatures();var cfg=draft.getAsJsonObject("features").getAsJsonObject("crying_obsidian_mix");cfg.addProperty("enabled",true);cfg.addProperty("flowing_only",false);config.update(draft,config.revision());
  try{
   var method=LiquidBlock.class.getDeclaredMethod("shouldSpreadLiquid",net.minecraft.world.level.Level.class,BlockPos.class,net.minecraft.world.level.block.state.BlockState.class);method.setAccessible(true);
   var sourcePos=new BlockPos(2160,240,20);var waterPos=sourcePos.east();placeLiquidFixture(level,sourcePos,Blocks.LAVA.defaultBlockState());placeLiquidFixture(level,waterPos,Blocks.WATER.defaultBlockState());
   require(level.getFluidState(sourcePos).isSource()&&level.getFluidState(waterPos).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER),
           "Crying-obsidian fixture must contain source lava and adjacent source water");
   method.invoke(Blocks.LAVA,level,sourcePos,Blocks.LAVA.defaultBlockState());require(level.getBlockState(sourcePos).is(Blocks.CRYING_OBSIDIAN),
           "Source lava meeting source water becomes crying obsidian (result="+net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(sourcePos).getBlock())+", feature="+Rules.on("crying_obsidian_mix")+")");
   cfg.addProperty("flowing_only",true);config.update(draft,config.revision());
   var flowingPos=sourcePos.east(4);var flowingWater=waterPos.east(4);placeLiquidFixture(level,flowingPos,Blocks.LAVA.defaultBlockState());placeLiquidFixture(level,flowingWater,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,1));
   method.invoke(Blocks.LAVA,level,flowingPos,Blocks.LAVA.defaultBlockState());require(level.getBlockState(flowingPos).is(Blocks.CRYING_OBSIDIAN),"Flowing water converts source lava when flowing-only mode is enabled");
   var sourceWaterPos=sourcePos.east(8);var sourceWater=waterPos.east(8);placeLiquidFixture(level,sourceWaterPos,Blocks.LAVA.defaultBlockState());placeLiquidFixture(level,sourceWater,Blocks.WATER.defaultBlockState());
   method.invoke(Blocks.LAVA,level,sourceWaterPos,Blocks.LAVA.defaultBlockState());require(level.getBlockState(sourceWaterPos).is(Blocks.OBSIDIAN),"Source water remains normal obsidian in flowing-only mode");
   cfg.addProperty("flowing_only",false);config.update(draft,config.revision());
   var flowingLavaPos=sourcePos.east(12);var flowingLavaWater=waterPos.east(12);var flowingLava=Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,1);placeLiquidFixture(level,flowingLavaPos,flowingLava);placeLiquidFixture(level,flowingLavaWater,Blocks.WATER.defaultBlockState());
   method.invoke(Blocks.LAVA,level,flowingLavaPos,flowingLava);require(level.getBlockState(flowingLavaPos).is(Blocks.COBBLESTONE),"Flowing lava still produces vanilla cobblestone");
   var basaltPos=sourcePos.east(16);ServerVerification.place(level,basaltPos.below(),Blocks.SOUL_SOIL.defaultBlockState());ServerVerification.place(level,basaltPos.west(),Blocks.BLUE_ICE.defaultBlockState());ServerVerification.place(level,basaltPos,Blocks.LAVA.defaultBlockState());
   method.invoke(Blocks.LAVA,level,basaltPos,Blocks.LAVA.defaultBlockState());require(level.getBlockState(basaltPos).is(Blocks.BASALT),"Soul-soil and blue-ice lava still produces vanilla basalt");
   cfg.addProperty("enabled",false);config.update(draft,config.revision());var offPos=sourcePos.east(20);placeLiquidFixture(level,offPos,Blocks.LAVA.defaultBlockState());placeLiquidFixture(level,offPos.east(),Blocks.WATER.defaultBlockState());
   method.invoke(Blocks.LAVA,level,offPos,Blocks.LAVA.defaultBlockState());require(level.getBlockState(offPos).is(Blocks.OBSIDIAN),"Crying-obsidian Config OFF restores vanilla obsidian");
   passed.accept("Native liquid spread converts source lava with water, honors flowing-only and preserves obsidian/cobblestone/basalt outcomes");
  }finally{config.update(before,config.revision());}
 }

 private static void placeLiquidFixture(ServerLevel level,BlockPos pos,BlockState state){ServerVerification.place(level,pos,Blocks.AIR.defaultBlockState());level.setBlock(pos,state,2);}

 private static void verifyStonecutterAndNetherHeat(MinecraftServer server,Consumer<String> passed)throws Exception{
  var config=Sum1reM0d.config();var before=config.snapshot();var world=server.overworld();var nether=server.getLevel(net.minecraft.world.level.Level.NETHER);require(nether!=null,"Native Nether dimension exists");var state=WorldState.get(server);
  var draft=disabledFeatures();var stone=draft.getAsJsonObject("features").getAsJsonObject("stonecutter_hazard");stone.addProperty("enabled",true);stone.addProperty("interval",1);stone.addProperty("damage",1000);config.update(draft,config.revision());
  var cutterPlayer=EconomyVerification.actor(server,world,"SumireStonecutterVerifier");var cutter=new BlockPos(2190,240,20);
  try{
   ServerVerification.place(world,cutter,Blocks.STONECUTTER.defaultBlockState());cutterPlayer.setPos(cutter.getX()+.5,cutter.getY()+1,cutter.getZ()+.5);cutterPlayer.setHealth(20);cutterPlayer.setInvulnerableTime(0);cutterPlayer.damageCooldownTime=0;
   WorldSurvivalRules.tickPlayer(cutterPlayer,state,1);require(!cutterPlayer.isAlive(),"The configured lethal stonecutter hazard kills on a native player tick");
  }finally{cutterPlayer.discard();}
  stone.addProperty("enabled",false);config.update(draft,config.revision());var offCutter=EconomyVerification.actor(server,world,"SumireStonecutterOffVerifier");
  try{offCutter.setPos(cutter.getX()+.5,cutter.getY()+1,cutter.getZ()+.5);offCutter.setHealth(20);WorldSurvivalRules.tickPlayer(offCutter,state,2);require(offCutter.getHealth()==20,"Stonecutter Config OFF prevents its damage");}finally{offCutter.discard();}
  var heat=draft.getAsJsonObject("features").getAsJsonObject("nether_heat");heat.addProperty("enabled",true);heat.addProperty("interval",1);heat.addProperty("damage",3);heat.addProperty("required_level",1);heat.addProperty("allow_fire_resistance",false);config.update(draft,config.revision());
  var netherPlayer=EconomyVerification.actor(server,nether,"SumireNetherHeatVerifier");var boots=new ItemStack(Items.DIAMOND_BOOTS);
  try{
   netherPlayer.setPos(2200.5,240,20.5);netherPlayer.getInventory().clearContent();netherPlayer.setHealth(20);netherPlayer.setInvulnerableTime(0);netherPlayer.damageCooldownTime=0;
   WorldSurvivalRules.tickPlayer(netherPlayer,state,10);require(netherPlayer.getHealth()==17,"Unprotected survival player takes Nether heat damage");
   boots.enchant(nether.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FIRE_PROTECTION),1);netherPlayer.setItemSlot(EquipmentSlot.FEET,boots);netherPlayer.setHealth(20);netherPlayer.setInvulnerableTime(0);netherPlayer.damageCooldownTime=0;
   WorldSurvivalRules.tickPlayer(netherPlayer,state,11);require(netherPlayer.getHealth()==20,"Required fire protection enchantment prevents Nether heat damage");
   heat.addProperty("required_level",2);config.update(draft,config.revision());netherPlayer.setHealth(20);netherPlayer.setInvulnerableTime(0);netherPlayer.damageCooldownTime=0;WorldSurvivalRules.tickPlayer(netherPlayer,state,12);
   require(netherPlayer.getHealth()==17,"Fire protection below configured level does not protect");
   heat.addProperty("allow_fire_resistance",true);config.update(draft,config.revision());netherPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,200));netherPlayer.setHealth(20);netherPlayer.setInvulnerableTime(0);netherPlayer.damageCooldownTime=0;
   WorldSurvivalRules.tickPlayer(netherPlayer,state,13);require(netherPlayer.getHealth()==20,"Configured fire resistance potion protection works independently");
   heat.addProperty("enabled",false);config.update(draft,config.revision());netherPlayer.setHealth(20);netherPlayer.setInvulnerableTime(0);netherPlayer.damageCooldownTime=0;WorldSurvivalRules.tickPlayer(netherPlayer,state,14);
   require(netherPlayer.getHealth()==20,"Nether heat Config OFF prevents damage");
   passed.accept("Native player receives lethal stonecutter damage, Nether heat checks fire-protection level and potion option, and both Config OFF paths");
  }finally{netherPlayer.discard();config.update(before,config.revision());}
 }

 private static void verifyNoonUndress(MinecraftServer server,Consumer<String> passed)throws Exception{
  var config=Sum1reM0d.config();var before=config.snapshot();var level=server.overworld();var state=WorldState.get(server);var player=EconomyVerification.actor(server,level,"SumireNoonUndressVerifier");var data=state.player(player);var originalSurvival=data.survival.deepCopy();
  var draft=disabledFeatures();var cfg=draft.getAsJsonObject("features").getAsJsonObject("noon_undress");cfg.addProperty("enabled",true);cfg.addProperty("time",0);cfg.addProperty("drop_if_full",true);config.update(draft,config.revision());
  try{
   long clock=player.level().getOverworldClockTime(),day=Math.floorDiv(clock,24000),trigger=day*24000L;data.survival.addProperty("clock",trigger-100);data.survival.addProperty("undressedDay",day-1);player.setPos(2210.5,245,20.5);player.getInventory().clearContent();
   for(int slot=0;slot<player.getInventory().getContainerSize();slot++)player.getInventory().setItem(slot,new ItemStack(Items.STONE,64));
   var armor=List.of(Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS);var slots=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
   for(int i=0;i<slots.size();i++)player.setItemSlot(slots.get(i),new ItemStack(armor.get(i)));
   int beforeDrops=countArmorDrops(level,player,armor);WorldSurvivalRules.tickPlayer(player,state,21);
   require(slots.stream().allMatch(slot->player.getItemBySlot(slot).isEmpty()),"Catch-up across noon removes equipped armor into full-inventory fallback");
   require(countArmorDrops(level,player,armor)-beforeDrops==4,"Full inventory drop policy releases all four armor pieces exactly once");
   for(int i=0;i<slots.size();i++)player.setItemSlot(slots.get(i),new ItemStack(armor.get(i)));data.survival.addProperty("clock",trigger-100);WorldSurvivalRules.tickPlayer(player,state,22);
   require(slots.stream().allMatch(slot->!player.getItemBySlot(slot).isEmpty())&&countArmorDrops(level,player,armor)-beforeDrops==4,"Noon undress triggers at most once per day after a catch-up crossing");
   cfg.addProperty("drop_if_full",false);config.update(draft,config.revision());data.survival.addProperty("clock",trigger-100);data.survival.addProperty("undressedDay",day-1);WorldSurvivalRules.tickPlayer(player,state,23);
   require(slots.stream().allMatch(slot->!player.getItemBySlot(slot).isEmpty())&&countArmorDrops(level,player,armor)-beforeDrops==4,"When full and dropping is disabled, armor remains equipped without loss");
   passed.accept("Noon armor catch-up, full inventory drop/retain policy and one trigger per world day");
  }finally{data.survival=originalSurvival;player.discard();config.update(before,config.revision());}
 }

 private static int countArmorDrops(ServerLevel level,Entity owner,List<Item> armor){int count=0;for(var entity:ServerVerification.spawnedIn(level))if(entity instanceof net.minecraft.world.entity.item.ItemEntity item&&armor.contains(item.getItem().getItem())&&item.distanceToSqr(owner)<100)count+=item.getItem().getCount();return count;}

 private static void verifyInfestedSwarm(MinecraftServer server,Consumer<String> passed)throws Exception{
  var config=Sum1reM0d.config();var before=config.snapshot();var level=server.overworld();var draft=disabledFeatures();var cfg=draft.getAsJsonObject("features").getAsJsonObject("infested_swarm");cfg.addProperty("enabled",true);cfg.addProperty("minimum",1);cfg.addProperty("maximum",1);config.update(draft,config.revision());
  try{
   var spawn=Class.forName("net.minecraft.world.level.block.InfestedBlock").getDeclaredMethod("spawnInfestation",ServerLevel.class,BlockPos.class);spawn.setAccessible(true);var infested=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("infested_stone"));
   int one=invokeInfestation(spawn,infested,level,new BlockPos(2230,245,20));require(one==1,"Native InfestedBlock entry honors configured one-silverfish minimum");
   cfg.addProperty("minimum",50);cfg.addProperty("maximum",50);config.update(draft,config.revision());int fifty=invokeInfestation(spawn,infested,level,new BlockPos(2240,245,20));require(fifty==50,"Native InfestedBlock entry honors configured 50-silverfish upper endpoint");
   passed.accept("Native InfestedBlock spawn hook emits the configured swarm count from 1 through 50");
  }finally{config.update(before,config.revision());}
 }

 private static int invokeInfestation(java.lang.reflect.Method spawn,Object block,ServerLevel level,BlockPos pos)throws Exception{
  level.getChunkAt(pos);int before=(int)ServerVerification.spawnedIn(level).stream().filter(e->e instanceof net.minecraft.world.entity.monster.Silverfish&&e.position().distanceToSqr(pos.getX()+.5,pos.getY(),pos.getZ()+.5)<16).count();spawn.invoke(block,level,pos);
  return (int)ServerVerification.spawnedIn(level).stream().filter(e->e instanceof net.minecraft.world.entity.monster.Silverfish&&e.position().distanceToSqr(pos.getX()+.5,pos.getY(),pos.getZ()+.5)<16).count()-before;
 }

 private static void verifyExplosiveDeaths(MinecraftServer server,Consumer<String> passed)throws Exception{
  var config=Sum1reM0d.config();var before=config.snapshot();var level=server.overworld();long gameTime=server.getWorldData().overworldData().getGameTime();var draft=disabledFeatures();var cfg=draft.getAsJsonObject("features").getAsJsonObject("explosive_deaths");cfg.addProperty("enabled",true);cfg.addProperty("maximum_depth",1);cfg.addProperty("maximum_per_tick",16);cfg.addProperty("power",8);cfg.addProperty("break_blocks",false);config.update(draft,config.revision());
  try{
   server.getWorldData().overworldData().setGameTime(gameTime+1);var center=new BlockPos(2250,245,20);level.getChunkAt(center);var mobs=new ArrayList<LivingEntity>();
   for(int i=0;i<8;i++){var mob=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);require(mob!=null,"Zombie fixture for explosive death chain");mob.setPos(center.getX()+.5+(i%4)*.7,center.getY(),center.getZ()+.5+(i/4)*.7);mob.setHealth(1);require(level.addFreshEntity(mob),"Explosive-death fixture joins native level");mobs.add(mob);}
   int baseline=ServerVerification.explosions().size();var first=mobs.getFirst();require(first.hurtServer(level,level.damageSources().explosion(first,null),1000)&&first.isDeadOrDying(),"Initial explosive damage kills the chain source");
   int explosions=ServerVerification.explosions().size()-baseline;require(explosions==1,"Recursive explosion death chain stops at configured depth one: " + explosions);
   server.getWorldData().overworldData().setGameTime(gameTime+2);cfg.addProperty("maximum_depth",16);cfg.addProperty("maximum_per_tick",3);config.update(draft,config.revision());baseline=ServerVerification.explosions().size();
   for(int i=0;i<5;i++){var mob=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);require(mob!=null,"Mob fixture for per-tick explosion cap");var isolated=center.offset(12+i*20,0,0);level.getChunkAt(isolated);mob.setPos(isolated.getX()+.5,isolated.getY(),isolated.getZ()+.5);mob.setHealth(1);require(level.addFreshEntity(mob),"Per-tick cap fixture joins native level");require(mob.hurtServer(level,level.damageSources().explosion(mob,null),1000),"Per-tick cap fixture accepts explosion damage");}
   explosions=ServerVerification.explosions().size()-baseline;require(explosions==3,"Five same-tick explosive deaths are capped at exactly three blasts: " + explosions);
   server.getWorldData().overworldData().setGameTime(gameTime+3);cfg.addProperty("enabled",false);config.update(draft,config.revision());var off=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);require(off!=null,"Config OFF explosion fixture");off.setPos(center.getX()+40,center.getY(),center.getZ());off.setHealth(1);level.addFreshEntity(off);baseline=ServerVerification.explosions().size();require(off.hurtServer(level,level.damageSources().explosion(off,null),1000),"Config OFF fixture accepts native explosion damage");require(ServerVerification.explosions().size()==baseline,"Explosive death Config OFF suppresses mod-created blast");
   passed.accept("Explosion-caused mob death recursion depth, exact per-tick cap and Config OFF behavior");
  }finally{server.getWorldData().overworldData().setGameTime(gameTime);config.update(before,config.revision());}
 }

 private static void verifySulfurContact(MinecraftServer server,Consumer<String> passed)throws Exception{
  var config=Sum1reM0d.config();var before=config.snapshot();var level=server.overworld();var state=WorldState.get(server);var player=EconomyVerification.actor(server,level,"SumireSulfurContactVerifier");var data=state.player(player);var previous=data.survival.deepCopy();var gameTime=server.getWorldData().overworldData().getGameTime();
  var draft=disabledFeatures();var sulfur=draft.getAsJsonObject("features").getAsJsonObject("sulfur_dirt");sulfur.addProperty("enabled",true);sulfur.addProperty("water",true);sulfur.addProperty("geysers",true);sulfur.addProperty("interval",5);sulfur.addProperty("amount",11);sulfur.addProperty("geyser_amount",17);var clean=draft.getAsJsonObject("features").getAsJsonObject("cleanliness");clean.addProperty("enabled",true);clean.addProperty("decay_per_minute",0);clean.addProperty("water_per_second",7);config.update(draft,config.revision());
  try{
   level.addNewPlayer(player);server.getWorldData().overworldData().setGameTime(gameTime+100);long tick=((level.getGameTime()+19)/20)*20;server.getWorldData().overworldData().setGameTime(tick);var water=new BlockPos(2270,245,20);ServerVerification.place(level,water,Blocks.WATER.defaultBlockState());player.setPos(water.getX()+.5,water.getY(),water.getZ()+.5);refreshFluidInteraction(player);require(player.isInWater(),"Noxious-water fixture exposes native fluid contact");data.cleanliness=50;boolean nativeWater=invokeNoxiousContact(player);
   require(data.cleanliness==39&&!WorldSurvivalRules.cleanWater(player,state),"Noxious water applies configured cleanliness loss and temporarily suppresses clean-water recovery");invokeNoxiousContact(player);require(data.cleanliness==39,"Repeated same-tick noxious-water contact respects the interval");server.getWorldData().overworldData().setGameTime(tick+20);Gameplay.tickPlayer(player,state,tick+20);require(data.cleanliness==39,"Cleanliness does not recover at a 20-tick update while noxious-water suppression is active");
   server.getWorldData().overworldData().setGameTime(tick+4);invokeNoxiousContact(player);require(data.cleanliness==39,"Noxious-water contact is throttled until the configured interval expires");server.getWorldData().overworldData().setGameTime(tick+5);invokeNoxiousContact(player);require(data.cleanliness==28,"Noxious-water contact applies loss again at the interval boundary");
   long resumeTick=tick+40;server.getWorldData().overworldData().setGameTime(resumeTick);boolean resumedEnabled=Rules.on("cleanliness"),resumedWater=player.isInWater(),resumedEligible=Gameplay.eligible(player),resumedClean=WorldSurvivalRules.cleanWater(player,state),sameRecord=data==state.player(player);double gain=Rules.n("cleanliness","water_per_second");
   require(resumeTick%20==0&&level.getGameTime()==resumeTick&&resumedEnabled&&resumedWater&&resumedEligible&&resumedClean&&sameRecord&&data.cleanliness==28,
           "Recovery fixture prerequisites: resumeTick="+resumeTick+", actualTime="+level.getGameTime()+", cleanEnabled="+resumedEnabled+", water="+resumedWater+", eligible="+resumedEligible+", cleanWater="+resumedClean+", cleanliness="+data.cleanliness+", deadline="+data.survival.get("sulfurWaterUntil")+", sameRecord="+sameRecord);
   Gameplay.tickPlayer(player,state,resumeTick);require(data.cleanliness==28+gain,"Clean-water recovery resumes at the next 20-tick update after the most recent suppression window; cleanliness="+data.cleanliness+", expected="+(28+gain)+", inWater="+player.isInWater()+", eligible="+Gameplay.eligible(player)+", enabled="+Rules.on("cleanliness")+", gain="+Rules.n("cleanliness","water_per_second")+", clean="+WorldSurvivalRules.cleanWater(player,state)+", deadline="+data.survival.get("sulfurWaterUntil")+", gameTime="+level.getGameTime()+", recordMatches="+(data==state.player(player)));
   data.survival.remove("sulfurWaterUntil");data.survival.remove("geyserDirtyAt");data.cleanliness=50;var geyser=new BlockPos(2280,245,20);player.setPos(geyser.getX()+.5,geyser.getY()+1,geyser.getZ()+.5);player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);boolean nativeGeyser=invokeGeyserLaunch(level,geyser,player);require(data.cleanliness==33,"Geyser contact applies its independent cleanliness loss");if(nativeGeyser)require(player.getDeltaMovement().y>0,"Native geyser launch applies upward movement");else require(player.getDeltaMovement().y==0,"Native geyser entity is unavailable on this version; helper fallback does not claim a native launch");player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);invokeGeyserLaunch(level,geyser,player);require(data.cleanliness==33,"Repeated geyser contact respects its own rate limit");
   sulfur.addProperty("enabled",false);config.update(draft,config.revision());data.cleanliness=50;WorldSurvivalRules.sulfurContact(player,true);require(data.cleanliness==50&&WorldSurvivalRules.cleanWater(player,state),"Sulfur Config OFF disables both dirtying and clean-water suppression");
   passed.accept("Noxious-water and geyser contact amounts, independent interval boundaries, cleanliness recovery suppression and Config OFF ("+(nativeWater?"native water hook":"native sulfur entity unavailable; water helper fallback")+", "+(nativeGeyser?"native geyser launch":"native geyser unavailable; contact helper fallback")+")");
  }finally{data.survival=previous;player.discard();server.getWorldData().overworldData().setGameTime(gameTime);config.update(before,config.revision());}
 }

 private static void refreshFluidInteraction(Entity entity)throws Exception{var update=Entity.class.getDeclaredMethod("updateFluidInteraction");update.setAccessible(true);update.invoke(entity);}

 private static boolean invokeNoxiousContact(ServerPlayer player)throws Exception{
  try{var type=Class.forName("net.minecraft.world.level.block.entity.PotentSulfurBlockEntity");var apply=type.getDeclaredMethod("applyNauseaEffect",LivingEntity.class);apply.setAccessible(true);apply.invoke(null,player);return true;}
  catch(ClassNotFoundException unavailableOnThisVersion){WorldSurvivalRules.sulfurContact(player,false);return false;}
 }

 private static boolean invokeGeyserLaunch(ServerLevel level,BlockPos pos,ServerPlayer player)throws Exception{
  Class<?> type;
  try{type=Class.forName("net.minecraft.world.level.block.entity.PotentSulfurBlockEntity");}
  catch(ClassNotFoundException unavailableOnThisVersion){WorldSurvivalRules.sulfurContact(player,true);return false;}
  var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("potent_sulfur"));
  if(block==Blocks.AIR){WorldSurvivalRules.sulfurContact(player,true);return false;}
  ServerVerification.place(level,pos,block.defaultBlockState());var blockEntity=level.getBlockEntity(pos);require(blockEntity!=null&&type.isInstance(blockEntity),"Potent sulfur placement creates its native block entity");
  var launch=type.getDeclaredMethod("lambda$static$5",net.minecraft.world.level.Level.class,BlockPos.class,net.minecraft.world.level.block.state.BlockState.class,type);launch.setAccessible(true);launch.invoke(null,level,pos,level.getBlockState(pos),blockEntity);return true;
 }

 private static void verifyPvPDeathMedia(MinecraftServer server,Consumer<String> passed)throws Exception{
  var config=Sum1reM0d.config();var before=config.snapshot();var level=server.overworld();var victim=EconomyVerification.actor(server,level,"SumireDeathMediaVictim");var killer=EconomyVerification.actor(server,level,"SumireDeathMediaKiller");var draft=disabledFeatures();var cfg=draft.getAsJsonObject("features").getAsJsonObject("pvp_death_media");cfg.addProperty("enabled",false);config.update(draft,config.revision());
  try{
   var state=new JsonObject();WorldMediaRules.died(victim,victim.damageSources().playerAttack(killer));WorldMediaRules.append(victim,state);require(state.get("deathMediaSerial").getAsLong()==0,"PvP death media defaults OFF and ignores player deaths");
   victim.setHealth(20);WorldMediaRules.tick(victim,1);cfg.addProperty("enabled",true);cfg.addProperty("media_source","https://example.invalid/verification.mp3");config.update(draft,config.revision());victim.setHealth(0);WorldMediaRules.died(victim,victim.damageSources().playerAttack(killer));WorldMediaRules.died(victim,victim.damageSources().playerAttack(killer));WorldMediaRules.append(victim,state);
   require(state.get("deathMediaSerial").getAsLong()==1&&state.get("deathMediaSource").getAsString().equals("https://example.invalid/verification.mp3"),"An enabled PvP death advances one serial and duplicate death dispatch is suppressed");
   victim.setHealth(20);WorldMediaRules.tick(victim,1);victim.setHealth(0);WorldMediaRules.died(victim,victim.damageSources().playerAttack(victim));WorldMediaRules.append(victim,state);require(state.get("deathMediaSerial").getAsLong()==1,"Self death does not trigger PvP media");
   victim.setHealth(20);WorldMediaRules.tick(victim,2);victim.setHealth(0);WorldMediaRules.died(victim,victim.damageSources().generic());WorldMediaRules.append(victim,state);require(state.get("deathMediaSerial").getAsLong()==1,"Non-player death does not trigger PvP media");
   var replacement=EconomyVerification.actor(server,level,"SumireMediaRespawn");
   try{
    replacement.restoreFrom(victim,false);WorldMediaRules.append(replacement,state);
    require(state.get("deathMediaSerial").getAsLong()==1,"Native respawn keeps the existing session media serial");
    WorldMediaRules.died(replacement,replacement.damageSources().playerAttack(killer));WorldMediaRules.append(replacement,state);
    require(state.get("deathMediaSerial").getAsLong()==2,"First PvP death after respawn advances the existing serial without replay");
   }finally{replacement.discard();}
   passed.accept("PvP death media respects default OFF, increments once per PvP death, and preserves serials across native respawn without duplicate/self/non-player playback");
  }finally{config.update(before,config.revision());victim.discard();killer.discard();}
 }
}
