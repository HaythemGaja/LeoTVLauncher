import com.leo.tvlauncher.TileMetrics;

/** Run with tests/run-tests.sh. No Android runtime or dependencies required. */
public final class TileMetricsTest {
    private static int assertions;

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        for (float density : new float[] {0.75f, 1f, 1.33125f, 1.5f, 2f, 3f}) {
            for (int screenDp : new int[] {640, 960, 1280, 1920}) {
                int available = Math.round((screenDp - 200 - 56) * density);
                int previous = Integer.MAX_VALUE;
                for (int icon : new int[] {40, 56, 72, 88, 104}) {
                    int columns = TileMetrics.columns(available, density, icon);
                    int margin = Math.round(8 * density);
                    int width = TileMetrics.width(available, columns, margin);
                    check(columns >= 1, "at least one column");
                    check(
                            (width + 2 * margin) * columns <= available,
                            "tiles and margins fit content, not full screen");
                    check(width > 0, "positive tile width");
                    check(columns <= previous, "larger density preset never adds columns");
                    previous = columns;
                }
            }
        }
        check(
                TileMetrics.columns(704, 1, 72) == 3,
                "960dp screen less fixed sidebar and padding fits three comfortable cards");
        check(TileMetrics.columns(1, 1, 72) == 1, "narrow layout cannot divide by zero");
        check(
                TileMetrics.width(704, 3, 8) == 218,
                "last row uses the same fixed-width track as full rows");
        System.out.println("TileMetrics: " + assertions + " assertions passed");
    }
}
