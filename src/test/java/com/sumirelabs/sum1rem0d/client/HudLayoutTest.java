package com.sumirelabs.sum1rem0d.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HudLayoutTest {
  @Test
  void cornersAndDefaults() {
    assertEquals(
        new HudLayout.Placement(8, 28, 1), HudLayout.place(320, 240, 100, 15, 0, 8, 28, 1));
    assertEquals(
        new HudLayout.Placement(212, 217, 1), HudLayout.place(320, 240, 100, 15, 3, 8, 8, 1));
    assertEquals(
        new HudLayout.Placement(8, 217, 1), HudLayout.place(320, 240, 100, 15, 2, 8, 8, 1));
    assertEquals(
        new HudLayout.Placement(212, 8, 1), HudLayout.place(320, 240, 100, 15, 1, 8, 8, 1));
  }

  @Test
  void scaleAndBounds() {
    var p = HudLayout.place(320, 240, 100, 15, 3, 8, 8, 2);
    assertEquals(56, p.x());
    assertEquals(101, p.y());
    var large = HudLayout.place(320, 240, 500, 15, 0, 8192, 8192, 3);
    assertTrue(large.scale() <= 0.64F);
    assertTrue((large.x() + 500) * large.scale() <= 320.01);
    assertTrue((large.y() + 15) * large.scale() <= 240.01);
  }
}
