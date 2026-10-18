package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Mob.class)
public abstract class NaturalFlipMixin implements NaturalFlip {
 //? if fabric {
 @Unique private static final EntityDataAccessor<Boolean> sum1rem0d$FLIPPED=SynchedEntityData.defineId(Mob.class,EntityDataSerializers.BOOLEAN);
 @Inject(method="defineSynchedData",at=@At("TAIL")) private void sum1rem0d$define(SynchedEntityData.Builder builder,CallbackInfo ci) { builder.define(sum1rem0d$FLIPPED,false); }
 //?}
 public boolean sum1rem0d$isFlipped() {
  //? if fabric {
  return ((Mob)(Object)this).getEntityData().get(sum1rem0d$FLIPPED);
  //?} elif neoforge {
  /*return NaturalFlipData.get((Mob)(Object)this);
  *///?}
 }
 @Unique private void sum1rem0d$setFlip(boolean value) {
  //? if fabric {
  ((Mob)(Object)this).getEntityData().set(sum1rem0d$FLIPPED,value);
  //?} elif neoforge {
  /*NaturalFlipData.set((Mob)(Object)this,value);
  *///?}
 }
 @Inject(method="finalizeSpawn",at=@At("RETURN")) private void sum1rem0d$spawn(ServerLevelAccessor level,DifficultyInstance difficulty,EntitySpawnReason reason,SpawnGroupData group,CallbackInfoReturnable<SpawnGroupData> cir) {
  var mob=(Mob)(Object)this;
  if (mob instanceof net.minecraft.world.entity.monster.Shulker && Rules.on("random_shulker_colors") && Rules.roll(mob.getRandom(), "random_shulker_colors")) ((ShulkerVariantAccess) mob).sum1rem0d$setVariant(java.util.Optional.of(net.minecraft.world.item.DyeColor.values()[mob.getRandom().nextInt(16)]));
  if(Rules.on("dinnerbone_mobs") && (reason==EntitySpawnReason.NATURAL || reason==EntitySpawnReason.CHUNK_GENERATION) && !mob.hasCustomName() && !(mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) && !(mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss)) sum1rem0d$setFlip(Rules.roll(mob.getRandom(),"dinnerbone_mobs"));
 }
 @Inject(method="addAdditionalSaveData",at=@At("TAIL")) private void sum1rem0d$save(ValueOutput output,CallbackInfo ci) { output.putBoolean("sum1rem0d:natural_flip",sum1rem0d$isFlipped()); }
 @Inject(method="readAdditionalSaveData",at=@At("TAIL")) private void sum1rem0d$load(ValueInput input,CallbackInfo ci) { sum1rem0d$setFlip(input.getBooleanOr("sum1rem0d:natural_flip",false)); }
}
