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
        Theme t = Theme.current(c);
        LinearLayout tile = new LinearLayout(c);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(Theme.dp(c, 6), Theme.dp(c, 6), Theme.dp(c, 6), Theme.dp(c, 8));
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        tile.setBackground(Theme.tileBackground(c, t));
        Theme.focusMotion(tile, 1.035f);
        int artWidth = Math.max(1, width - Theme.dp(c, 12));
        FrameLayout artwork = new FrameLayout(c);
        LinearLayout.LayoutParams artLp =
                new LinearLayout.LayoutParams(-1, Math.max(1, artWidth * 9 / 16));
        artwork.setLayoutParams(artLp);
        GradientDrawable bg =
                new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR, new int[] {t.cardFocus, t.card});
        bg.setCornerRadius(Theme.dp(c, 12));
        artwork.setBackground(bg);
        artwork.setClipToOutline(true);
        tile.addView(artwork);
        if (entry.folderId != null) {
            bindFolder(c, artwork, entry, catalog, t);
        } else {
            ImageView image = new ImageView(c);
            FrameLayout.LayoutParams imageLp;
            if (entry.banner != null) {
                imageLp = new FrameLayout.LayoutParams(-1, -1);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setImageDrawable(entry.banner);
            } else {
                int size =
                        Math.min(Theme.dp(c, Theme.iconSizeDp(c)), artLp.height - Theme.dp(c, 16));
                size = Math.max(Theme.dp(c, 24), size);
                imageLp = new FrameLayout.LayoutParams(size, size, Gravity.CENTER);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                if (entry.icon != null)
                    image.setImageBitmap(Theme.shapedIcon(entry.icon, size, Theme.iconShape(c)));
                else image.setImageResource(R.drawable.ic_launcher);
            }
            artwork.addView(image, imageLp);
        }
        LinearLayout badges = new LinearLayout(c);
        badges.setOrientation(LinearLayout.HORIZONTAL);
        boolean locked =
                entry.folderId != null
                        ? folderLocked(c, entry.folderId)
                        : PinStore.hasPin(c) && PinStore.lockedApps(c).contains(entry.pkg);
        if (locked) addBadge(c, badges, new LeoIcon(LeoIcon.LOCK, t.accent));
        if (entry.favorite) addBadge(c, badges, new LeoIcon(LeoIcon.STAR, t.accent));
        FrameLayout.LayoutParams badgesLp =
                new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        badgesLp.setMargins(0, Theme.dp(c, 7), Theme.dp(c, 7), 0);
        artwork.addView(badges, badgesLp);
        TextView label = new TextView(c);
        label.setText(entry.folderId != null ? entry.label : HomePrefs.displayLabel(c, entry));
        label.setTextColor(t.text);
        label.setTextSize(Theme.labelSizeSp(c));
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        label.setMaxLines(2);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setPadding(Theme.dp(c, 5), Theme.dp(c, 10), Theme.dp(c, 5), 0);
        // Reserve two scaled text lines so larger labels do not collide with the next row.
        label.setMinHeight((int) Math.ceil(label.getTextSize() * 2.5f) + Theme.dp(c, 10));
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.addView(label, new LinearLayout.LayoutParams(-1, -2));
        tile.setContentDescription(
                label.getText()
                        + (locked ? ", locked" : "")
                        + (entry.favorite ? ", favorite" : "")
                        + (entry.folderId != null ? ", folder" : ""));
        return tile;
    }

    private static boolean folderLocked(Context c, String id) {
        FolderStore.Folder f = FolderStore.get(c, id);
        return f != null && f.pin != null;
    }

    private static void addBadge(Context c, LinearLayout badges, Drawable icon) {
        ImageView v = new ImageView(c);
        v.setImageDrawable(icon);
        int p = Theme.dp(c, 5);
        v.setPadding(p, p, p, p);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6101820);
        bg.setCornerRadius(Theme.dp(c, 7));
        v.setBackground(bg);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(Theme.dp(c, 28), Theme.dp(c, 28));
        lp.setMarginStart(Theme.dp(c, 4));
        badges.addView(v, lp);
    }

    private static void bindFolder(
            Context c, FrameLayout art, AppEntry e, Map<String, AppEntry> catalog, Theme t) {
        FolderStore.Folder folder = FolderStore.get(c, e.folderId);
        LinearLayout rows = new LinearLayout(c);
        rows.setOrientation(LinearLayout.VERTICAL);
        int p = Theme.dp(c, 9);
        rows.setPadding(p, p, p, p);
        art.addView(rows, new FrameLayout.LayoutParams(-1, -1));
        int index = 0;
        for (int y = 0; y < 2; y++) {
            LinearLayout row = new LinearLayout(c);
            rows.addView(row, new LinearLayout.LayoutParams(-1, 0, 1));
            for (int x = 0; x < 2; x++) {
                AppEntry app = null;
                while (folder != null && index < folder.apps.size() && app == null)
                    app = catalog.get(folder.apps.get(index++));
                ImageView image = new ImageView(c);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                int pad = Theme.dp(c, 4);
                image.setPadding(pad, pad, pad, pad);
                if (app != null) image.setImageDrawable(app.banner != null ? app.banner : app.icon);
                else image.setImageDrawable(new LeoIcon(LeoIcon.FOLDER, t.text2));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
                lp.setMargins(pad, pad, pad, pad);
                row.addView(image, lp);
            }
        }
    }
}
