package com.leo.tvlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Customize the left navigation bar: show/hide each item and reorder them.
 * Plain rows for reliable D-pad focus.
 */
public class SidebarCustomActivity extends BaseSidebarActivity {

    private LinearLayout rows;
    private Theme theme;
    private LayoutInflater inflater;

    @Override
    protected String sidebarSelectedId() {
        return "settings";
    }

    // id -> display name (emoji matches the sidebar)
    private static String nameOf(String id) {
        return Sidebar.nameOf(id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSidebarContent(R.layout.activity_settings);
        ((TextView) findViewById(R.id.settings_title)).setText("Sidebar");
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
        final List<String> order = new ArrayList<>(HomePrefs.sidebarOrder(this));
        final Set<String> hidden = HomePrefs.sidebarHidden(this);
        for (int i = 0; i < order.size(); i++) {
            final String id = order.get(i);
            final int index = i;
            View v = inflater.inflate(R.layout.item_lock, rows, false);
            v.setFocusable(true);
            v.setFocusableInTouchMode(true);
            v.setClickable(true);
            v.setBackground(Theme.tileBackground(this, theme));
            v.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    showItemMenu(id, index, order.size());
                }
            });
            TextView label = v.findViewById(R.id.lock_label);
            TextView state = v.findViewById(R.id.lock_state);
            v.findViewById(R.id.lock_icon).setVisibility(View.GONE);
            label.setText(nameOf(id));
            label.setTextColor(theme.text);
            state.setText(hidden.contains(id) ? "hidden" : "shown");
            state.setTextColor(theme.text2);
            rows.addView(v);
        }
    }

    private void showItemMenu(final String id, final int index, final int count) {
        final boolean isHidden = HomePrefs.sidebarHidden(this).contains(id);
        List<String> items = new ArrayList<>();
        items.add(isHidden ? "Show" : "Hide");
        if (index > 0) items.add("Move up");
        if (index < count - 1) items.add("Move down");
        new AlertDialog.Builder(this)
                .setTitle(nameOf(id))
                .setItems(items.toArray(new String[0]),
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int which) {
                                String item = ((AlertDialog) d).getListView()
                                        .getAdapter().getItem(which).toString();
                                if ("Show".equals(item)) {
                                    HomePrefs.setSidebarHidden(
                                            SidebarCustomActivity.this, id, false);
                                } else if ("Hide".equals(item)) {
                                    HomePrefs.setSidebarHidden(
                                            SidebarCustomActivity.this, id, true);
                                } else {
                                    List<String> order = new ArrayList<>(
                                            HomePrefs.sidebarOrder(SidebarCustomActivity.this));
                                    int i = order.indexOf(id);
                                    int j = "Move up".equals(item) ? i - 1 : i + 1;
                                    if (i >= 0 && j >= 0 && j < order.size()) {
                                        Collections.swap(order, i, j);
                                        HomePrefs.setSidebarOrder(
                                                SidebarCustomActivity.this, order);
                                    }
                                }
                                buildRows();
                            }
                        })
                .show();
    }
}
