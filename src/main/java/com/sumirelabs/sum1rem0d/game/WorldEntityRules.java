package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class WorldEntityRules {
 private record BurstBudget(long tick,int count){}
 private static final Map<ServerLevel,BurstBudget> BURSTS=new WeakHashMap<>();
 private static final Map<LivingEntity,Boolean> DEAD=new WeakHashMap<>();
 private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
 private WorldEntityRules(){}
 public static void tick(LivingEntity entity){
  if(!(entity.level() instanceof ServerLevel level)||!entity.isAlive())return;
  if(entity instanceof net.minecraft.world.entity.monster.Creeper creeper&&Rules.on("powered_creeper")&&!creeper.isIgnited()){
   for(var pos:net.minecraft.core.BlockPos.betweenClosed(creeper.getBoundingBox().inflate(.05))){
    if(!level.hasChunkAt(pos))continue;var wire=level.getBlockState(pos);
    boolean allowed=wire.getBlock() instanceof net.minecraft.world.level.block.RedstoneWireBlock||wire.getBlock() instanceof GlowWireBlock;
    if(wire.getBlock() instanceof GlowWireBlock dust&&!Rules.on(dust.feature()))allowed=false;
    if(allowed&&wire.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER)&&wire.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER)>=Rules.i("powered_creeper","minimum_signal")){creeper.ignite();break;}
   }
  }
  if(entity instanceof IronGolem){
   var attribute=entity.getAttribute(Attributes.MAX_HEALTH);if(attribute!=null){
    var state=WorldState.get(level.getServer());String id=entity.getUUID().toString();var originals=state.golemHealth();
    if(Rules.on("golem_upgrade")){
     if(!originals.containsKey(id)){originals.put(id,attribute.getBaseValue());state.save();}
     double target=Rules.n("golem_upgrade","health");if(attribute.getBaseValue()!=target){double ratio=entity.getHealth()/entity.getMaxHealth();attribute.setBaseValue(target);entity.setHealth((float)(entity.getMaxHealth()*ratio));}
    }else if(originals.containsKey(id)){double ratio=entity.getHealth()/entity.getMaxHealth();attribute.setBaseValue(originals.remove(id));entity.setHealth((float)(entity.getMaxHealth()*ratio));state.save();}
   }
  }
  if(entity instanceof Chicken chicken&&chicken.isBaby()&&Rules.on("chick_trample")){
   for(var player:level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,chicken.getBoundingBox().inflate(.08,.3,.08),p->!p.isSpectator()&&p.onGround())){
    double feet=player.getBoundingBox().minY;if(feet>=chicken.getY()-.2&&feet<=chicken.getY()+.4){
     level.sendParticles(ParticleTypes.POOF,chicken.getX(),chicken.getY()+.1,chicken.getZ(),6,.15,.02,.15,.01);
     chicken.hurtServer(level,player.damageSources().playerAttack(player),(float)Rules.n("chick_trample","damage"));break;
    }
   }
  }
 }
 public static boolean golemLoot(Mob mob,ServerLevel level){
  if(!(mob instanceof IronGolem)||!Rules.on("golem_upgrade"))return false;
  int count=Rules.i("golem_upgrade","iron_blocks");while(count>0){int part=Math.min(64,count);mob.spawnAtLocation(level,new ItemStack(Items.IRON_BLOCK,part));count-=part;}
  WorldState.get(level.getServer()).golemHealth().remove(mob.getUUID().toString());return true;
 }
 public static void died(LivingEntity entity,DamageSource damage){
  if(entity instanceof net.minecraft.server.level.ServerPlayer player)WorldMediaRules.died(player,damage);
  if(entity.level() instanceof ServerLevel deathLevel&&entity instanceof IronGolem)WorldState.get(deathLevel.getServer()).golemHealth().remove(entity.getUUID().toString());
  if(!(entity.level() instanceof ServerLevel level)||!(entity instanceof Mob)||!Rules.on("explosive_deaths")||!damage.is(DamageTypeTags.IS_EXPLOSION)||DEAD.put(entity,true)!=null)return;
  var budget=BURSTS.get(level);long tick=level.getGameTime();int count=budget!=null&&budget.tick()==tick?budget.count():0;
  int depth=DEPTH.get();if(depth>=Rules.i("explosive_deaths","maximum_depth")||count>=Rules.i("explosive_deaths","maximum_per_tick"))return;
  BURSTS.put(level,new BurstBudget(tick,count+1));DEPTH.set(depth+1);
  try{level.explode(entity,entity.getX(),entity.getY()+.2,entity.getZ(),(float)Rules.n("explosive_deaths","power"),Rules.b("explosive_deaths","break_blocks")?Level.ExplosionInteraction.MOB:Level.ExplosionInteraction.NONE);}
  finally{DEPTH.set(depth);}
 }
}
