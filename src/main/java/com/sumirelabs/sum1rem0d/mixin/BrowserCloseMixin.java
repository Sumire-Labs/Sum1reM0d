package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.client.BrowserOverlays;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class BrowserCloseMixin {
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("RETURN"))
    private void sum1rem0d$disconnect(net.minecraft.client.gui.screens.Screen screen, boolean transfer, boolean keepResourcePacks, CallbackInfo ci) {
        com.sumirelabs.sum1rem0d.config.ClientRules.clear();
        com.sumirelabs.sum1rem0d.config.ClientGameState.clear();
        com.sumirelabs.sum1rem0d.client.AfkLights.clear();
        BrowserOverlays.closeAll();
    }
    @Inject(method = "close", at = @At("HEAD"))
    private void sum1rem0d$close(CallbackInfo ci) { BrowserOverlays.closeAll(); }
}
