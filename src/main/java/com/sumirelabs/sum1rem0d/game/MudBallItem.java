package com.sumirelabs.sum1rem0d.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The soap's opposite: intentionally dirty yourself, never falling below zero. */
public final class MudBallItem extends Item {
  public MudBallItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult use(Level level, Player player, InteractionHand hand) {
    if (!Rules.on("mud_ball") || !Rules.on("cleanliness")) return InteractionResult.PASS;
    if (player instanceof ServerPlayer server) {
      if (!server.isAlive() || server.isSpectator()) return InteractionResult.PASS;
      var stack = server.getItemInHand(hand);
      if (server.getCooldowns().isOnCooldown(stack)) return InteractionResult.PASS;
      var data = WorldState.get(server.level().getServer()).player(server);
      double amount = Rules.n("mud_ball", "cleanliness_loss");
      if (data.cleanliness <= 0 || amount <= 0) return InteractionResult.PASS;
      data.cleanliness =
          Math.max(0, Math.min(Rules.n("cleanliness", "maximum"), data.cleanliness) - amount);
      int ticks = Rules.i("mud_ball", "cooldown_ticks");
      if (ticks > 0) server.getCooldowns().addCooldown(stack, ticks);
      if (Rules.b("mud_ball", "consume")) stack.consume(1, server);
      Gameplay.sendState(server);
    }
    return InteractionResult.SUCCESS;
  }
}
