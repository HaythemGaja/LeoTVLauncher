package com.leo.tvlauncher;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;

/** Shared TV banner tile. Launch/organize callbacks remain owned by the activities. */
public final class AppTiles {
    private AppTiles() {}

    public static int available(GridLayout grid) {
        // The measured width already excludes sidebar and parent padding.
        return Math.max(1, grid.getWidth() - grid.getPaddingLeft() - grid.getPaddingRight());
    }

    public static int columns(Context c, GridLayout grid) {
        return TileMetrics.columns(
                available(grid), c.getResources().getDisplayMetrics().density, Theme.iconSizeDp(c));
    }

    public static void sizeGridTile(Context c, View tile, int available, int columns) {
        int margin = Theme.dp(c, 8);
        int width = TileMetrics.width(available, columns, margin);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = width;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED);
        lp.setMargins(margin, margin, margin, Theme.dp(c, 14));
        tile.setLayoutParams(lp);
    }

    public static View create(Context c, AppEntry entry, int width, Map<String, AppEntry> catalog) {
        return create(c, entry, width, catalog, false);
    }

    public static View create(
            Context c,
            AppEntry entry,
            int width,
            Map<String, AppEntry> catalog,
            boolean artworkLed) {
        Theme t = Theme.current(c);
        LinearLayout tile = new LinearLayout(c);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setClipChildren(false);
        tile.setClipToPadding(false);
        // The art is the only card surface. The label sits directly on the canvas.
        tile.setPadding(0, 0, 0, Theme.dp(c, 4));
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        Theme.focusMotion(tile, 1.025f);
        int artWidth = Math.max(1, width);
        FrameLayout artwork = new FrameLayout(c);
        LinearLayout.LayoutParams artLp =
                new LinearLayout.LayoutParams(-1, Math.max(1, artWidth * 9 / 16));
        artwork.setLayoutParams(artLp);
        GradientDrawable bg =
                Theme.surface(c, entry.folderId != null ? 0xFF202A32 : tint(entry), 0, 0);
        bg.setCornerRadius(Theme.dp(c, 12));
        artwork.setBackground(bg);
        artwork.setElevation(Theme.dp(c, 3));
        artwork.setClipToOutline(true);
        artwork.setDuplicateParentStateEnabled(true);
        android.graphics.drawable.StateListDrawable outline =
                new android.graphics.drawable.StateListDrawable();
        outline.addState(new int[] {android.R.attr.state_focused}, artSurface(c, 0, t.accent));
        outline.addState(new int[] {android.R.attr.state_pressed}, artSurface(c, 0x18000000, 0));
        outline.addState(new int[] {}, artSurface(c, 0, 0));
        artwork.setForeground(outline);
        tile.addView(artwork);
        if (entry.folderId != null) {
            bindFolder(c, artwork, entry, catalog, t);
        } else {
            if (entry.banner != null) {
                ImageView image = new ImageView(c);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setImageDrawable(entry.banner);
                artwork.addView(image, new FrameLayout.LayoutParams(-1, -1));
            } else if (artworkLed) {
                bindBrand(c, artwork, entry, artWidth, artLp.height);
            } else {
                ImageView image = new ImageView(c);
                int size =
                        Math.max(
                                Theme.dp(c, 24),
                                Math.min(
                                        Theme.dp(c, Theme.iconSizeDp(c)),
                                        artLp.height - Theme.dp(c, 16)));
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                if (entry.icon != null)
                    image.setImageBitmap(Theme.shapedIcon(entry.icon, size, Theme.iconShape(c)));
                else image.setImageResource(R.drawable.ic_launcher);
                artwork.addView(image, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
            }
        }
        LinearLayout badges = new LinearLayout(c);
        badges.setOrientation(LinearLayout.HORIZONTAL);
        boolean locked =
                entry.folderId != null
                        ? folderLocked(c, entry.folderId)
                        : PinStore.hasPin(c) && PinStore.lockedApps(c).contains(entry.pkg);
        if (locked) addBadge(c, badges, new LeoIcon(LeoIcon.LOCK, t.text2));
        if (entry.favorite) addBadge(c, badges, new LeoIcon(LeoIcon.STAR, t.text2));
        FrameLayout.LayoutParams badgesLp =
                new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        badgesLp.setMargins(0, Theme.dp(c, 7), Theme.dp(c, 7), 0);
        artwork.addView(badges, badgesLp);
        TextView label = new TextView(c);
        label.setText(entry.folderId != null ? entry.label : HomePrefs.displayLabel(c, entry));
        if (entry.folderId != null) {
            LeoIcon folderMark = new LeoIcon(LeoIcon.FOLDER, t.text2);
            int mark = Theme.dp(c, 16);
            folderMark.setBounds(0, 0, mark, mark);
            label.setCompoundDrawablesRelative(folderMark, null, null, null);
            label.setCompoundDrawablePadding(Theme.dp(c, 6));
        }
        label.setTextColor(t.text);
        label.setTextSize(Theme.labelSizeSp(c));
        label.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        label.setMaxLines(2);
        label.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
        label.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setPadding(Theme.dp(c, 2), Theme.dp(c, 8), Theme.dp(c, 2), 0);
        // Reserve two scaled text lines so larger labels do not collide with the next row.
        label.setMinHeight(
                (int) Math.ceil(label.getTextSize() * (artworkLed ? 1.35f : 2.35f))
                        + Theme.dp(c, 8));
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.addView(label, new LinearLayout.LayoutParams(-1, -2));
        tile.setContentDescription(
                label.getText()
                        + (locked ? ", locked" : "")
                        + (entry.favorite ? ", favorite" : "")
                        + (entry.folderId != null ? ", folder" : ""));
        return tile;
    }

    private static GradientDrawable artSurface(Context c, int fill, int stroke) {
        GradientDrawable drawable = Theme.surface(c, fill, stroke, stroke == 0 ? 0 : 1);
        drawable.setCornerRadius(Theme.dp(c, 12));
        return drawable;
    }

    private static int tint(AppEntry entry) {
        if (entry.brandTint != null) return entry.brandTint;
        int color = 0xFF252B32;
        if (entry.icon != null) {
            android.graphics.Bitmap sample =
                    android.graphics.Bitmap.createBitmap(
                            24, 24, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Rect bounds = new android.graphics.Rect(entry.icon.getBounds());
            try {
                entry.icon.setBounds(0, 0, 24, 24);
                entry.icon.draw(new android.graphics.Canvas(sample));
                int[] pixels = new int[24 * 24];
                sample.getPixels(pixels, 0, 24, 0, 0, 24, 24);
                color = BrandTint.fromPixels(pixels);
            } catch (RuntimeException unavailableIcon) {
                color = 0xFF252B32;
            } finally {
                entry.icon.setBounds(bounds);
                sample.recycle();
            }
        }
        entry.brandTint = color;
        return color;
    }

    private static void bindBrand(
            Context c, FrameLayout artwork, AppEntry entry, int width, int height) {
        LinearLayout mark = new LinearLayout(c);
        mark.setGravity(Gravity.CENTER);
        int inset = Theme.dp(c, 18);
        FrameLayout.LayoutParams markLp =
                new FrameLayout.LayoutParams(Math.max(1, width - inset * 2), -1, Gravity.CENTER);
        artwork.addView(mark, markLp);
        if (entry.icon != null) {
            ImageView logo = new ImageView(c);
            logo.setImageDrawable(entry.icon);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            // Optical logo size follows the art stage, not the tiny library-icon preference.
            int logoSize = Math.max(Theme.dp(c, 32), Math.min(height * 3 / 5, width / 4));
            LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(logoSize, logoSize);
            logoLp.setMarginEnd(Theme.dp(c, 10));
            mark.addView(logo, logoLp);
        }
        TextView wordmark = new TextView(c);
        wordmark.setText(HomePrefs.displayLabel(c, entry));
        wordmark.setTextColor(0xFFF7F8FA);
        wordmark.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        wordmark.setTextSize(width >= Theme.dp(c, 210) ? 20 : 18);
        wordmark.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
        wordmark.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        wordmark.setMaxLines(2);
        wordmark.setEllipsize(android.text.TextUtils.TruncateAt.END);
        mark.addView(wordmark, new LinearLayout.LayoutParams(0, -2, 1));
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    private static boolean folderLocked(Context c, String id) {
        FolderStore.Folder f = FolderStore.get(c, id);
        return f != null && f.pin != null;
    }

    private static void addBadge(Context c, LinearLayout badges, Drawable icon) {
        ImageView v = new ImageView(c);
        v.setImageDrawable(icon);
        int p = Theme.dp(c, 3);
        v.setPadding(p, p, p, p);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xD9101418);
        bg.setCornerRadius(Theme.dp(c, 7));
        v.setBackground(bg);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(Theme.dp(c, 22), Theme.dp(c, 22));
        lp.setMarginStart(Theme.dp(c, 4));
        badges.addView(v, lp);
    }

    private static void bindFolder(
            Context c, FrameLayout art, AppEntry e, Map<String, AppEntry> catalog, Theme t) {
        FolderStore.Folder folder = FolderStore.get(c, e.folderId);
        java.util.List<AppEntry> preview = new java.util.ArrayList<>();
        if (folder != null) {
            for (String pkg : folder.apps) {
                AppEntry app = catalog.get(pkg);
                if (app != null && preview.size() < 4) preview.add(app);
            }
        }
        // One matte cover; square logos keep a mixed folder visually consistent.
        LinearLayout mosaic = new LinearLayout(c);
        mosaic.setOrientation(LinearLayout.VERTICAL);
        int inset = Theme.dp(c, 8);
        mosaic.setPadding(inset, inset, inset, inset);
        FrameLayout.LayoutParams coverLp = new FrameLayout.LayoutParams(-1, -1);
        art.addView(mosaic, coverLp);
        int index = 0;
        for (int y = 0; y < 2; y++) {
            LinearLayout row = new LinearLayout(c);
            mosaic.addView(row, new LinearLayout.LayoutParams(-1, 0, 1));
            for (int x = 0; x < 2; x++) {
                FrameLayout cell = new FrameLayout(c);
                row.addView(cell, new LinearLayout.LayoutParams(0, -1, 1));
                if (index < preview.size()) {
                    AppEntry app = preview.get(index++);
                    ImageView logo = new ImageView(c);
                    logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    logo.setImageDrawable(app.icon);
                    // Fit within each cell even at the smallest card setting.
                    FrameLayout.LayoutParams logoLp = new FrameLayout.LayoutParams(-1, -1);
                    logoLp.setMargins(
                            Theme.dp(c, 4), Theme.dp(c, 2), Theme.dp(c, 4), Theme.dp(c, 2));
                    cell.addView(logo, logoLp);
                }
            }
        }
        if (preview.isEmpty()) {
            ImageView folderIcon = new ImageView(c);
            folderIcon.setImageDrawable(new LeoIcon(LeoIcon.FOLDER, t.text2));
            art.addView(
                    folderIcon,
                    new FrameLayout.LayoutParams(Theme.dp(c, 28), Theme.dp(c, 28), Gravity.CENTER));
        }
    }
}
