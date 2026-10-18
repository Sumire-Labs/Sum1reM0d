package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.DeathBoxContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class DeathBoxInventoryMixin {
    @Inject(method="dropEquipment",at=@At("HEAD"))
    private void sum1rem0d$captureDeathInventory(ServerLevel level, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player) DeathBoxContent.capture(player, level);
    }
}
