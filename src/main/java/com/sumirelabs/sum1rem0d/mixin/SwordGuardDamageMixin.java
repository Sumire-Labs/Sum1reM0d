package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.SwordGuard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(LivingEntity.class)
public abstract class SwordGuardDamageMixin {
 @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true)
 private float sum1rem0d$damage(float damage,ServerLevel level,DamageSource source,float original) {return SwordGuard.damage((LivingEntity)(Object)this,level,source,damage);}
}
