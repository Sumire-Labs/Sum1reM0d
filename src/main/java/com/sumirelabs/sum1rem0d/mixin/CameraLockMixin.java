package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.ClientRuntime;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
public abstract class CameraLockMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$turn(double yaw, double pitch, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer && ClientRuntime.cameraLocked()) ci.cancel();
    }
}
