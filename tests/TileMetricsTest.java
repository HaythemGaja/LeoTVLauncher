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
                for (int icon : new int[] {24, 32, 40, 56, 72, 88, 104}) {
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
                TileMetrics.columns(704, 1, 72) == 4,
                "960dp screen less fixed sidebar and padding fits four comfortable cards");
        check(TileMetrics.columns(1, 1, 72) == 1, "narrow layout cannot divide by zero");
        check(
                TileMetrics.width(704, 4, 8) == 160,
                "last row uses the same fixed-width track as full rows");
        check(TileMetrics.columns(704, 1, 24) == 5, "tiny preset really fits more cards");
        check(TileMetrics.columns(704, 1, 32) == 4, "very compact keeps four tracks at this width");
        check(TileMetrics.desiredWidthDp(24) >= 124, "tiny cards keep room for readable labels");
        check(TileMetrics.columns(704, 1.33125f, 72) == TileMetrics.columns(529, 1, 72), "density, not raw pixels, controls columns");
        check(TileMetrics.homeColumns(1024, 1, 72, 5) == 4, "five apps compose as four across on a wide display");
        check(TileMetrics.homeColumns(704, 1, 24, 5) == 5, "explicit Tiny can fit five apps");
        check(TileMetrics.homeColumns(400, 1, 72, 5) == 2, "four-column target never squeezes narrow screens");
        check(TileMetrics.homeColumns(1024, 1, 72, 30) == 5, "large libraries retain width-driven density");
        System.out.println("TileMetrics: " + assertions + " assertions passed");
    }
}
