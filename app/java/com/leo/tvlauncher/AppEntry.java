package com.leo.tvlauncher;

import android.graphics.drawable.Drawable;

/** One entry on the home grid: a real app or a folder tile. */
public class AppEntry {
    public String label;
    public String pkg;
    public Drawable icon;
    public Drawable banner; // activity banner, then application banner when available

    Integer brandTint; // cached once per current app catalog, never persisted

    public String folderId = null; // set for folder tiles
    public boolean favorite = false; // shown with a star on home

    public boolean isTile() {
        return folderId != null;
    }

    /** Emoji shown on folder tiles that have no app icon. */
    public String tileEmoji() {
        if (folderId != null) return "\uD83D\uDCC1";
        return "";
    }
}
