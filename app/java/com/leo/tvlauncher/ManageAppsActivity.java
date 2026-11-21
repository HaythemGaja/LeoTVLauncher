package com.leo.tvlauncher;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Full list of installed apps with Uninstall / App info / Hide actions.
 * Plain rows for reliable D-pad focus. Refreshes on resume so uninstalled
 * apps disappear when coming back from the system uninstall screen.
 */
public class ManageAppsActivity extends BaseSidebarActivity {

    private LinearLayout rows;
    private Theme theme;
    private LayoutInflater inflater;

    @Override
    protected String sidebarSelectedId() {
        return "settings";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSidebarContent(R.layout.activity_settings);
        ((TextView) findViewById(R.id.settings_title)).setText("Manage apps");
        inflater = LayoutInflater.from(this);
        rows = findViewById(R.id.settings_rows);
    }

    @Override
    protected void onResume() {
        super.onResume();
        theme = Theme.applyBase(this, (TextView) findViewById(R.id.settings_title));
        buildRows();
        rows.post(new Runnable() {
            @Override public void run() {
                if (rows.getChildCount() > 0 && getCurrentFocus() == null) {
                    rows.getChildAt(0).requestFocus();
                }
            }
        });
    }

    private void buildRows() {
        rows.removeAllViews();
        List<AppEntry> apps = Apps.query(this);
        for (final AppEntry e : apps) {
            View v = inflater.inflate(R.layout.item_lock, rows, false);
            v.setFocusable(true);
            v.setFocusableInTouchMode(true);
            v.setClickable(true);
            v.setBackground(Theme.tileBackground(this, theme));
            v.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { showAppMenu(e); }
            });
            ImageView icon = v.findViewById(R.id.lock_icon);
            TextView label = v.findViewById(R.id.lock_label);
            TextView state = v.findViewById(R.id.lock_state);
            label.setText(HomePrefs.displayLabel(this, e));
            label.setTextColor(theme.text);
            icon.setVisibility(View.VISIBLE);
            icon.setImageDrawable(e.icon);
            boolean hidden = HomePrefs.hidden(this).contains(e.pkg);
            boolean locked = PinStore.hasPin(this)
                    && PinStore.lockedApps(this).contains(e.pkg);
            StringBuilder sb = new StringBuilder();
            if (locked) sb.append("🔒 ");
            if (hidden) sb.append("hidden");
            state.setText(sb.toString());
            state.setTextColor(theme.text2);
            rows.addView(v);
        }
    }

    private void showAppMenu(final AppEntry e) {
        final boolean hidden = HomePrefs.hidden(this).contains(e.pkg);
        String[] items = {
                "🗑 Uninstall",
                "App info",
                hidden ? "Unhide app" : "Hide app",
        };
        Theme.dialog(this)
                .setTitle(e.label)
                .setItems(
                        items,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                if (which == 0) {
                                    Apps.uninstall(
                                            ManageAppsActivity.this,
                                            e.pkg,
                                            e.label,
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    buildRows();
                                                }
                                            });
                                } else if (which == 1) {
                                    try {
                                        startActivity(
                                                new Intent(
                                                        Settings
                                                                .ACTION_APPLICATION_DETAILS_SETTINGS,
                                                        Uri.parse("package:" + e.pkg)));
                                    } catch (ActivityNotFoundException ex) {
                                        Toast.makeText(
                                                        ManageAppsActivity.this,
                                                        "App info not available",
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                } else {
                                    HomePrefs.setHidden(ManageAppsActivity.this, e.pkg, !hidden);
                                    buildRows();
                                }
                            }
                        })
                .show();
    }
}
