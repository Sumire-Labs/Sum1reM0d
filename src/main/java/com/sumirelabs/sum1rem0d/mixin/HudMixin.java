package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.ClientRuntime;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

//? if >=26.2 {
@Mixin(Hud.class)
//?} else {
/*@Mixin(Gui.class)
*///?}
public abstract class HudMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void sum1rem0d$hud(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) { ClientRuntime.overlay(graphics); }
}
