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

    private Theme(
            String id,
            String name,
            int bg,
            int card,
            int cardFocus,
            int accent,
            int text,
            int text2) {
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
        new Theme(
                "midnight",
                "Midnight",
                0xFF13161D,
                0xE02A2E38,
                0xE03B414D,
                0xFFBED0F1,
                0xFFFFFFFF,
                0xFFB0BEC5),
        new Theme(
                "ocean",
                "Ocean",
                0xFF13161D,
                0xE02A2E38,
                0xE03B414D,
                0xFFA5C9EA,
                0xFFFFFFFF,
                0xFF90A4AE),
        new Theme(
                "forest",
                "Forest",
                0xFF13161D,
                0xE02A2E38,
                0xE03B414D,
                0xFFAACBB7,
                0xFFFFFFFF,
                0xFFA5BFA8),
        new Theme(
                "crimson",
                "Crimson",
                0xFF13161D,
                0xE02A2E38,
                0xE03B414D,
                0xFFE1B4BF,
                0xFFFFFFFF,
                0xFFC9A9A9),
        new Theme(
                "light",
                "Light",
                0xFFF5F5F5,
                0xFFFFFFFF,
                0xFFE0E0E0,
                0xFF1565C0,
                0xFF212121,
                0xFF616161),
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
        return Math.max(17, Math.min(22, Math.round(iconDp * 0.25f)));
    }

    public static void setLabelSizeSp(Context c, int sp) {
        prefs(c).edit().putInt("label_size", sp).apply();
    }

    public static boolean labelSizeAuto(Context c) {
        return prefs(c).getInt("label_size", -1) <= 0;
    }

    public static int dp(Context c, int dp) {
        return (int)
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics());
    }

    /** Shared states: focus, press, disabled and resting are deliberately different. */
    public static StateListDrawable tileBackground(Context c, Theme t) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {-android.R.attr.state_enabled}, surface(c, t.card, 0, 0));
        states.addState(new int[] {android.R.attr.state_pressed}, surface(c, t.cardFocus, 0, 0));
        states.addState(
                new int[] {android.R.attr.state_focused}, surface(c, t.cardFocus, t.accent, 1));
        states.addState(new int[] {android.R.attr.state_selected}, surface(c, t.cardFocus, 0, 0));
        states.addState(new int[] {}, surface(c, t.card, 0, 0));
        return states;
    }

    public static GradientDrawable surface(Context c, int fill, int stroke, int width) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(c, 22));
        if (width > 0) bg.setStroke(dp(c, width), stroke);
        return bg;
    }

    /** Always visible while moving, not confused with the focus outline. */
    public static StateListDrawable movingTileBackground(Context c, Theme t) {
        GradientDrawable bg = surface(c, t.cardFocus, t.accent, 1);
        bg.setStroke(dp(c, 2), t.accent, dp(c, 7), dp(c, 4));
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {}, bg);
        return states;
    }

    public static boolean reducedMotion(Context c) {
        if (prefs(c).getBoolean("reduce_motion", false)) return true;
        try {
            return android.provider.Settings.Global.getFloat(
                            c.getContentResolver(),
                            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                            1f)
                    == 0f;
        } catch (SecurityException ignored) {
            return false;
        }
    }

    public static void setReducedMotion(Context c, boolean value) {
        prefs(c).edit().putBoolean("reduce_motion", value).apply();
    }

    public static void focusMotion(final View view, final float focusedScale) {
        view.setOnFocusChangeListener(
                new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean focused) {
                        v.animate().cancel();
                        float scale = focused && !reducedMotion(v.getContext()) ? focusedScale : 1f;
                        v.animate()
                                .scaleX(scale)
                                .scaleY(scale)
                                .translationZ(focused ? dp(v.getContext(), 6) : 0)
                                .setDuration(reducedMotion(v.getContext()) ? 0 : 160)
                                .start();
                    }
                });
    }

    /** Palette + readable typography, including every child on utility screens. */
    public static Theme applyBase(Activity a, TextView titleView) {
        Theme t = current(a);
        View root = ((ViewGroup) a.findViewById(android.R.id.content)).getChildAt(0);
        if (root != null) {
            root.setBackground(Glass.wallpaper(t));
            View content = a.findViewById(R.id.sidebar_content);
            if (content instanceof ViewGroup && ((ViewGroup) content).getChildCount() > 0)
                ((ViewGroup) content).getChildAt(0).setBackgroundColor(t.bg);
            styleTree(root, t);
        }
        if (titleView != null) {
            titleView.setTextColor(t.text);
            titleView.setTextSize(30);
            titleView.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
        }
        return t;
    }

    public static void styleTree(View view, Theme t) {
        // Design 1 was not selected. Keep the permanent sidebar and its layout intact.
        if (view.getId() == R.id.sidebar) return;
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            text.setTypeface(android.graphics.Typeface.create("sans-serif", 0));
            text.setTextColor(t.text);
            if (view instanceof android.widget.Button) {
                text.setAllCaps(false);
                text.setTextSize(16);
                text.setBackground(tileBackground(view.getContext(), t));
                focusMotion(view, 1.015f);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleTree(group.getChildAt(i), t);
        }
    }

    /** Match the palette of all utility dialogs without changing their actions. */
    public static android.app.AlertDialog.Builder dialog(Context c) {
        return new android.app.AlertDialog.Builder(
                c, "light".equals(current(c).id) ? R.style.LeoDialogLight : R.style.LeoDialog) {
            @Override
            public android.app.AlertDialog create() {
                android.app.AlertDialog dialog = super.create();
                dialog.setOnShowListener(ignored -> Glass.dialogShown(dialog, c));
                return dialog;
            }
        };
    }

    /** Clip an app icon to the chosen shape. */
    public static Bitmap shapedIcon(Drawable d, int sizePx, String shape) {
        Bitmap src = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(src);
        android.graphics.Rect oldBounds = new android.graphics.Rect(d.getBounds());
        d.setBounds(0, 0, sizePx, sizePx);
        d.draw(c);
        d.setBounds(oldBounds);
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
