import com.leo.tvlauncher.BrandTint;

public final class BrandTintTest {
    private static int assertions;
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(BrandTint.fromPixels(new int[0]) == 0xFF252B32, "empty uses neutral");
        check(BrandTint.fromPixels(new int[] {0x00FF0000, 0xFFFFFFFF, 0xFF000000}) == 0xFF252B32, "ignore transparent, white and black");
        int red = BrandTint.fromPixels(new int[] {0xFFFF0000});
        check(((red >> 16) & 255) > (red & 255), "red icon makes red matte");
        int green = BrandTint.fromPixels(new int[] {0xFF00FF00});
        check(((green >> 8) & 255) > (green & 255), "green icon makes green matte");
        int blue = BrandTint.fromPixels(new int[] {0xFF0000FF});
        check((blue & 255) > ((blue >> 16) & 255), "blue icon makes blue matte");
        check(BrandTint.fromPixels(new int[] {0xFFFF0000,0xFFFF0000,0xFF0000FF}) == red, "dominant hue beats minority");
        check(BrandTint.fromPixels(new int[] {0xFFFF0000,0xFFFFFFFF,0xFF000000}) == red, "neutral pixels do not wash tint out");
        for (int r=0;r<256;r+=17) for (int g=0;g<256;g+=17) for (int b=0;b<256;b+=17) {
            int color=BrandTint.fromPixels(new int[] {0xFF000000 | r<<16 | g<<8 | b});
            check((color >>> 24)==255, "matte is opaque");
            check(((color >> 16)&255)<=69 && ((color >> 8)&255)<=69 && (color&255)<=69, "matte remains dark for white wordmark");
        }
        System.out.println("BrandTint: " + assertions + " assertions passed");
    }
}
