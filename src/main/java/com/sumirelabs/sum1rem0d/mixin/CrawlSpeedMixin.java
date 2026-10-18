package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.WorldMobility;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Player overrides LivingEntity#getSpeed() in every supported Minecraft version,
// returning MOVEMENT_SPEED directly. Target the override so the server-side crawl
// multiplier is applied to the value used by player movement.
@Mixin(Player.class)
public abstract class CrawlSpeedMixin {
    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$crawlSpeed(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof Player player) cir.setReturnValue(WorldMobility.crawlSpeed(player, cir.getReturnValueF()));
    }
}
