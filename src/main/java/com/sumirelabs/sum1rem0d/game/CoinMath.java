package com.sumirelabs.sum1rem0d.game;

/** Integral accounting: validate both accounts before mutating either. */
public final class CoinMath {
    private CoinMath() {}
    public record Exchange(long first, long second) {}
    public static Exchange exchange(long a, long b, long giveA, long giveB, long maximum) {
        if (a < 0 || b < 0 || giveA < 0 || giveB < 0 || giveA > a || giveB > b || maximum < 1)
            throw new IllegalArgumentException("残高または金額が不正です");
        long nextA = Math.addExact(a - giveA, giveB), nextB = Math.addExact(b - giveB, giveA);
        if (nextA > Math.max(a, maximum) || nextB > Math.max(b, maximum)) throw new IllegalArgumentException("受取後の残高が上限を超えます");
        return new Exchange(nextA, nextB);
    }
    public static long credit(long balance, long amount, long maximum) {
        if (balance < 0 || amount < 0) throw new IllegalArgumentException("金額が不正です");
        long next = Math.addExact(balance, amount);
        if (next > Math.max(balance, maximum)) throw new IllegalArgumentException("残高上限です");
        return next;
    }
}
