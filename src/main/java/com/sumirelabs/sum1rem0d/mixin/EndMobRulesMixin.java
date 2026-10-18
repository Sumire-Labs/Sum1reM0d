package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.EndMobRules;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class EndMobRulesMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void sum1rem0d$laser(CallbackInfo ci) { EndMobRules.tick((Mob) (Object) this); }
}
