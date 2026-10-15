package com.sumirelabs.sum1rem0d.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class CoinItem extends Item {
    public CoinItem(Properties properties) { super(properties); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!Rules.on("economy") || !Rules.b("economy", "deposit")) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server) Economy.depositHand(server, hand);
        return InteractionResult.SUCCESS;
    }
}
