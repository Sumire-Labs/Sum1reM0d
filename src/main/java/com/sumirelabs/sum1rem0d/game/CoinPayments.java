package com.sumirelabs.sum1rem0d.game;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.config.ClientGameState;
import com.sumirelabs.sum1rem0d.mixin.*;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/**
 * A payment is persisted only after the native menu operation succeeds; save failures restore the
 * entire click.
 */
public final class CoinPayments {
  private CoinPayments() {}

  private static final ThreadLocal<Transaction> ACTIVE = new ThreadLocal<>();

  public static boolean enabled(String feature) {
    return Rules.on("economy")
        && Rules.on(feature)
        && (WorldState.serverThread()
            || ClientGameState.snapshot().has("economy")
                && ClientGameState.snapshot().getAsJsonObject("economy").has("xpPoints"));
  }

  public static long cost(String feature, int levels) {
    return Math.multiplyExact((long) Math.max(0, levels), Rules.i(feature, "coins_per_level"));
  }

  public static long balance(Player p) {
    if (p instanceof ServerPlayer server) return Economy.balance(server);
    var state = ClientGameState.snapshot();
    return state.has("economy") ? state.getAsJsonObject("economy").get("balance").getAsLong() : 0;
  }

  public static int levels(Player p, String feature) {
    return (int) Math.min(Integer.MAX_VALUE, balance(p) / Rules.i(feature, "coins_per_level"));
  }

  public static int[] displayCosts(int[] original) {
    if (!enabled("coin_enchanting")) return original;
    int[] scaled = original.clone();
    for (int i = 0; i < scaled.length; i++)
      scaled[i] =
          (int)
              Math.min(
                  Integer.MAX_VALUE,
                  cost(
                      "coin_enchanting",
                      Rules.b("coin_enchanting", "require_balance")
                          ? scaled[i]
                          : scaled[i] > 0 ? i + 1 : 0));
    return scaled;
  }

  public static int displayBalance(Player p) {
    return (int) Math.min(Integer.MAX_VALUE, balance(p));
  }

  public static boolean canAnvil(Player p, int levels) {
    return p.isAlive()
        && !p.isSpectator()
        && levels > 0
        && (p.hasInfiniteMaterials() || balance(p) >= cost("coin_anvil", levels));
  }

  public static void charge(Player p, String feature, int levels) {
    if (!(p instanceof ServerPlayer server) || p.hasInfiniteMaterials()) return;
    var transaction = ACTIVE.get();
    if (transaction == null || transaction.player != server)
      throw new PaymentFailure("支払い処理を再試行してください");
    long amount = cost(feature, levels);
    if (amount > Economy.balance(server)) throw new PaymentFailure("コインが不足しています");
    Economy.balance(server, Economy.balance(server) - amount);
  }

  public static void afterCommit(Runnable action) {
    var transaction = ACTIVE.get();
    if (transaction == null) action.run();
    else transaction.afterCommit.add(action);
  }

  public static Transaction begin(AbstractContainerMenu menu, Player player, String feature) {
    if (!enabled(feature)
        || !(player instanceof ServerPlayer server)
        || player.hasInfiniteMaterials()) return null;
    if (ACTIVE.get() != null) throw new IllegalStateException("Nested coin payment");
    var transaction = new Transaction(menu, server);
    ACTIVE.set(transaction);
    return transaction;
  }

  public static final class PaymentFailure extends RuntimeException {
    public PaymentFailure(String message) {
      super(message);
    }
  }

  public static final class Transaction implements AutoCloseable {
    private final AbstractContainerMenu menu;
    private final ServerPlayer player;
    private final JsonObject world;
    private final long balance;
    private final PlayerExperience xp;
    private final int seed, enchantStat;
    private final List<ItemStack> slots;
    private final ItemStack carried;
    private final int[] data;
    private final List<Runnable> afterCommit = new ArrayList<>();
    private boolean durable, rolledBack;

    Transaction(AbstractContainerMenu menu, ServerPlayer p) {
      this.menu = menu;
      player = p;
      balance = Economy.balance(p);
      world = WorldState.get(p.level().getServer()).checkpoint();
      xp = PlayerExperience.capture(p);
      seed = p.getEnchantmentSeed();
      enchantStat = p.getStats().getValue(Stats.CUSTOM.get(Stats.ENCHANT_ITEM));
      slots = menu.getItems().stream().map(ItemStack::copy).toList();
      carried = menu.getCarried().copy();
      data =
          ((CoinMenuDataAccess) menu)
              .sum1rem0d$dataSlots().stream().mapToInt(DataSlot::get).toArray();
    }

    public void finish() {
      if (Economy.balance(player) == balance) return;
      try {
        WorldState.get(player.level().getServer()).saveStrict();
        durable = true;
      } catch (RuntimeException failure) {
        com.sumirelabs.sum1rem0d.Sum1reM0d.LOGGER.error("Cannot persist coin payment", failure);
        rollback();
        throw new PaymentFailure("保存に失敗したため支払いと操作を取り消しました");
      }
      afterCommit.forEach(Runnable::run);
      Economy.saved(player);
      player.containerMenu.broadcastFullState();
    }

    public void rollback() {
      if (durable || rolledBack) return;
      rolledBack = true;
      WorldState.get(player.level().getServer()).restore(world);
      xp.apply(player);
      player.getStats().setValue(player, Stats.CUSTOM.get(Stats.ENCHANT_ITEM), enchantStat);
      ((CoinEnchantmentSeedAccess) player).sum1rem0d$enchantmentSeed(seed);
      for (int i = 0; i < slots.size(); i++) menu.getSlot(i).set(slots.get(i).copy());
      menu.setCarried(carried.copy());
      var values = ((CoinMenuDataAccess) menu).sum1rem0d$dataSlots();
      for (int i = 0; i < data.length; i++) values.get(i).set(data[i]);
      menu.broadcastFullState();
      xp.send(player);
      Gameplay.sendState(player);
    }

    public void rejected(PaymentFailure failure) {
      player.sendSystemMessage(Component.literal("[Sum1reコ↑イン↓] " + failure.getMessage()));
    }

    @Override
    public void close() {
      ACTIVE.remove();
    }
  }
}
