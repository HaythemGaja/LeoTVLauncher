package com.leo.tvlauncher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Home-screen prefs: favorites order, app order, hidden apps, re-lock, wallpaper. */
public class HomePrefs {

    private static final String PREFS = "leo_tv_launcher";

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<String> getList(Context c, String key) {
        List<String> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p(c).getString(key, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void setList(Context c, String key, List<String> l) {
        JSONArray a = new JSONArray();
        for (String s : l) a.put(s);
        p(c).edit().putString(key, a.toString()).apply();
    }

    // --- Favorites ---
    public static List<String> favOrder(Context c) {
        return getList(c, "fav_order");
    }

    public static boolean isFav(Context c, String pkg) {
        return favOrder(c).contains(pkg);
    }

    public static void setFav(Context c, String pkg, boolean fav) {
        List<String> f = favOrder(c);
        List<String> o = getList(c, "app_order");
        f.remove(pkg);
        o.remove(pkg);
        if (fav) f.add(pkg);
        else o.add(pkg);
        setList(c, "fav_order", f);
        setList(c, "app_order", o);
    }

    // --- Hidden ---
    public static Set<String> hidden(Context c) {
        return new HashSet<>(p(c).getStringSet("hidden", new HashSet<String>()));
    }

    public static void setHidden(Context c, String pkg, boolean hide) {
        Set<String> s = hidden(c);
        if (hide) s.add(pkg);
        else s.remove(pkg);
        p(c).edit().putStringSet("hidden", s).apply();
    }

    // --- Reorder (favorites list or main app list) ---
    public static void moveInOrder(Context c, String pkg, int dir) {
        String key = isFav(c, pkg) ? "fav_order" : "app_order";
        List<String> l = getList(c, key);
        int i = l.indexOf(pkg);
        if (i < 0) return;
        int j = i + dir;
        if (j < 0 || j >= l.size()) return;
        Collections.swap(l, i, j);
        setList(c, key, l);
    }

    // --- Re-lock timer (minutes, 0 = always ask) ---
    public static int relockMinutes(Context c) {
        return p(c).getInt("relock_min", 0);
    }
    public static void setRelockMinutes(Context c, int m) {
        p(c).edit().putInt("relock_min", m).apply();
    }

    public static long lastUnlock(Context c) {
        return p(c).getLong("last_unlock", 0);
    }

    public static void setLastUnlock(Context c, long t) {
        p(c).edit().putLong("last_unlock", t).apply();
    }

    // --- Wallpaper ---
    public static boolean wallpaperCustom(Context c) {
        return "custom".equals(p(c).getString("wallpaper", "theme"));
    }

    public static void setWallpaperMode(Context c, String mode) {
        p(c).edit().putString("wallpaper", mode).apply();
    }

    // --- Kids mode ---
    public static boolean kidsMode(Context c) {
        return p(c).getBoolean("kids_mode", false);
    }

    public static void setKidsMode(Context c, boolean on) {
        p(c).edit().putBoolean("kids_mode", on).apply();
    }

    public static List<String> kidsApps(Context c) {
        return getList(c, "kids_apps");
    }

    public static void setKidsApps(Context c, List<String> l) {
        setList(c, "kids_apps", l);
    }

    // --- Recents ---
    public static List<String> recents(Context c) {
        return getList(c, "recents");
    }

    public static void pushRecent(Context c, String pkg) {
        List<String> r = getList(c, "recents");
        r.remove(pkg);
        r.add(0, pkg);
        while (r.size() > 8) r.remove(r.size() - 1);
        setList(c, "recents", r);
    }

    // --- Custom app labels (rename) ---
    public static String customLabel(Context c, String pkg) {
        try {
            JSONObject o = new JSONObject(p(c).getString("custom_labels", "{}"));
            return o.isNull(pkg) ? null : o.optString(pkg, null);
        } catch (Exception e) {
            return null;
        }
    }

    public static void setCustomLabel(Context c, String pkg, String label) {
        try {
            JSONObject o = new JSONObject(p(c).getString("custom_labels", "{}"));
            if (label == null || label.trim().isEmpty()) o.remove(pkg);
            else o.put(pkg, label.trim());
            p(c).edit().putString("custom_labels", o.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static String displayLabel(Context c, AppEntry e) {
        if (e.pkg != null) {
            String custom = customLabel(c, e.pkg);
            if (custom != null) return custom;
        }
        return e.label;
    }

    // --- Sidebar customization ---
    public static final String[] DEFAULT_SIDEBAR =
            {"home", "favorites", "recent", "folders", "notifications",
             "quicksettings", "applock", "kids", "settings"};

    public static List<String> sidebarOrder(Context c) {
        List<String> o = getList(c, "sidebar_order");
        if (o.isEmpty()) {
            o = new ArrayList<>();
            Collections.addAll(o, DEFAULT_SIDEBAR);
        } else {
            for (String id : DEFAULT_SIDEBAR) if (!o.contains(id)) o.add(id);
        }
        return o;
    }

    public static void setSidebarOrder(Context c, List<String> o) {
        setList(c, "sidebar_order", o);
    }

    public static Set<String> sidebarHidden(Context c) {
        return new HashSet<>(p(c).getStringSet("sidebar_hidden", new HashSet<String>()));
    }

    public static void setSidebarHidden(Context c, String id, boolean hide) {
        Set<String> s = sidebarHidden(c);
        if (hide) s.add(id);
        else s.remove(id);
        p(c).edit().putStringSet("sidebar_hidden", s).apply();
    }

    // --- Lock Settings behind the PIN ---
    public static boolean settingsLocked(Context c) {
        return p(c).getBoolean("settings_lock", false);
    }

    public static void setSettingsLocked(Context c, boolean v) {
        p(c).edit().putBoolean("settings_lock", v).apply();
    }

    // --- App to open on TV boot (null = none) ---
    public static String bootApp(Context c) {
        return p(c).getString("boot_app", null);
    }

    public static void setBootApp(Context c, String pkg) {
        if (pkg == null) p(c).edit().remove("boot_app").apply();
        else p(c).edit().putString("boot_app", pkg).apply();
    }
}
