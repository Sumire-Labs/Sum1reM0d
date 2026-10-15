package com.sumirelabs.sum1rem0d.game;

/** Fuel efficiency is output per fuel: a -80% day has efficiency .2 and costs 5 fuel per operation. */
public final class FuelAccounting {
    private FuelAccounting() {}
    public static double consumptionMultiplier(double efficiency) {
        if (!Double.isFinite(efficiency) || efficiency <= 0) throw new IllegalArgumentException("Fuel efficiency must be positive");
        return 1.0 / efficiency;
    }
    public static int requiredFuel(double credit, double cost) { return Math.max(0, (int) Math.ceil(cost - credit - 1.0E-9)); }
    public static double remainingCredit(double credit, double cost, int paid) { return Math.max(0, Math.min(0.999999999, credit + paid - cost)); }
}
