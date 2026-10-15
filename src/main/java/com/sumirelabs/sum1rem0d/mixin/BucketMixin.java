package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.BlockGameplay;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(BucketItem.class)
public abstract class BucketMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$bucket(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player instanceof ServerPlayer server && BlockGameplay.collectLava(server, hand)) cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
