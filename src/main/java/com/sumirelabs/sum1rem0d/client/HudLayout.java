package com.sumirelabs.sum1rem0d.client;

/** Positions use GUI pixels measured inward from one of the four corners. */
public final class HudLayout {
  private HudLayout() {}

  public record Placement(float x, float y, float scale) {}

  public static Placement place(
      int screenWidth,
      int screenHeight,
      int width,
      int height,
      int anchor,
      int offsetX,
      int offsetY,
      double requestedScale) {
    float scale =
        (float)
            Math.min(
                requestedScale,
                Math.min(
                    screenWidth / (double) Math.max(1, width),
                    screenHeight / (double) Math.max(1, height)));
    scale = Math.max(0.01F, scale);
    float maxX = Math.max(0, screenWidth / scale - width),
        maxY = Math.max(0, screenHeight / scale - height);
    float x = (anchor % 2 == 1 ? maxX - offsetX / scale : offsetX / scale),
        y = (anchor >= 2 ? maxY - offsetY / scale : offsetY / scale);
    return new Placement(Math.max(0, Math.min(maxX, x)), Math.max(0, Math.min(maxY, y)), scale);
  }
}
