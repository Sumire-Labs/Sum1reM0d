package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.WorldState;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerConnectionMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void sum1rem0d$join(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        com.sumirelabs.sum1rem0d.game.Economy.recover(player);
        var data = WorldState.get(player.level().getServer()).player(player);
        data.lastClock = player.level().getOverworldClockTime(); data.afkTicks = 0; data.exposureTicks = 0; data.sleepTicks = 0; data.sleepAwarded = false; data.lastX = Double.NaN;
    }
    @Inject(method = "remove", at = @At("HEAD"))
    private void sum1rem0d$leave(ServerPlayer player, CallbackInfo ci) {
        com.sumirelabs.sum1rem0d.game.MultiplayerVerification.leaving(player);
        com.sumirelabs.sum1rem0d.game.CoinServicesMultiplayerVerification.leaving(player);
        com.sumirelabs.sum1rem0d.game.Economy.leaving(player);
        var data = WorldState.get(player.level().getServer()).player(player);
        data.lastClock = Long.MIN_VALUE; data.afkTicks = 0; data.exposureTicks = 0; data.sleepTicks = 0; data.lastX = Double.NaN;
    }
}
