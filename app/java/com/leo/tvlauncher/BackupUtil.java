package com.leo.tvlauncher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Backup & restore for every launcher setting (favorites, folders, hidden
 * apps, PIN, theme, sidebar, ...). The whole SharedPreferences file is
 * serialized to JSON so future settings are covered automatically.
 */
public class BackupUtil {

    private static final String PREFS = "leo_tv_launcher";
    private static final String APP_ID = "leo-tv-launcher";

    public static JSONObject export(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONObject out = new JSONObject();
        JSONObject data = new JSONObject();
        try {
            out.put("app", APP_ID);
            out.put("v", 1);
            for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
                Object v = e.getValue();
                if (v instanceof Set) {
                    JSONArray a = new JSONArray();
                    for (Object s : (Set<?>) v) a.put(String.valueOf(s));
                    data.put(e.getKey(), a);
                } else {
                    data.put(e.getKey(), JSONObject.wrap(v));
                }
            }
            out.put("data", data);
        } catch (Exception ignored) {
        }
        return out;
    }

    /** @return null on success, or an error message. */
    public static String importData(Context c, String json) {
        try {
            JSONObject backup = new JSONObject(json);
            if (!APP_ID.equals(backup.optString("app", ""))) {
                return "Not a Leo TV Launcher backup";
            }
            JSONObject data = backup.getJSONObject("data");
            SharedPreferences.Editor ed =
                    c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            ed.clear();
            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                Object v = data.get(k);
                if (v instanceof JSONArray) {
                    Set<String> s = new HashSet<>();
                    JSONArray a = (JSONArray) v;
                    for (int i = 0; i < a.length(); i++) s.add(a.getString(i));
                    ed.putStringSet(k, s);
                } else if (v instanceof Integer) {
                    ed.putInt(k, (Integer) v);
                } else if (v instanceof Long) {
                    ed.putLong(k, (Long) v);
                } else if (v instanceof Boolean) {
                    ed.putBoolean(k, (Boolean) v);
                } else if (v == JSONObject.NULL) {
                    ed.remove(k);
                } else {
                    ed.putString(k, String.valueOf(v));
                }
            }
            ed.apply();
            return null;
        } catch (Exception e) {
            return "Couldn't read that backup file";
        }
    }
}
