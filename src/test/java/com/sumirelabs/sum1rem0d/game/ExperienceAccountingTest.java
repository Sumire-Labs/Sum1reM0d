package com.sumirelabs.sum1rem0d.game;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExperienceAccountingTest {
  @Test
  void curveMatchesVanillaAtBoundaries() {
    assertEquals(315, ExperienceAccounting.atLevel(15));
    assertEquals(352, ExperienceAccounting.atLevel(16));
    assertEquals(394, ExperienceAccounting.atLevel(17));
    assertEquals(1395, ExperienceAccounting.atLevel(30));
    assertEquals(1507, ExperienceAccounting.atLevel(31));
    assertEquals(1628, ExperienceAccounting.atLevel(32));
  }

  @Test
  void everyBarRoundTripsWithoutLosingPoints() {
    for (int points = 0; points < 10000; points++) {
      var b = ExperienceAccounting.bar(points);
      assertEquals(points, ExperienceAccounting.points(b.level(), b.progress()), "XP " + points);
    }
    for (long points : new long[] {100000, 1000000, 1000000000, Integer.MAX_VALUE}) {
      var b = ExperienceAccounting.bar(points);
      assertEquals(points, ExperienceAccounting.points(b.level(), b.progress()));
    }
  }

  @Test
  void freeBankRoundTripConservesXpAndCurrency() {
    var a = ExperienceAccounting.exchange(1395, 0, 100, 1, 1, 0, true, 1000000);
    assertEquals(1295, a.xpAfter());
    assertEquals(100, a.coinsAfter());
    var b =
        ExperienceAccounting.exchange(a.xpAfter(), a.coinsAfter(), 100, 1, 1, 0, false, 1000000);
    assertEquals(1395, b.xpAfter());
    assertEquals(0, b.coinsAfter());
  }

  @Test
  void unitsFeesAndInsufficientBalancesAreChecked() {
    var a = ExperienceAccounting.exchange(100, 0, 10, 5, 10, 10, true, 1000);
    assertEquals(18, a.coins());
    assertEquals(2, a.fee());
    assertThrows(
        IllegalArgumentException.class,
        () -> ExperienceAccounting.exchange(100, 0, 3, 5, 10, 0, true, 1000));
    assertThrows(
        IllegalArgumentException.class,
        () -> ExperienceAccounting.exchange(100, 0, 10, 1, 1, 0, false, 1000));
    assertThrows(
        IllegalArgumentException.class,
        () -> ExperienceAccounting.exchange(5, 0, 10, 1, 1, 0, true, 1000));
  }

  @Test
  void overflowAndCapsCannotMintOrDestroyPoints() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ExperienceAccounting.exchange(100, 100, 10, 1, 1, 0, true, 100));
    assertThrows(
        ArithmeticException.class,
        () ->
            ExperienceAccounting.exchange(100, 0, Long.MAX_VALUE, 1, 100, 0, true, Long.MAX_VALUE));
    assertThrows(IllegalArgumentException.class, () -> ExperienceAccounting.bar(-1));
    assertThrows(
        IllegalArgumentException.class, () -> ExperienceAccounting.bar(1L + Integer.MAX_VALUE));
    assertThrows(IllegalArgumentException.class, () -> ExperienceAccounting.points(10, Float.NaN));
  }
}
