package com.leo.tvlauncher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The sidebar's Notifications tab: every notification the TV has received (newest first). OK on one
 * to open its app, or clear them all.
 */
public class NotificationsActivity extends BaseSidebarActivity {

    private LinearLayout rows;
    private LayoutInflater inflater;

    @Override
    protected String sidebarSelectedId() {
        return "notifications";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSidebarContent(R.layout.activity_settings);
        ((TextView) findViewById(R.id.settings_title)).setText("Notifications");
        inflater = LayoutInflater.from(this);
        rows = findViewById(R.id.settings_rows);
        findViewById(R.id.settings_category_scroll).setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        theme = Theme.applyBase(this, (TextView) findViewById(R.id.settings_title));
        buildRows();
        rows.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (rows.getChildCount() > 0 && getCurrentFocus() == null) {
                            rows.getChildAt(0).requestFocus();
                        }
                    }
                });
    }

    private void buildRows() {
        rows.removeAllViews();
        if (!NotifListener.isEnabled(this)) {
            addRow(
                    "Notification access is off",
                    "Settings → System → Notification access to turn it on",
                    null,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openNotifSettings();
                        }
                    });
            return;
        }
        List<NotifListener.Note> notes = NotifListener.notes();
        if (notes.isEmpty()) {
            addRow("No notifications", "New notifications will appear here.", null, null);
            return;
        }
        addRow(
                "Clear all",
                notes.size() + " notification(s)",
                null,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        NotifListener.clearAll();
                        buildRows();
                        Toast.makeText(
                                        NotificationsActivity.this,
                                        "Notifications cleared",
                                        Toast.LENGTH_SHORT)
                                .show();
                    }
                });
        Map<String, String> labels = new HashMap<>();
        Map<String, android.graphics.drawable.Drawable> icons = new HashMap<>();
        PackageManager pm = getPackageManager();
        for (final NotifListener.Note n : notes) {
            String appLabel = labels.get(n.pkg);
            if (appLabel == null && !labels.containsKey(n.pkg)) {
                try {
                    android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(n.pkg, 0);
                    appLabel = pm.getApplicationLabel(ai).toString();
                    labels.put(n.pkg, appLabel);
                    try {
                        icons.put(n.pkg, pm.getApplicationIcon(ai));
                    } catch (Exception ignored) {
                    }
                } catch (Exception e) {
                    appLabel = n.pkg;
                    labels.put(n.pkg, appLabel);
                }
            }
            String sub = (n.text == null ? "" : n.text);
            if (sub.length() > 80) sub = sub.substring(0, 80) + "…";
            String when =
                    DateUtils.getRelativeTimeSpanString(
                                    n.when, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                            .toString();
            final String pkg = n.pkg;
            final String label = appLabel;
            addRow(
                    (n.title == null || n.title.isEmpty() ? label : n.title),
                    label + "  •  " + when + (sub.isEmpty() ? "" : "\n" + sub),
                    icons.get(n.pkg),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openApp(pkg, label);
                        }
                    });
        }
    }

    private void addRow(
            String title,
            String sub,
            android.graphics.drawable.Drawable icon,
            View.OnClickListener onClick) {
        rows.addView(UtilityRows.create(this, title, sub, icon, onClick));
    }

    private void openApp(String pkg, String label) {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch == null) launch = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (launch != null) {
                startActivity(launch);
                HomePrefs.pushRecent(this, pkg);
            } else {
                Toast.makeText(this, "Can't open " + label, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Can't open " + label, Toast.LENGTH_SHORT).show();
        }
    }

    private void openNotifSettings() {
        AccessSettings.notificationAccess(this);
    }
}
