package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Leashable.class)
public interface UnbreakableLeashMixin {
    @Inject(method = "canBeLeashed", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$allLeashableTargets(CallbackInfoReturnable<Boolean> cir) {
        if (com.sumirelabs.sum1rem0d.game.WorldMobility.enabled("unbreakable_lead")) cir.setReturnValue(true);
    }

    @Inject(method = "dropLeash", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$preventSnapping(CallbackInfo ci) {
        if (!com.sumirelabs.sum1rem0d.game.WorldMobility.enabled("unbreakable_lead")) return;
        Leashable leashable = (Leashable) this;
        Entity holder = leashable.getLeashHolder();
        if (holder == null || !holder.isAlive()) return;
        double max = Math.max(0, Math.min(256, Rules.n("unbreakable_lead", "max_length")));
        if (max == 0 || leashable.leashDistanceTo(holder) <= max) ci.cancel();
    }
    @Inject(method = "removeLeash", at = @At("HEAD"))
    private void sum1rem0d$untrack(CallbackInfo ci) {
        Leashable leashable = (Leashable) this;
        Entity holder = leashable.getLeashHolder();
        if (holder != null) com.sumirelabs.sum1rem0d.game.WorldMobility.untrackLeash((Entity) this, holder);
    }
    @Inject(method = "leashSnapDistance", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$configuredLength(CallbackInfoReturnable<Double> cir) {
        if (com.sumirelabs.sum1rem0d.game.WorldMobility.enabled("unbreakable_lead")) {
            double max = Math.max(0, Math.min(256, Rules.n("unbreakable_lead", "max_length")));
            cir.setReturnValue(max == 0 ? Double.MAX_VALUE : max);
        }
    }
}
