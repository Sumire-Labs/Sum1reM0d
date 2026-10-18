package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.config.*;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(PlayerInfo.class)
public abstract class TabNameMixin {
    @Shadow public abstract com.mojang.authlib.GameProfile getProfile();
    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$tab(CallbackInfoReturnable<Component> cir) {
        if (ClientGameState.received() && ClientRules.enabled("name_swap") && ClientRules.bool("name_swap", "tab")) {
            String name = ClientGameState.alias(getProfile().id());
            if (name != null) cir.setReturnValue(net.minecraft.world.scores.PlayerTeam.formatNameForTeam(((PlayerInfo) (Object) this).getTeam(), Component.literal(name)).withStyle(style -> style.withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(Component.literal(getProfile().name())))));
        }
        if (PersonalConfig.store().enabled("tab_ping")) {
            int ping = Math.max(0, ((PlayerInfo) (Object) this).getLatency());
            Component base = cir.getReturnValue();
            if (base == null) base = Component.literal(getProfile().name());
            cir.setReturnValue(base.copy().append(Component.literal("  " + ping + " ms").withStyle(net.minecraft.ChatFormatting.GRAY)));
        }
    }
}
