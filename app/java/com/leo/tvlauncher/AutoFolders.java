package com.leo.tvlauncher;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * One-tap auto-folders: sorts unorganized apps into Video / Music / Games /
 * Tools folders by matching package names and labels against keyword lists.
 * Favorites, hidden apps and apps already in a folder are left alone.
 */
public class AutoFolders {

    public static class Plan {
        public String folderName;
        public final List<AppEntry> apps = new ArrayList<>();
    }

    // Category name -> comma-separated keywords (matched case-insensitively
    // against "package label").
    private static final String[][] CATS = {
            {"\uD83C\uDFAC Video",
                    "youtube,netflix,primevideo,prime video,disney,hulu,plex,kodi,"
                    + "vlc,mxplayer,tubi,peacock,appletv,hbomax,paramount,"
                    + "crunchyroll,twitch,video,player,movie,film,cinema,iptv,stremio"},
            {"\uD83C\uDFB5 Music",
                    "spotify,music,pandora,deezer,tidal,tunein,iheart,"
                    + "podcast,radio,soundcloud,audible"},
            {"\uD83C\uDFAE Games",
                    "game,gaming,arcade,emulator,retroarch,ppsspp,nvidia games"},
            {"\uD83D\uDEE0 Tools",
                    "file manager,filemanager,explorer,browser,chrome,firefox,"
                    + "downloader,vpn,speedtest,cleaner,settings"},
    };

    /** Folders with at least this many apps are suggested. */
    private static final int MIN_APPS = 2;

    /** Build the folder plan for apps that aren't organized yet. */
    public static List<Plan> suggest(Context c) {
        List<Plan> plans = new ArrayList<>();
        for (String[] cat : CATS) {
            Plan p = new Plan();
            p.folderName = cat[0];
            plans.add(p);
        }
        Set<String> hidden = HomePrefs.hidden(c);
        for (AppEntry e : Apps.query(c)) {
            if (e.pkg == null) continue;
            if (hidden.contains(e.pkg)) continue;
            if (HomePrefs.isFav(c, e.pkg)) continue;
            if (FolderStore.folderOf(c, e.pkg) != null) continue;
            String hay = (e.pkg + " " + e.label).toLowerCase();
            for (int i = 0; i < CATS.length; i++) {
                boolean hit = false;
                for (String kw : CATS[i][1].split(",")) {
                    if (!kw.isEmpty() && hay.contains(kw)) {
                        hit = true;
                        break;
                    }
                }
                if (hit) {
                    plans.get(i).apps.add(e);
                    break;
                }
            }
        }
        List<Plan> out = new ArrayList<>();
        for (Plan p : plans) {
            if (p.apps.size() >= MIN_APPS) out.add(p);
        }
        return out;
    }

    /** Create the folders and move the apps. Returns the number of apps moved. */
    public static int apply(Context c, List<Plan> plans) {
        int moved = 0;
        for (Plan p : plans) {
            FolderStore.Folder f = FolderStore.create(c, p.folderName);
            for (AppEntry e : p.apps) {
                FolderStore.moveTo(c, e.pkg, f.id);
                moved++;
            }
        }
        return moved;
    }
}
