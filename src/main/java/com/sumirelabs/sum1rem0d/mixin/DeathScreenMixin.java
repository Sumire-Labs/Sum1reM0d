package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.config.ClientRules;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/Screen;<init>(Lnet/minecraft/network/chat/Component;)V"), index = 0)
    private static Component sum1rem0d$title(Component original) {
        return ClientRules.enabled("death_message") ? Component.literal(ClientRules.text("death_message", "text")) : original;
    }
}
