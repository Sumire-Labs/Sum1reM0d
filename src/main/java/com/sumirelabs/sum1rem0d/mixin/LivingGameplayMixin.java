package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Gameplay;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class LivingGameplayMixin {
    @Unique private float sum1rem0d$healthBefore;
    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void sum1rem0d$beforeDamage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        sum1rem0d$healthBefore = ((LivingEntity) (Object) this).getHealth();
    }
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void sum1rem0d$afterDamage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player && player.getHealth() < sum1rem0d$healthBefore) Gameplay.damaged(player);
    }
    @Inject(method = "die", at = @At("HEAD"))
    private void sum1rem0d$death(DamageSource source, CallbackInfo ci) { Gameplay.sheepKilled((LivingEntity) (Object) this, source); }
}
