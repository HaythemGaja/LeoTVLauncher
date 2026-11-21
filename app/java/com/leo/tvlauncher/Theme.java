package com.leo.tvlauncher;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Theme presets + icon shapes, stored in SharedPreferences. */
public class Theme {

    public final String id;
    public final String name;
    public final int bg;
    public final int card;
    public final int cardFocus;
    public final int accent;
    public final int text;
    public final int text2;

    private Theme(String id, String name, int bg, int card, int cardFocus,
                  int accent, int text, int text2) {
        this.id = id;
        this.name = name;
        this.bg = bg;
        this.card = card;
        this.cardFocus = cardFocus;
        this.accent = accent;
        this.text = text;
        this.text2 = text2;
    }

    public static final Theme[] PRESETS = {
            new Theme("midnight", "Midnight", 0xFF101418, 0xFF1C2126, 0xFF2A3138,
                    0xFFFFB300, 0xFFFFFFFF, 0xFFB0BEC5),
            new Theme("ocean", "Ocean", 0xFF0A1628, 0xFF12233D, 0xFF1B3355,
                    0xFF4FC3F7, 0xFFFFFFFF, 0xFF90A4AE),
            new Theme("forest", "Forest", 0xFF0C1A12, 0xFF14261B, 0xFF1E3A28,
                    0xFF81C784, 0xFFFFFFFF, 0xFFA5BFA8),
            new Theme("crimson", "Crimson", 0xFF1A0E0E, 0xFF2A1515, 0xFF3D1F1F,
                    0xFFFF5252, 0xFFFFFFFF, 0xFFC9A9A9),
            new Theme("light", "Light", 0xFFF5F5F5, 0xFFFFFFFF, 0xFFE0E0E0,
                    0xFF1565C0, 0xFF212121, 0xFF757575),
    };

    public static final String[] SHAPES = {"circle", "rounded", "square"};
    public static final String[] SHAPE_NAMES = {"Circle", "Rounded square", "Square"};

    private static final String PREFS = "leo_tv_launcher";
    private static final String KEY_THEME = "theme_id";
    private static final String KEY_SHAPE = "icon_shape";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static Theme current(Context c) {
        String id = prefs(c).getString(KEY_THEME, "midnight");
        for (Theme t : PRESETS) if (t.id.equals(id)) return t;
        return PRESETS[0];
    }

    public static void setCurrent(Context c, String id) {
        prefs(c).edit().putString(KEY_THEME, id).apply();
    }

    public static String iconShape(Context c) {
        return prefs(c).getString(KEY_SHAPE, "rounded");
    }

    public static void setIconShape(Context c, String shape) {
        prefs(c).edit().putString(KEY_SHAPE, shape).apply();
    }

    public static int iconSizeDp(Context c) {
        return prefs(c).getInt("icon_size", 72);
    }

    public static void setIconSizeDp(Context c, int dp) {
        prefs(c).edit().putInt("icon_size", dp).apply();
    }

    public static int labelSizeSp(Context c) {
        int v = prefs(c).getInt("label_size", -1);
        if (v > 0) return v;
        // Auto: follow the icon size, clamped to readable bounds.
        int iconDp = iconSizeDp(c);
        return Math.max(11, Math.min(22, Math.round(iconDp * 0.22f)));
    }

    public static void setLabelSizeSp(Context c, int sp) {
        prefs(c).edit().putInt("label_size", sp).apply();
    }

    public static boolean labelSizeAuto(Context c) {
        return prefs(c).getInt("label_size", -1) <= 0;
    }

    public static int dp(Context c, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                c.getResources().getDisplayMetrics());
    }

    /** Focus-aware tile background in the current palette. */
    public static StateListDrawable tileBackground(Context c, Theme t) {
        float r = dp(c, 12);
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(t.card);
        normal.setCornerRadius(r);
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(t.cardFocus);
        focused.setCornerRadius(r);
        focused.setStroke(dp(c, 3), t.accent);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, focused);
        s.addState(new int[]{}, normal);
        return s;
    }

    /** Tile background while an app is being moved: always shows the accent border. */
    public static StateListDrawable movingTileBackground(Context c, Theme t) {
        float r = dp(c, 12);
        GradientDrawable moving = new GradientDrawable();
        moving.setColor(t.cardFocus);
        moving.setCornerRadius(r);
        moving.setStroke(dp(c, 4), t.accent);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{}, moving);
        return s;
    }

    /** Paint activity root background + title in the current palette. */
    public static Theme applyBase(Activity a, TextView titleView) {
        Theme t = current(a);
        View root = ((ViewGroup) a.findViewById(android.R.id.content)).getChildAt(0);
        if (root != null) root.setBackgroundColor(t.bg);
        if (titleView != null) titleView.setTextColor(t.accent);
        return t;
    }

    /** Clip an app icon to the chosen shape. */
    public static Bitmap shapedIcon(Drawable d, int sizePx, String shape) {
        Bitmap src = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(src);
        d.setBounds(0, 0, sizePx, sizePx);
        d.draw(c);
        Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas c2 = new Canvas(out);
        Path p = new Path();
        float s = sizePx;
        if ("circle".equals(shape)) {
            p.addCircle(s / 2f, s / 2f, s / 2f, Path.Direction.CW);
        } else if ("square".equals(shape)) {
            p.addRect(new RectF(0, 0, s, s), Path.Direction.CW);
        } else {
            p.addRoundRect(new RectF(0, 0, s, s), s * 0.28f, s * 0.28f, Path.Direction.CW);
        }
        c2.clipPath(p);
        c2.drawBitmap(src, 0, 0, null);
        return out;
    }
}
