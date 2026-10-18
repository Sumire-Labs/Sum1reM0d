package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.WindChargeBlackHole;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class WindChargeBlackHoleMixin {
    @Inject(method = "onHit", at = @At("HEAD"))
    private void sum1rem0d$windChargeField(HitResult hit, CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;
        if (BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()).toString().equals("minecraft:wind_charge"))
            WindChargeBlackHole.impact(projectile);
    }
}
