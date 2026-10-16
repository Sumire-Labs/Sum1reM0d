package com.sumirelabs.sum1rem0d.game;

/** Server revisions increase within a connection; clear on every connection boundary. */
public final class InviteNotificationTracker {
  private long seen, generation;

  public boolean observe(long revision) {
    if (revision <= seen) return false;
    seen = revision;
    return true;
  }

  public void clear() {
    seen = 0;
    generation++;
  }

  public long generation() {
    return generation;
  }
}
