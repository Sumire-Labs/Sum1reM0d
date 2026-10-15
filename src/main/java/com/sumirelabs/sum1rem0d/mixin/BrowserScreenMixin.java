package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.BrowserOverlays;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractContainerScreen.class)
public abstract class BrowserScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$click(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) { if (BrowserOverlays.click((Screen) (Object) this, event, doubled)) cir.setReturnValue(true); }
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$release(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) { if (BrowserOverlays.release((Screen) (Object) this, event)) cir.setReturnValue(true); }
    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$scroll(double x, double y, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) { if (BrowserOverlays.scroll((Screen) (Object) this, x, y, vertical)) cir.setReturnValue(true); }
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$drag(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) { if (BrowserOverlays.drag((Screen) (Object) this, event)) cir.setReturnValue(true); }
}
