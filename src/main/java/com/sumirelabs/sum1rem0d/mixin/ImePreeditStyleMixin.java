package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.ImeInput;
import net.minecraft.client.gui.components.IMEPreeditOverlay;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Styles native composition clauses without touching committed text or OS candidate placement. */
@Mixin(IMEPreeditOverlay.class)
public abstract class ImePreeditStyleMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/PreeditEvent;toFormattedText(Lnet/minecraft/network/chat/Style;)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent sum1rem0d$formatComposition(PreeditEvent event, Style vanillaFocusedStyle) {
        return ImeInput.format(event, vanillaFocusedStyle);
    }
}
