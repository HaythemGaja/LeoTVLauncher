package com.leo.tvlauncher;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the TV's notifications so the sidebar's Notifications tab can
 * show them. The user grants access once via Settings → General →
 * "Notification access". Kept in memory (last 50), newest first.
 */
public class NotifListener extends NotificationListenerService {

    public static class Note {
        public String key;
        public String pkg;
        public String title;
        public String text;
        public long when;
    }

    private static final List<Note> NOTES = new ArrayList<>();
    private static final int MAX = 50;
    private static NotifListener instance;

    public static List<Note> notes() {
        synchronized (NOTES) {
            return new ArrayList<>(NOTES);
        }
    }

    /** True when the user has granted this app notification access. */
    public static boolean isEnabled(Context c) {
        try {
            String flat = android.provider.Settings.Secure.getString(
                    c.getContentResolver(), "enabled_notification_listeners");
            return flat != null && flat.contains(c.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    public static void clearAll() {
        synchronized (NOTES) {
            NOTES.clear();
        }
        try {
            if (instance != null) instance.cancelAllNotifications();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getPackageName() == null) return;
        String pkg = sbn.getPackageName();
        if (pkg.equals(getPackageName())) return; // ignore our own
        Notification n = sbn.getNotification();
        if (n == null) return;
        Bundle ex = n.extras;
        String title = ex != null && ex.getCharSequence(Notification.EXTRA_TITLE) != null
                ? ex.getCharSequence(Notification.EXTRA_TITLE).toString() : "";
        String text = ex != null && ex.getCharSequence(Notification.EXTRA_TEXT) != null
                ? ex.getCharSequence(Notification.EXTRA_TEXT).toString() : "";
        if (title.isEmpty() && text.isEmpty()) return;
        Note note = new Note();
        note.key = sbn.getKey();
        note.pkg = pkg;
        note.title = title;
        note.text = text;
        note.when = sbn.getPostTime();
        synchronized (NOTES) {
            // Replace an update to the same notification instead of duplicating.
            for (int i = 0; i < NOTES.size(); i++) {
                if (NOTES.get(i).key != null && NOTES.get(i).key.equals(note.key)) {
                    NOTES.remove(i);
                    break;
                }
            }
            NOTES.add(0, note);
            while (NOTES.size() > MAX) NOTES.remove(NOTES.size() - 1);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null || sbn.getKey() == null) return;
        synchronized (NOTES) {
            for (int i = 0; i < NOTES.size(); i++) {
                if (sbn.getKey().equals(NOTES.get(i).key)) {
                    NOTES.remove(i);
                    break;
                }
            }
        }
    }
}
