package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.StumbleStrikeRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class StumbleStrikeMixin {
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void sum1rem0d$stumbleFollowup(ServerLevel level, DamageSource source, float damage,
            CallbackInfoReturnable<Boolean> cir) {
        StumbleStrikeRules.afterDamage((LivingEntity) (Object) this, level, source, cir.getReturnValueZ());
    }
}
