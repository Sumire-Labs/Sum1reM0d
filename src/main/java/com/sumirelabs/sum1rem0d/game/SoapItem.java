package com.sumirelabs.sum1rem0d.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class SoapItem extends Item {
    public SoapItem(Properties properties) { super(properties); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!Rules.on("cleanliness")) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) {
            var data = WorldState.get(serverPlayer.level().getServer()).player(serverPlayer);
            if (data.cleanliness >= Rules.n("cleanliness", "maximum")) return InteractionResult.PASS;
            data.cleanliness = Math.min(Rules.n("cleanliness", "maximum"), data.cleanliness + Rules.n("cleanliness", "soap_recovery"));
            player.getItemInHand(hand).consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
