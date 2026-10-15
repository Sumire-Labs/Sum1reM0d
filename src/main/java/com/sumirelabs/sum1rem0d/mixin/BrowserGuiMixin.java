package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.client.BrowserOverlays;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.2 {
@Mixin(Gui.class)
//?} else {
/*@Mixin(net.minecraft.client.Minecraft.class)
*///?}
public abstract class BrowserGuiMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$screen(Screen next, CallbackInfo ci) {
        if (com.sumirelabs.sum1rem0d.client.ViewerBlocker.screen(next)) { ci.cancel(); return; }
        BrowserOverlays.changed(next);
    }
}
