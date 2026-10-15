package com.sumirelabs.sum1rem0d.mixin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayer.class)
public abstract class CoinMerchantSyncMixin {
    @Inject(method="sendMerchantOffers",at=@At("HEAD"))
    private void sum1rem0d$offers(int id,MerchantOffers offers,int level,int xp,boolean progress,boolean restock,CallbackInfo ci){for(var offer:offers){((com.sumirelabs.sum1rem0d.game.CoinMerchantConversion)offer).sum1rem0d$prepareCurrency(com.sumirelabs.sum1rem0d.game.Rules.on("economy")&&com.sumirelabs.sum1rem0d.game.Rules.on("coin_trading"));offer.getCostA();offer.getCostB();offer.getResult();}}
}
