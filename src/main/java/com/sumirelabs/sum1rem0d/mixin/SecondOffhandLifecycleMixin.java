package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.SecondOffhandService;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class SecondOffhandLifecycleMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void sum1rem0d$syncSecondOffhandsOnJoin(Connection connection, ServerPlayer player,
            CommonListenerCookie cookie, CallbackInfo ci) {
        SecondOffhandService.syncAll(player);
        com.sumirelabs.sum1rem0d.game.PaxelContent.sendRecipeMap(player);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void sum1rem0d$clearSecondOffhandOnLeave(ServerPlayer player, CallbackInfo ci) {
        SecondOffhandService.disconnect(player);
    }
}
