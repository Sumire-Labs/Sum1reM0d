package com.sumirelabs.sum1rem0d.mixin.compat;
import com.sumirelabs.sum1rem0d.client.ViewerBlocker;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets = "dev.emi.emi.screen.EmiScreenManager", remap = false)
public abstract class EmiMixin {
    @Inject(method = "isDisabled", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sum1rem0d$disabled(CallbackInfoReturnable<Boolean> cir) { if (ViewerBlocker.blocked("emi")) cir.setReturnValue(true); }
    @Inject(method = {"render", "drawBackground", "drawForeground"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void sum1rem0d$render(CallbackInfo ci) { if (ViewerBlocker.blocked("emi")) ci.cancel(); }
    @Inject(method = {"mouseClicked", "mouseReleased", "mouseScrolled", "keyPressed", "keyReleased", "charTyped"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void sum1rem0d$input(CallbackInfoReturnable<Boolean> cir) { if (ViewerBlocker.blocked("emi")) cir.setReturnValue(false); }
}
