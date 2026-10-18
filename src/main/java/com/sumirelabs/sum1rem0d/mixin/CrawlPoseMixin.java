package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.CrawlClientState;
import com.sumirelabs.sum1rem0d.game.WorldMobility;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class CrawlPoseMixin {
    @Inject(method = "updatePlayerPose", at = @At("TAIL"))
    private void sum1rem0d$keepCrawlPose(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        boolean active = player instanceof ServerPlayer ? WorldMobility.crawling(player)
                : CrawlClientState.crawling();
        if (WorldMobility.enabled("crawl") && active && !player.isInWater() && !player.isPassenger() && !player.isSleeping() && !player.isFallFlying()) player.setPose(Pose.SWIMMING);
    }
}
