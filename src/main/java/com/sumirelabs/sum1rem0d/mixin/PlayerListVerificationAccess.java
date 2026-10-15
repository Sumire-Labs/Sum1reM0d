package com.sumirelabs.sum1rem0d.mixin;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Used only by the opt-in empty-server verifier to register temporary native actors. */
@Mixin(PlayerList.class)
public interface PlayerListVerificationAccess {
    @Accessor("players") List<ServerPlayer> sum1rem0d$verificationPlayers();
}
