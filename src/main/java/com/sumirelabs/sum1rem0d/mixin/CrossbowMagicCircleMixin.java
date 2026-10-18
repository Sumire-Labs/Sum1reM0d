package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.CrossbowMagicCircle;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class CrossbowMagicCircleMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void sum1rem0d$drawCrossbowMagicCircle(CallbackInfo ci) { CrossbowMagicCircle.tick(); }
}
