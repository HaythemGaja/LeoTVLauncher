package com.leo.tvlauncher;

/** Small deterministic icon palette sampler. No network, brand database or arbitrary app list. */
public final class BrandTint {
    private BrandTint() {}

    public static int fromPixels(int[] pixels) {
        long[] red = new long[12], green = new long[12], blue = new long[12], weight = new long[12];
        for (int pixel : pixels) {
            int alpha = pixel >>> 24;
            int r = (pixel >>> 16) & 255, g = (pixel >>> 8) & 255, b = pixel & 255;
            int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
            int chroma = max - min;
            if (alpha < 128 || chroma < 32 || max < 48) continue;
            float hue =
                    max == r
                            ? ((g - b) / (float) chroma + 6) % 6
                            : max == g
                                    ? (b - r) / (float) chroma + 2
                                    : (r - g) / (float) chroma + 4;
            int bucket = Math.min(11, (int) (hue * 2));
            long w = (long) chroma * alpha;
            red[bucket] += r * w;
            green[bucket] += g * w;
            blue[bucket] += b * w;
            weight[bucket] += w;
        }
        int best = 0;
        for (int i = 1; i < weight.length; i++) if (weight[i] > weight[best]) best = i;
        if (weight[best] == 0) return 0xFF252B32;
        int r = (int) (red[best] / weight[best]),
                g = (int) (green[best] / weight[best]),
                b = (int) (blue[best] / weight[best]);
        // Compress luminance for white logo/wordmark contrast, retaining the sampled hue.
        return 0xFF000000 | ((18 + r / 5) << 16) | ((18 + g / 5) << 8) | (18 + b / 5);
    }
}
