package com.sumirelabs.sum1rem0d.game;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InviteNotificationTrackerTest {
  @Test
  void repeatedAndStaleSnapshotsNeverRepeatNotifications() {
    var t = new InviteNotificationTracker();
    assertTrue(t.observe(1));
    assertFalse(t.observe(1));
    assertTrue(t.observe(3));
    assertFalse(t.observe(2));
    assertFalse(t.observe(0));
  }

  @Test
  void connectionResetAllowsReusedRevisionsButInvalidatesOldToken() {
    var t = new InviteNotificationTracker();
    t.observe(4);
    long old = t.generation();
    t.clear();
    assertNotEquals(old, t.generation());
    assertTrue(t.observe(1));
  }

  @Test
  void disabledNotificationsCanConsumeAnInviteWithoutReplayingOnEnable() {
    var t = new InviteNotificationTracker();
    t.observe(10);
    assertFalse(t.observe(10));
    assertTrue(t.observe(11));
  }
}
