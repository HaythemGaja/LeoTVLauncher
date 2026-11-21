package com.leo.tvlauncher;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

/** Tinted layered materials, not a claim of macOS refraction or in-window backdrop blur. */
final class Glass {
    private Glass() {}

    static boolean light(Theme theme) {
        return "light".equals(theme.id);
    }

    static GradientDrawable panel(Context c, Theme theme) {
        int[] colors =
                light(theme)
                        ? new int[] {0xEAF9FAFC, 0xDCE9EDF3}
                        : new int[] {0xE0333740, 0xE01D2028};
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(Theme.dp(c, 26));
        // A quiet material edge, not a selected/focused control outline.
        d.setStroke(Theme.dp(c, 1), light(theme) ? 0x30FFFFFF : 0x20FFFFFF);
        return d;
    }

    static Drawable wallpaper(Theme theme) {
        return new Drawable() {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            public void draw(Canvas canvas) {
                float w = getBounds().width(), h = getBounds().height();
                if (w <= 0 || h <= 0) return;
                paint.setShader(
                        new LinearGradient(
                                0,
                                0,
                                w,
                                h,
                                light(theme)
                                        ? new int[] {0xFFE2E8F0, 0xFFC9D0DF}
                                        : new int[] {0xFF191C26, 0xFF0D1017},
                                null,
                                Shader.TileMode.CLAMP));
                canvas.drawRect(getBounds(), paint);
                paint.setShader(
                        new RadialGradient(
                                w * .78f,
                                h * .15f,
                                w * .65f,
                                light(theme) ? 0x70B1BFE2 : 0x405D668C,
                                0x00000000,
                                Shader.TileMode.CLAMP));
                canvas.drawRect(getBounds(), paint);
                paint.setShader(
                        new RadialGradient(
                                w * .35f,
                                h * .95f,
                                w * .55f,
                                light(theme) ? 0x60C9B9D9 : 0x303E526E,
                                0x00000000,
                                Shader.TileMode.CLAMP));
                canvas.drawRect(getBounds(), paint);
                paint.setShader(null);
            }

            @Override
            public void setAlpha(int alpha) {}

            @Override
            public void setColorFilter(ColorFilter filter) {}

            @Override
            public int getOpacity() {
                return PixelFormat.OPAQUE;
            }
        };
    }

    /** Real cross-window blur belongs only to supported dialog windows, never Home panels. */
    static void dialogShown(AlertDialog dialog, Context c) {
        Window window = dialog.getWindow();
        if (window == null) return;
        Theme theme = Theme.current(c);
        for (int buttonId :
                new int[] {
                    AlertDialog.BUTTON_POSITIVE,
                    AlertDialog.BUTTON_NEGATIVE,
                    AlertDialog.BUTTON_NEUTRAL
                }) {
            android.widget.Button button = dialog.getButton(buttonId);
            if (button != null) {
                button.setTextColor(theme.accent);
                button.setAllCaps(false);
                button.setTextSize(17);
            }
        }
        window.setBackgroundDrawable(panel(c, theme));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(.55f);
        if (Build.VERSION.SDK_INT < 31) return;
        WindowManager manager = c.getSystemService(WindowManager.class);
        if (manager == null) return;
        java.util.function.Consumer<Boolean> listener =
                enabled -> {
                    WindowManager.LayoutParams params = window.getAttributes();
                    if (enabled) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                        params.setBlurBehindRadius(Theme.dp(c, 16));
                        params.dimAmount = .25f;
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                        params.setBlurBehindRadius(0);
                        params.dimAmount = .55f;
                    }
                    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    window.setAttributes(params);
                };
        listener.accept(manager.isCrossWindowBlurEnabled());
        manager.addCrossWindowBlurEnabledListener(listener);
        dialog.setOnDismissListener(
                ignored -> manager.removeCrossWindowBlurEnabledListener(listener));
    }
}
