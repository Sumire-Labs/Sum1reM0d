package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBullet.class)
public abstract class FastShulkerBulletMixin {
    @Unique private Vec3 sum1rem0d$unscaled;
    @Inject(method = "tick", at = @At("HEAD"))
    private void sum1rem0d$clientSpeed(CallbackInfo ci) {
        var bullet = (ShulkerBullet) (Object) this;
        if (!bullet.level().isClientSide() || !Rules.on("fast_shulker_bullets")) return;
        sum1rem0d$unscaled = bullet.getDeltaMovement();
        bullet.setDeltaMovement(sum1rem0d$unscaled.scale(Rules.n("fast_shulker_bullets", "speed_multiplier")));
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getHitResultOnMoveVector(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/world/phys/HitResult;"))
    private void sum1rem0d$speed(CallbackInfo ci) {
        var bullet = (ShulkerBullet) (Object) this;
        if (!Rules.on("fast_shulker_bullets")) return;
        sum1rem0d$unscaled = bullet.getDeltaMovement();
        bullet.setDeltaMovement(sum1rem0d$unscaled.scale(Rules.n("fast_shulker_bullets", "speed_multiplier")));
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void sum1rem0d$restoreHoming(CallbackInfo ci) {
        if (sum1rem0d$unscaled == null) return;
        ((ShulkerBullet) (Object) this).setDeltaMovement(sum1rem0d$unscaled);
        sum1rem0d$unscaled = null;
    }
}
