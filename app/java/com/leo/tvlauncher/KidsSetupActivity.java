package com.leo.tvlauncher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Choose which apps kids can open in kids mode. */
public class KidsSetupActivity extends BaseSidebarActivity {

    private LinearLayout list;
    private TextView titleView;
    private TextView subtitleView;
    private Theme theme;
    private final Map<String, Bitmap> iconCache = new HashMap<>();

    @Override
    protected String sidebarSelectedId() {
        return "settings";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSidebarContent(R.layout.activity_kids_setup);
        theme = Theme.current(this);
        titleView = findViewById(R.id.kids_title);
        subtitleView = findViewById(R.id.kids_subtitle);
        list = findViewById(R.id.kids_list);
        Theme.applyBase(this, titleView);
        subtitleView.setTextColor(theme.text2);
        buildList();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void buildList() {
        list.removeAllViews();
        final Set<String> allowed = new HashSet<>(HomePrefs.kidsApps(this));
        LayoutInflater inf = LayoutInflater.from(this);
        for (final AppEntry e : Apps.query(this)) {
            View row = inf.inflate(R.layout.item_lock, list, false);
            row.setFocusable(true);
            row.setFocusableInTouchMode(true);
            row.setClickable(true);
            row.setBackground(Theme.tileBackground(this, theme));
            final TextView label = row.findViewById(R.id.lock_label);
            final TextView check = row.findViewById(R.id.lock_state);
            ImageView icon = row.findViewById(R.id.lock_icon);
            label.setTextColor(theme.text);
            label.setText(e.label);
            String key = e.pkg + "|" + Theme.iconShape(this);
            Bitmap b = iconCache.get(key);
            if (b == null && e.icon != null) {
                b = Theme.shapedIcon(e.icon, Theme.dp(this, 48), Theme.iconShape(this));
                iconCache.put(key, b);
            }
            icon.setImageBitmap(b);
            updateCheck(check, allowed.contains(e.pkg));
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (allowed.contains(e.pkg)) allowed.remove(e.pkg);
                    else allowed.add(e.pkg);
                    HomePrefs.setKidsApps(KidsSetupActivity.this,
                            new java.util.ArrayList<>(allowed));
                    updateCheck(check, allowed.contains(e.pkg));
                }
            });
            list.addView(row);
        }
        if (list.getChildCount() > 0) {
            list.post(new Runnable() {
                @Override public void run() { list.getChildAt(0).requestFocus(); }
            });
        }
    }

    private void updateCheck(TextView check, boolean on) {
        check.setText(on ? "✓" : "");
        check.setTextColor(theme.accent);
    }
}
