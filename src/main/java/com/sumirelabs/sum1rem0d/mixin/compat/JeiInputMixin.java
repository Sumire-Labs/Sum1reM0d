package com.sumirelabs.sum1rem0d.mixin.compat;
import com.sumirelabs.sum1rem0d.client.ViewerBlocker;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets = "mezz.jei.gui.input.ClientInputHandler", remap = false)
public abstract class JeiInputMixin {
    @Inject(method = {"onKeyboardKeyPressedPre", "onKeyboardKeyPressedPost", "onKeyboardCharTypedPre", "onGuiMouseClicked", "onGuiMouseReleased", "onGuiMouseScroll", "onGuiMouseDragged"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void sum1rem0d$input(CallbackInfoReturnable<Boolean> cir) { if (ViewerBlocker.blocked("jei")) cir.setReturnValue(false); }
    @Inject(method = "onKeyboardCharTypedPost", at = @At("HEAD"), cancellable = true, require = 0)
    private void sum1rem0d$text(CallbackInfo ci) { if (ViewerBlocker.blocked("jei")) ci.cancel(); }
}
