package com.sumirelabs.sum1rem0d.mixin;

import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keeps the original signed body intact and changes only the recipient-visible unsigned content. */
@Mixin(PlayerList.class)
public abstract class SlimeMouthChatMixin {
    @ModifyVariable(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private PlayerChatMessage sum1rem0d$forceDisplayText(PlayerChatMessage message) {
        ServerPlayer sender = ((PlayerList) (Object) this).getPlayer(message.sender());
        return sender == null ? message : com.sumirelabs.sum1rem0d.game.SlimeMouthContent.visibleChat(sender, message);
    }
}
