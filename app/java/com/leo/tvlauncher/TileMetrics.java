package com.leo.tvlauncher;

/** Pure layout arithmetic shared by home/folders and the host-side regression test. */
public final class TileMetrics {
    private TileMetrics() {}

    public static int desiredWidthDp(int legacyIconDp) {
        // Comfortable is four across at 704dp after the fixed sidebar/padding.
        // Explicit tiny presets can fit five without shrinking text below 17sp.
        if (legacyIconDp <= 72) return Math.max(124, Math.min(160, 100 + legacyIconDp));
        return Math.min(240, 160 + (legacyIconDp - 72) * 2);
    }

    public static int columns(int availablePx, float density, int iconDp) {
        if (density <= 0) density = 1;
        int minimum = Math.round((desiredWidthDp(iconDp) + 16) * density);
        return Math.max(1, availablePx / Math.max(1, minimum));
    }

    public static int homeColumns(int availablePx, float density, int iconDp, int count) {
        int fitting = columns(availablePx, density, iconDp);
        // A small library gets an intentional four-track composition, unless Tiny is chosen.
        return count > 0 && count <= 8 && iconDp >= 32 ? Math.min(4, fitting) : fitting;
    }

    public static int heroColumns(int availablePx, float density) {
        if (density <= 0) density = 1;
        return Math.max(1, Math.min(3, availablePx / Math.max(1, Math.round(208 * density))));
    }

    public static int shelfColumns(int availablePx, float density) {
        if (density <= 0) density = 1;
        return Math.max(1, Math.min(4, availablePx / Math.max(1, Math.round(156 * density))));
    }

    public static int width(int availablePx, int columns, int gutterPx) {
        return Math.max(1, availablePx / Math.max(1, columns) - 2 * gutterPx);
    }
}
