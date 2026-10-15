package com.sumirelabs.sum1rem0d.mixin.compat;

import com.sumirelabs.sum1rem0d.client.ViewerBlocker;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** EMI dispatches text input straight to its search widget, outside the overlay manager. */
@Pseudo
@Mixin(targets = "dev.emi.emi.screen.widget.EmiSearchWidget", remap = false)
public abstract class EmiSearchMixin {
    @Inject(method = "isFocused", at = @At("HEAD"), cancellable = true, require = 0)
    private void sum1rem0d$focus(CallbackInfoReturnable<Boolean> cir) {
        if (ViewerBlocker.blocked("emi")) {
            ((EditBox) (Object) this).setFocused(false);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setFocused", at = @At("HEAD"), cancellable = true, require = 0)
    private void sum1rem0d$preventFocus(boolean focused, CallbackInfo ci) {
        if (focused && ViewerBlocker.blocked("emi")) {
            ((EditBox) (Object) this).setFocused(false);
            ci.cancel();
        }
    }
}
