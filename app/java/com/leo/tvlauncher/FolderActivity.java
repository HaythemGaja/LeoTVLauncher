package com.leo.tvlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shows the apps inside one folder. Plain GridLayout for reliable D-pad focus. */
public class FolderActivity extends BaseSidebarActivity {

    private String folderId;
    private GridLayout grid;
    private TextView titleView;
    private TextView subtitleView;
    private final Map<String, Bitmap> iconCache = new HashMap<>();
    private Theme theme;
    private LayoutInflater inflater;

    @Override
    protected String sidebarSelectedId() {
        return "folders";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        folderId = getIntent().getStringExtra("folder_id");
        FolderStore.Folder existing = FolderStore.get(this, folderId);
        if (existing == null) {
            finish();
            return;
        }
        if (existing.pin != null && !getIntent().getBooleanExtra("verified", false)) {
            Intent i = new Intent(this, PinActivity.class);
            i.putExtra("mode", PinActivity.MODE_UNLOCK_FOLDER);
            i.putExtra("folder_id", folderId);
            startActivity(i);
            finish();
            return;
        }
        setSidebarContent(R.layout.activity_folder);
        inflater = LayoutInflater.from(this);
        titleView = findViewById(R.id.folder_title);
        subtitleView = findViewById(R.id.folder_subtitle);
        grid = findViewById(R.id.folder_grid);

        Button rename = findViewById(R.id.btn_rename);
        Button delete = findViewById(R.id.btn_delete_folder);
        Button pin = findViewById(R.id.btn_folder_pin);
        rename.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { promptRename(); }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmDelete(); }
        });
        pin.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { folderPinMenu(); }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FolderStore.Folder f = FolderStore.get(this, folderId);
        if (f == null) {
            finish();
            return;
        }
        theme = Theme.applyBase(this, titleView);
        subtitleView.setTextColor(theme.text2);
        titleView.setText(f.name);
        iconCache.clear();
        buildGrid(f);
        grid.post(new Runnable() {
            @Override public void run() {
                if (grid.getChildCount() > 0 && getCurrentFocus() == null) {
                    grid.getChildAt(0).requestFocus();
                }
            }
        });
    }

    private void buildGrid(FolderStore.Folder f) {
        grid.removeAllViews();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int cols = Math.max(3, (int) (dm.widthPixels / dm.density / 190));
        grid.setColumnCount(cols);

        Map<String, AppEntry> byPkg = new HashMap<>();
        for (AppEntry e : Apps.query(this)) byPkg.put(e.pkg, e);
        List<AppEntry> apps = new ArrayList<>();
        for (String pkg : f.apps) {
            AppEntry e = byPkg.get(pkg);
            if (e != null) apps.add(e);
        }
        subtitleView.setText(apps.size() + (apps.size() == 1 ? " app" : " apps"));

        String shape = Theme.iconShape(this);
        final int iconDp = Theme.iconSizeDp(this);
        for (final AppEntry e : apps) {
            View v = inflater.inflate(R.layout.item_app, grid, false);
            v.setFocusable(true);
            v.setFocusableInTouchMode(true);
            v.setClickable(true);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = Theme.dp(this, iconDp + 88);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(8, 8, 8, 8);
            v.setLayoutParams(lp);
            v.setBackground(Theme.tileBackground(this, theme));
            v.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { Apps.openApp(FolderActivity.this, e); }
            });
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    showAppMenu(e);
                    return true;
                }
            });
            ImageView icon = v.findViewById(R.id.app_icon);
            TextView emoji = v.findViewById(R.id.lock_emoji);
            TextView label = v.findViewById(R.id.app_label);
            emoji.setVisibility(View.GONE);
            icon.setVisibility(View.VISIBLE);
            label.setText(HomePrefs.displayLabel(this, e));
            label.setTextColor(theme.text);
            label.setTextSize(Theme.labelSizeSp(this));
            String key = e.pkg + "|" + shape + "|" + iconDp;
            Bitmap b = iconCache.get(key);
            if (b == null && e.icon != null) {
                b = Theme.shapedIcon(e.icon, Theme.dp(this, iconDp), shape);
                iconCache.put(key, b);
            }
            int px = Theme.dp(this, iconDp);
            icon.getLayoutParams().width = px;
            icon.getLayoutParams().height = px;
            icon.setImageBitmap(b);
            grid.addView(v);
        }
    }

    private void showAppMenu(final AppEntry e) {
        new AlertDialog.Builder(this)
                .setTitle(e.label)
                .setItems(new String[]{"Remove from folder", "🗑 Uninstall"},
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int which) {
                                if (which == 0) confirmRemoveFromFolder(e);
                                else uninstallApp(e);
                            }
                        })
                .show();
    }

    private void uninstallApp(AppEntry e) {
        Apps.uninstall(this, e.pkg, e.label);
    }

    private void confirmRemoveFromFolder(final AppEntry e) {
        new AlertDialog.Builder(this)
                .setTitle(e.label)
                .setMessage("Remove from this folder? It will return to the home screen.")
                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        FolderStore.moveTo(FolderActivity.this, e.pkg, null);
                        FolderStore.Folder f = FolderStore.get(FolderActivity.this, folderId);
                        if (f != null) buildGrid(f);
                        Toast.makeText(FolderActivity.this,
                                e.label + " moved to home", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptRename() {
        final FolderStore.Folder f = FolderStore.get(this, folderId);
        if (f == null) return;
        final EditText input = new EditText(this);
        input.setText(f.name);
        input.selectAll();
        new AlertDialog.Builder(this)
                .setTitle("Rename folder")
                .setView(input)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        String name = input.getText().toString().trim();
                        if (!name.isEmpty()) {
                            FolderStore.rename(FolderActivity.this, folderId, name);
                            titleView.setText(name);
                            Toast.makeText(FolderActivity.this, "Renamed", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void folderPinMenu() {
        final boolean has = FolderStore.hasPin(this, folderId);
        String[] items = has
                ? new String[]{"Change PIN", "Remove PIN"}
                : new String[]{"Set PIN"};
        new AlertDialog.Builder(this)
                .setTitle("Folder PIN")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (has && which == 1) {
                            FolderStore.clearPin(FolderActivity.this, folderId);
                            Toast.makeText(FolderActivity.this, "Folder PIN removed",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Intent i = new Intent(FolderActivity.this, PinActivity.class);
                            i.putExtra("mode", PinActivity.MODE_SET_FOLDER);
                            i.putExtra("folder_id", folderId);
                            startActivity(i);
                        }
                    }
                })
                .show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete folder?")
                .setMessage("Apps inside will go back to the home screen.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        FolderStore.delete(FolderActivity.this, folderId);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
