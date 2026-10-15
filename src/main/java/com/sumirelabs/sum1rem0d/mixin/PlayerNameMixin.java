package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.*;
import com.sumirelabs.sum1rem0d.config.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Player.class)
public abstract class PlayerNameMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$name(CallbackInfoReturnable<Component> cir) {
        Player player = (Player) (Object) this;
        String alias;
        if (player instanceof ServerPlayer server) {
            if (!Rules.on("name_swap") || !Rules.b("name_swap", "chat")) return;
            alias = WorldState.get(server.level().getServer()).aliases().get(player.getUUID().toString());
        } else {
            if (!ClientGameState.received() || !ClientRules.enabled("name_swap") || !ClientRules.bool("name_swap", "nametag")) return;
            alias = ClientGameState.alias(player.getUUID());
        }
        if (alias != null) cir.setReturnValue(net.minecraft.world.scores.PlayerTeam.formatNameForTeam(player.getTeam(), Component.literal(alias)).withStyle(cir.getReturnValue().getStyle()));
    }
}
