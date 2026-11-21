package com.leo.tvlauncher;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
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
    private Theme theme;
    private int measuredGridWidth = -1;
    private String lastFocusedPkg;

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
        titleView = findViewById(R.id.folder_title);
        subtitleView = findViewById(R.id.folder_subtitle);
        grid = findViewById(R.id.folder_grid);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.addOnLayoutChangeListener(
                new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(
                            View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                        int width = AppTiles.available(grid);
                        if (width > 1 && width != measuredGridWidth) {
                            measuredGridWidth = width;
                            FolderStore.Folder f = FolderStore.get(FolderActivity.this, folderId);
                            if (f != null) buildGrid(f);
                        }
                    }
                });

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
        View focused = getCurrentFocus();
        String restore =
                focused != null && focused.getTag() instanceof String
                        ? (String) focused.getTag()
                        : lastFocusedPkg;
        grid.removeAllViews();
        if (AppTiles.available(grid) <= 1) return;
        int cols = AppTiles.columns(this, grid);
        grid.setColumnCount(cols);

        Map<String, AppEntry> byPkg = new HashMap<>();
        for (AppEntry e : Apps.query(this)) byPkg.put(e.pkg, e);
        List<AppEntry> apps = new ArrayList<>();
        for (String pkg : f.apps) {
            AppEntry e = byPkg.get(pkg);
            if (e != null) apps.add(e);
        }
        subtitleView.setText(apps.size() + (apps.size() == 1 ? " app" : " apps"));

        int width = TileMetrics.width(AppTiles.available(grid), cols, Theme.dp(this, 8));
        for (final AppEntry e : apps) {
            View v = AppTiles.create(this, e, width, byPkg);
            v.setTag(e.pkg);
            AppTiles.sizeGridTile(this, v, AppTiles.available(grid), cols);
            v.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            lastFocusedPkg = e.pkg;
                            Apps.openApp(FolderActivity.this, e);
                        }
                    });
            v.setOnLongClickListener(
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {
                            showAppMenu(e);
                            return true;
                        }
                    });
            grid.addView(v);
        }
        View previous = restore == null ? null : grid.findViewWithTag(restore);
        if (previous != null) previous.requestFocus();
    }

    private void showAppMenu(final AppEntry e) {
        Theme.dialog(this)
                .setTitle(e.label)
                .setItems(
                        new String[] {"Remove from folder", "🗑 Uninstall"},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
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
        Theme.dialog(this)
                .setTitle(e.label)
                .setMessage("Remove from this folder? It will return to the home screen.")
                .setPositiveButton(
                        "Remove",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                FolderStore.moveTo(FolderActivity.this, e.pkg, null);
                                FolderStore.Folder f =
                                        FolderStore.get(FolderActivity.this, folderId);
                                if (f != null) buildGrid(f);
                                Toast.makeText(
                                                FolderActivity.this,
                                                e.label + " moved to home",
                                                Toast.LENGTH_SHORT)
                                        .show();
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
        Theme.dialog(this)
                .setTitle("Rename folder")
                .setView(input)
                .setPositiveButton(
                        "Save",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                String name = input.getText().toString().trim();
                                if (!name.isEmpty()) {
                                    FolderStore.rename(FolderActivity.this, folderId, name);
                                    titleView.setText(name);
                                    Toast.makeText(
                                                    FolderActivity.this,
                                                    "Renamed",
                                                    Toast.LENGTH_SHORT)
                                            .show();
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
        Theme.dialog(this)
                .setTitle("Folder PIN")
                .setItems(
                        items,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                if (has && which == 1) {
                                    FolderStore.clearPin(FolderActivity.this, folderId);
                                    Toast.makeText(
                                                    FolderActivity.this,
                                                    "Folder PIN removed",
                                                    Toast.LENGTH_SHORT)
                                            .show();
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
        Theme.dialog(this)
                .setTitle("Delete folder?")
                .setMessage("Apps inside will go back to the home screen.")
                .setPositiveButton(
                        "Delete",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                FolderStore.delete(FolderActivity.this, folderId);
                                finish();
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
