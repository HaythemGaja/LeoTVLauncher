package com.leo.tvlauncher;

/** Pure layout arithmetic shared by home/folders and the host-side regression test. */
public final class TileMetrics {
    private TileMetrics() {}

    public static int desiredWidthDp(int legacyIconDp) {
        return Math.max(164, Math.min(280, 140 + legacyIconDp));
    }

    public static int columns(int availablePx, float density, int iconDp) {
        if (density <= 0) density = 1;
        int minimum = Math.round((desiredWidthDp(iconDp) + 16) * density);
        return Math.max(1, availablePx / Math.max(1, minimum));
    }

    public static int width(int availablePx, int columns, int gutterPx) {
        return Math.max(1, availablePx / Math.max(1, columns) - 2 * gutterPx);
    }
}
