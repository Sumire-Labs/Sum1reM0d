package com.sumirelabs.sum1rem0d.mixin.compat;
import com.sumirelabs.sum1rem0d.client.ViewerBlocker;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl", remap = false)
public abstract class ReiMixin {
    @Inject(method = {"render", "lateRender", "renderWidgets", "renderTooltip"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void sum1rem0d$render(CallbackInfo ci) { if (ViewerBlocker.blocked("roughlyenoughitems")) ci.cancel(); }
    @Inject(method = {"mouseClicked", "mouseReleased", "mouseDragged", "mouseScrolled", "keyPressed", "keyReleased", "charTyped", "isInside"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void sum1rem0d$input(CallbackInfoReturnable<Boolean> cir) { if (ViewerBlocker.blocked("roughlyenoughitems")) cir.setReturnValue(false); }
    @Inject(method = "getFocused", at = @At("HEAD"), cancellable = true, require = 0)
    private void sum1rem0d$focus(CallbackInfoReturnable<net.minecraft.client.gui.components.events.GuiEventListener> cir) { if (ViewerBlocker.blocked("roughlyenoughitems")) cir.setReturnValue(null); }
}
