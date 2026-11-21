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
        GradientDrawable bg = Theme.surface(c, t.card, 0, 0);
        artwork.setBackground(entry.banner == null ? Glass.panel(c, t) : bg);
        artwork.setElevation(Theme.dp(c, 3));
        artwork.setClipToOutline(true);
        artwork.setDuplicateParentStateEnabled(true);
        android.graphics.drawable.StateListDrawable outline =
                new android.graphics.drawable.StateListDrawable();
        outline.addState(
                new int[] {android.R.attr.state_focused}, Theme.surface(c, 0, t.accent, 1));
        outline.addState(
                new int[] {android.R.attr.state_pressed}, Theme.surface(c, 0x18000000, 0, 0));
        outline.addState(new int[] {}, Theme.surface(c, 0, 0, 0));
        artwork.setForeground(outline);
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
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        label.setMaxLines(2);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setPadding(Theme.dp(c, 2), Theme.dp(c, 8), Theme.dp(c, 2), 0);
        // Reserve two scaled text lines so larger labels do not collide with the next row.
        label.setMinHeight((int) Math.ceil(label.getTextSize() * 2.35f) + Theme.dp(c, 8));
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
