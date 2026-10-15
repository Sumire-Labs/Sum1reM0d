package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.client.*;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardHandler.class)
public abstract class BrowserKeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$key(long window, int action, KeyEvent event, CallbackInfo ci) { if (action != 0) ClientRuntime.input(); if (BrowserOverlays.key(event, action == 0)) ci.cancel(); }
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$character(long window, CharacterEvent event, CallbackInfo ci) { if (BrowserOverlays.character(event)) ci.cancel(); }
}
