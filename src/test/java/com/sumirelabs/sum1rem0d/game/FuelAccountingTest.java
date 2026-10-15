package com.sumirelabs.sum1rem0d.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FuelAccountingTest {
    private int fuelFor(int operations, double efficiency) {
        double credit = 0, cost = FuelAccounting.consumptionMultiplier(efficiency); int total = 0;
        for (int operation = 0; operation < operations; operation++) {
            int paid = FuelAccounting.requiredFuel(credit, cost); total += paid;
            credit = FuelAccounting.remainingCredit(credit, cost, paid);
        }
        return total;
    }
    @Test void eightyPercentWorseRequiresFiveFuelForOneItem() { assertEquals(5, fuelFor(1, .2)); assertEquals(50, fuelFor(10, .2)); }
    @Test void eightyPercentBetterMakesNineItemsWithFiveFuel() { assertEquals(5, fuelFor(9, 1.8)); }
    @Test void ordinaryAndHalfEfficiencyDaysHaveExpectedRatios() { assertEquals(10, fuelFor(10, 1)); assertEquals(2, fuelFor(1, .5)); assertEquals(5, fuelFor(4, .8)); }
    @Test void prepaidFractionsDoNotRoundEveryOperationUp() { assertEquals(2, fuelFor(3, 1.5)); assertEquals(5, fuelFor(6, 1.2)); }
    @Test void creditCarriesAcrossEfficiencyChanges() {
        double firstCost = FuelAccounting.consumptionMultiplier(1.8);
        int firstPaid = FuelAccounting.requiredFuel(0, firstCost);
        double credit = FuelAccounting.remainingCredit(0, firstCost, firstPaid);
        double nextCost = FuelAccounting.consumptionMultiplier(.8);
        assertEquals(1, firstPaid);
        assertEquals(1, FuelAccounting.requiredFuel(credit, nextCost));
    }
    @Test void invalidEfficiencyIsRejected() { assertThrows(IllegalArgumentException.class, () -> FuelAccounting.consumptionMultiplier(0)); assertThrows(IllegalArgumentException.class, () -> FuelAccounting.consumptionMultiplier(Double.NaN)); }
}
