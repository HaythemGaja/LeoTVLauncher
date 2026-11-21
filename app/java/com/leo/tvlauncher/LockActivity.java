package com.leo.tvlauncher;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Pick which apps require the PIN. Reached only after PIN verify/set. */
public class LockActivity extends Activity {

    private LinearLayout rows;
    private Theme theme;
    private LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PinStore.hasPin(this) && !getIntent().getBooleanExtra("verified", false)) {
            Intent i = new Intent(this, PinActivity.class);
            i.putExtra("mode", PinActivity.MODE_VERIFY);
            startActivity(i);
            finish();
            return;
        }
        setContentView(R.layout.activity_lock);
        inflater = LayoutInflater.from(this);
        rows = findViewById(R.id.lock_rows);

        Button change = findViewById(R.id.btn_change_pin);
        Button remove = findViewById(R.id.btn_remove_pin);
        change.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(LockActivity.this, PinActivity.class);
                i.putExtra("mode", PinActivity.MODE_SET);
                startActivity(i);
            }
        });
        remove.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmRemovePin(); }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        theme = Theme.applyBase(this, (TextView) findViewById(R.id.lock_title));
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
        List<AppEntry> apps = new ArrayList<>(Apps.query(this));
        for (final AppEntry e : apps) {
            View v = inflater.inflate(R.layout.item_lock, rows, false);
            v.setFocusable(true);
            v.setFocusableInTouchMode(true);
            v.setClickable(true);
            v.setBackground(Theme.tileBackground(this, theme));
            v.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    int idx = rows.indexOfChild(view);
                    boolean locked = PinStore.lockedApps(LockActivity.this).contains(e.pkg);
                    PinStore.setLocked(LockActivity.this, e.pkg, !locked);
                    buildRows();
                    if (idx >= 0 && idx < rows.getChildCount()) {
                        rows.getChildAt(idx).requestFocus();
                    }
                }
            });
            ImageView icon = v.findViewById(R.id.lock_icon);
            TextView label = v.findViewById(R.id.lock_label);
            TextView state = v.findViewById(R.id.lock_state);
            label.setText(e.label);
            label.setTextColor(theme.text);
            if (e.icon != null) icon.setImageDrawable(e.icon);
            else icon.setImageDrawable(null);
            boolean locked = PinStore.lockedApps(this).contains(e.pkg);
            state.setText(locked ? "\uD83D\uDD12 Locked" : "\u25CB Unlocked");
            state.setTextColor(locked ? theme.accent : theme.text2);
            rows.addView(v);
        }
    }

    private void confirmRemovePin() {
        Theme.dialog(this)
                .setTitle("Remove PIN?")
                .setMessage("All app locks will be turned off.")
                .setPositiveButton(
                        "Remove",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                PinStore.clearPin(LockActivity.this);
                                for (String pkg : PinStore.lockedApps(LockActivity.this)) {
                                    PinStore.setLocked(LockActivity.this, pkg, false);
                                }
                                Toast.makeText(LockActivity.this, "PIN removed", Toast.LENGTH_SHORT)
                                        .show();
                                finish();
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
