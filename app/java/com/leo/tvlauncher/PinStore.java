package com.leo.tvlauncher;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/** Stores the PIN as a salted SHA-256 hash plus the set of locked packages. */
public class PinStore {
    private static final String PREFS = "leo_tv_launcher";
    private static final String KEY_HASH = "pin_hash";
    private static final String KEY_SALT = "pin_salt";
    private static final String KEY_LOCKED = "locked_apps";

    public static boolean hasPin(Context c) {
        return prefs(c).contains(KEY_HASH);
    }

    /** Create a portable "salt:hash" PIN token (used for the main PIN and folder PINs). */
    public static String makeHash(String pin) {
        String salt = Long.toHexString(System.currentTimeMillis())
                + Long.toHexString(Double.doubleToLongBits(Math.random()));
        return salt + ":" + sha(salt + "|" + pin);
    }

    public static boolean checkHash(String stored, String pin) {
        if (stored == null) return false;
        int i = stored.indexOf(':');
        if (i < 0) return false;
        String salt = stored.substring(0, i);
        return stored.equals(salt + ":" + sha(salt + "|" + pin));
    }

    public static void setPin(Context c, String pin) {
        prefs(c).edit().putString(KEY_HASH, makeHash(pin)).putInt("pin_ver", 2).apply();
    }

    /** True when a pre-v1.7 digit PIN exists that can never be entered again. */
    public static boolean needsMigration(Context c) {
        return hasPin(c) && prefs(c).getInt("pin_ver", 1) < 2;
    }

    /** One-time reset: drops the old digit PIN and old folder PINs. Keeps the locked-app list. */
    public static void migrate(Context c) {
        clearPin(c);
        FolderStore.clearAllPins(c);
        prefs(c).edit().putInt("pin_ver", 2).apply();
    }

    public static void clearPin(Context c) {
        prefs(c).edit().remove(KEY_HASH).remove(KEY_SALT).apply();
    }

    public static boolean checkPin(Context c, String pin) {
        SharedPreferences p = prefs(c);
        String stored = p.getString(KEY_HASH, "");
        if (stored.contains(":")) return checkHash(stored, pin);
        // Legacy format from v1.0.
        String salt = p.getString(KEY_SALT, "");
        return !stored.isEmpty() && stored.equals(sha(salt + "|" + pin));
    }

    public static Set<String> lockedApps(Context c) {
        return new HashSet<>(prefs(c).getStringSet(KEY_LOCKED, new HashSet<String>()));
    }

    public static void setLocked(Context c, String pkg, boolean locked) {
        Set<String> s = lockedApps(c);
        if (locked) s.add(pkg); else s.remove(pkg);
        prefs(c).edit().putStringSet(KEY_LOCKED, s).apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String sha(String s) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte[] b = d.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(b.length * 2);
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
