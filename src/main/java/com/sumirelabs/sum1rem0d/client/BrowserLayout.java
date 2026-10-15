package com.sumirelabs.sum1rem0d.client;

/** Fits beside the existing container without moving Minecraft's widgets. */
public record BrowserLayout(int x, int y, int width, int height) {
    public static BrowserLayout fit(int left, int screenHeight, int requestedWidth, int requestedHeight, int gap) {
        int width = Math.max(0, Math.min(requestedWidth, left - gap - 4));
        int height = Math.max(0, Math.min(requestedHeight, screenHeight - 42));
        return new BrowserLayout(left - gap - width, Math.max(25, (screenHeight - height) / 2), width, height);
    }
    public boolean visible() { return width >= 40 && height >= 30; }
    public int[] exclusion() { return new int[] { x - 2, y - 18, width + 4, height + 20 }; }
}
