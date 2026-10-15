package com.sumirelabs.sum1rem0d.mixin.compat;
import com.sumirelabs.sum1rem0d.client.ViewerBlocker;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.REIRuntimeImpl", remap = false)
public abstract class ReiRuntimeMixin {
    @Inject(method = "isOverlayVisible", at = @At("HEAD"), cancellable = true, require = 0)
    private void sum1rem0d$visible(CallbackInfoReturnable<Boolean> cir) { if (ViewerBlocker.blocked("roughlyenoughitems")) cir.setReturnValue(false); }
}
