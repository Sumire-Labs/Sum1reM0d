package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.CombatEnchantRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rebukes qualifying attackers only after the wearer actually takes server damage. */
@Mixin(LivingEntity.class)
public abstract class ChestplateKnockbackMixin {
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void sum1rem0d$counterAttack(ServerLevel level, DamageSource source, float damage,
            CallbackInfoReturnable<Boolean> callback) {
        CombatEnchantRules.afterDamage((LivingEntity) (Object) this, level, source, damage, callback.getReturnValueZ());
    }
}
