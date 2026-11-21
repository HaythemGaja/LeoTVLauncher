package com.leo.tvlauncher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Folders stored as JSON in SharedPreferences. */
public class FolderStore {

    public static class Folder {
        public String id;
        public String name;
        public List<String> apps = new ArrayList<>();
        public String pin; // nullable "salt:hash" token
    }

    private static final String PREFS = "leo_tv_launcher";
    private static final String KEY = "folders_json";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<Folder> getFolders(Context c) {
        List<Folder> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Folder f = new Folder();
                f.id = o.getString("id");
                f.name = o.getString("name");
                f.pin = o.isNull("pin") ? null : o.optString("pin", null);
                JSONArray apps = o.optJSONArray("apps");
                if (apps != null) {
                    for (int j = 0; j < apps.length(); j++) f.apps.add(apps.getString(j));
                }
                out.add(f);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static void save(Context c, List<Folder> folders) {
        try {
            JSONArray arr = new JSONArray();
            for (Folder f : folders) {
                JSONObject o = new JSONObject();
                o.put("id", f.id);
                o.put("name", f.name);
                if (f.pin != null) o.put("pin", f.pin);
                JSONArray apps = new JSONArray();
                for (String p : f.apps) apps.put(p);
                o.put("apps", apps);
                arr.put(o);
            }
            prefs(c).edit().putString(KEY, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static Folder get(Context c, String id) {
        for (Folder f : getFolders(c)) if (f.id.equals(id)) return f;
        return null;
    }

    public static Folder create(Context c, String name) {
        List<Folder> fs = getFolders(c);
        Folder f = new Folder();
        f.id = "f" + System.currentTimeMillis();
        f.name = name;
        fs.add(f);
        save(c, fs);
        return f;
    }

    public static void rename(Context c, String id, String name) {
        List<Folder> fs = getFolders(c);
        for (Folder f : fs) if (f.id.equals(id)) f.name = name;
        save(c, fs);
    }

    public static void delete(Context c, String id) {
        List<Folder> fs = getFolders(c);
        for (int i = fs.size() - 1; i >= 0; i--) {
            if (fs.get(i).id.equals(id)) fs.remove(i);
        }
        save(c, fs);
    }

    /** Which folder holds this package, or null. */
    public static String folderOf(Context c, String pkg) {
        for (Folder f : getFolders(c)) {
            if (f.apps.contains(pkg)) return f.id;
        }
        return null;
    }

    /** Move a package into a folder (null = back to home). Removes it elsewhere first. */
    public static void moveTo(Context c, String pkg, String folderId) {
        List<Folder> fs = getFolders(c);
        for (Folder f : fs) f.apps.remove(pkg);
        if (folderId != null) {
            for (Folder f : fs) {
                if (f.id.equals(folderId) && !f.apps.contains(pkg)) f.apps.add(pkg);
            }
        }
        save(c, fs);
    }

    public static int nextFolderNumber(Context c) {
        return getFolders(c).size() + 1;
    }

    // --- Per-folder PIN ---

    public static boolean hasPin(Context c, String id) {
        Folder f = get(c, id);
        return f != null && f.pin != null;
    }

    public static void setPin(Context c, String id, String hash) {
        List<Folder> fs = getFolders(c);
        for (Folder f : fs) if (f.id.equals(id)) f.pin = hash;
        save(c, fs);
    }

    public static void clearPin(Context c, String id) {
        setPin(c, id, null);
    }

    public static boolean checkPin(Context c, String id, String pin) {
        Folder f = get(c, id);
        return f != null && PinStore.checkHash(f.pin, pin);
    }

    /** Drop every folder PIN (used when the PIN system itself changes). */
    public static void clearAllPins(Context c) {
        List<Folder> fs = getFolders(c);
        for (Folder f : fs) f.pin = null;
        save(c, fs);
    }
}
