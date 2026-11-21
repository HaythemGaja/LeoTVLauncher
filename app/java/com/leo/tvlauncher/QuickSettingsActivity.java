package com.leo.tvlauncher;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The sidebar's Quick settings tab: one-press shortcuts to the system pages
 * people actually need on a TV, plus sleep / restart attempts.
 */
public class QuickSettingsActivity extends BaseSidebarActivity {

    private LinearLayout rows;
    private LayoutInflater inflater;

    private static class Shortcut {
        String emoji;
        String label;
        String sub;
        String action; // Settings.* action, or "sleep"/"restart"
    }

    @Override
    protected String sidebarSelectedId() {
        return "quicksettings";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSidebarContent(R.layout.activity_settings);
        ((TextView) findViewById(R.id.settings_title)).setText("Quick settings");
        inflater = LayoutInflater.from(this);
        rows = findViewById(R.id.settings_rows);
        findViewById(R.id.settings_category_scroll).setVisibility(View.GONE);
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

    private Shortcut sc(String emoji, String label, String sub, String action) {
        Shortcut s = new Shortcut();
        s.emoji = emoji;
        s.label = label;
        s.sub = sub;
        s.action = action;
        return s;
    }

    private void buildRows() {
        rows.removeAllViews();
        Shortcut[] list = {
            sc("📶", "Wi-Fi", "Networks & internet", Settings.ACTION_WIFI_SETTINGS),
            sc("🖥", "Display", "Brightness, HDR, resolution", Settings.ACTION_DISPLAY_SETTINGS),
            sc("🔊", "Sound", "Volume & audio output", Settings.ACTION_SOUND_SETTINGS),
            sc("💾", "Storage", "Free space on this TV", Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
            sc("🕒", "Date & time", "Clock settings", Settings.ACTION_DATE_SETTINGS),
            sc("ℹ", "About", "Device info", Settings.ACTION_DEVICE_INFO_SETTINGS),
            sc("💤", "Sleep TV", "Turn the screen off", "sleep"),
            sc("🔁", "Restart TV", "Reboot the device", "restart"),
        };
        int[] symbols = {
            LeoIcon.WIFI,
            LeoIcon.DISPLAY,
            LeoIcon.SOUND,
            LeoIcon.STORAGE,
            LeoIcon.CLOCK,
            LeoIcon.INFO,
            LeoIcon.POWER,
            LeoIcon.RESTART
        };
        for (int index = 0; index < list.length; index++) {
            final Shortcut s = list[index];
            rows.addView(
                    UtilityRows.create(
                            this,
                            s.label,
                            s.sub,
                            new LeoIcon(symbols[index], theme.text2),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    onShortcut(s);
                                }
                            }));
        }
    }

    private void onShortcut(Shortcut s) {
        if ("sleep".equals(s.action)) {
            try {
                // Raw action string: the constant is hidden in the SDK.
                startActivity(new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN"));
            } catch (Exception e) {
                Toast.makeText(this, "Use the power button on your remote",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }
        if ("restart".equals(s.action)) {
            try {
                Intent i = new Intent("android.intent.action.REBOOT");
                i.putExtra("nowait", 1);
                i.putExtra("interval", 1);
                i.putExtra("window", 0);
                startActivity(i);
            } catch (Exception e) {
                try {
                    startActivity(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
                    Toast.makeText(this, "Choose Restart on the About screen",
                            Toast.LENGTH_LONG).show();
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(this, "Hold the power button on your remote to restart",
                            Toast.LENGTH_LONG).show();
                }
            }
            return;
        }
        try {
            startActivity(new Intent(s.action));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, s.label + " not available on this TV",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
