package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.WorldEntityRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class WorldEntityRulesMixin {
 @Inject(method="tick",at=@At("TAIL"))private void sum1rem0d$tick(CallbackInfo ci){WorldEntityRules.tick((LivingEntity)(Object)this);}
 @Inject(method="die",at=@At("HEAD"))private void sum1rem0d$explosiveDeath(DamageSource source,CallbackInfo ci){WorldEntityRules.died((LivingEntity)(Object)this,source);}
}
