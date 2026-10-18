package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.DailyTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses only the drawing of our resource reload overlay while preserving its tick lifecycle. */
@Mixin(LoadingOverlay.class)
public abstract class DailyReloadOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$hideReload(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (DailyTextures.quietReloadActive()) ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void sum1rem0d$allowQuietReloadToFinish(CallbackInfo ci) {
        if (DailyTextures.quietReloadActive() && fadeInStart < 0) {
            // Native render normally establishes this one-second readiness gate. Since quiet
            // reload skips drawing, establish it on tick instead so the future still completes.
            fadeInStart = Util.getMillis() - 1000L;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sum1rem0d$finishQuietReload(CallbackInfo ci) {
        if (!DailyTextures.quietReloadActive() || fadeOutStart < 0) return;
        // The native tick above ran onFinish (completing Minecraft's reload future). We clear the
        // visual overlay only after that lifecycle callback has executed.
        //? if >=26.2 {
        minecraft.gui.setOverlay(null);
        //?} else {
        /*minecraft.setOverlay(null);
        *///?}
        DailyTextures.reloadOverlayFinished();
    }
}
