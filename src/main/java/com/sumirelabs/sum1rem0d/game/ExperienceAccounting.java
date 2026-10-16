package com.sumirelabs.sum1rem0d.game;

/** Vanilla XP curve and integral bank quotes, independent of Minecraft/bootstrap. */
public final class ExperienceAccounting {
  private ExperienceAccounting() {}

  public record Bar(int level, float progress, int total) {}

  public record Quote(long xpAfter, long coinsAfter, long coins, long fee) {}

  public static long atLevel(int level) {
    if (level < 0) throw new IllegalArgumentException("XP level must be nonnegative");
    long square = Math.multiplyExact((long) level, level);
    if (level <= 16) return Math.addExact(square, 6L * level);
    if (level <= 31) return (Math.multiplyExact(5L, square) - 81L * level + 720) / 2;
    return Math.addExact(Math.multiplyExact(9L, square) - 325L * level, 4440) / 2;
  }

  public static long needed(int level) {
    if (level < 0) throw new IllegalArgumentException("XP level must be nonnegative");
    return level >= 30
        ? 112L + 9L * (level - 30)
        : level >= 15 ? 37L + 5L * (level - 15) : 7L + 2L * level;
  }

  public static long points(int level, float progress) {
    if (!Float.isFinite(progress) || progress < 0 || progress >= 1)
      throw new IllegalArgumentException("Invalid XP progress");
    long partial = Math.min(needed(level) - 1, Math.round(progress * (double) needed(level)));
    return Math.addExact(atLevel(level), partial);
  }

  public static Bar bar(long points) {
    if (points < 0 || points > Integer.MAX_VALUE)
      throw new IllegalArgumentException("XPポイントが銀行の上限を超えています");
    int low = 0, high = 100000;
    while (low + 1 < high) {
      int mid = (low + high) >>> 1;
      if (atLevel(mid) <= points) low = mid;
      else high = mid;
    }
    long rest = points - atLevel(low);
    return new Bar(low, (float) (rest / (double) needed(low)), (int) points);
  }

  public static Quote exchange(
      long xp,
      long coins,
      long amount,
      int xpUnit,
      int coinUnit,
      int feePercent,
      boolean deposit,
      long maximum) {
    if (xp < 0
        || coins < 0
        || amount <= 0
        || xpUnit <= 0
        || coinUnit <= 0
        || amount % xpUnit != 0
        || feePercent < 0
        || feePercent > 99) throw new IllegalArgumentException("交換単位と金額を確認してください");
    long gross = Math.multiplyExact(amount / xpUnit, coinUnit),
        fee = (Math.multiplyExact(gross, feePercent) + 99) / 100;
    if (deposit) {
      if (amount > xp) throw new IllegalArgumentException("XPが不足しています");
      long received = gross - fee;
      if (received <= 0) throw new IllegalArgumentException("手数料を超える交換量を指定してください");
      return new Quote(xp - amount, CoinMath.credit(coins, received, maximum), received, fee);
    }
    long cost = Math.addExact(gross, fee);
    if (cost > coins) throw new IllegalArgumentException("コインが不足しています");
    long next = Math.addExact(xp, amount);
    bar(next);
    return new Quote(next, coins - cost, cost, fee);
  }
}
