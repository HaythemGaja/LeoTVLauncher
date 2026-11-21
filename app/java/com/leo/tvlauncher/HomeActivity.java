package com.leo.tvlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lean, D-pad friendly TV home screen with a Roku-style left navigation bar. Plain GridLayout (no
 * GridView): every tile is a real focused view, so the remote's D-pad and OK button always work.
 * Long-press a tile to organize.
 */
public class HomeActivity extends Activity {

    private static final int REQ_EXIT_KIDS = 100;

    public static final String EXTRA_VIEW = "extra_view";
    public static final String EXTRA_ACTION = "extra_action";

    private GridLayout grid;
    private ImageView wallpaperView;
    private TextView hintView;
    private TextView emptyView;
    private TextView recentHeader;
    private HorizontalScrollView recentScroller;
    private LinearLayout recentRow;
    private LinearLayout sidebarBox;
    private Sidebar sidebar;
    private View root;
    private final Handler clockHandler = new Handler();
    private final List<Integer> favTileIndices = new ArrayList<>();
    private long lastHomePress = 0;
    private Theme theme;
    private int currentView = Sidebar.VIEW_HOME;
    // Live move mode state.
    private String movingPkg = null;
    private List<String> movingSnapshot = null;
    private int gridCols = 4;
    private int measuredGridWidth = -1;
    private final Map<String, AppEntry> catalog = new HashMap<>();
    private String lastFocusedTag;

    private final Runnable clockTick =
            new Runnable() {
                @Override
                public void run() {
                    if (sidebar != null) sidebar.tick();
                    clockHandler.postDelayed(this, 30000);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        root = findViewById(R.id.home_root);
        grid = findViewById(R.id.app_grid);
        sidebarBox = findViewById(R.id.sidebar);
        wallpaperView = findViewById(R.id.home_wallpaper);
        hintView = findViewById(R.id.home_hint);
        emptyView = findViewById(R.id.empty_view);
        recentHeader = findViewById(R.id.recent_header);
        recentScroller = findViewById(R.id.recent_scroller);
        recentRow = findViewById(R.id.recent_row);
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
                            buildGrid();
                        }
                    }
                });
        handleNavExtras(getIntent());
    }

    /** Jump to a sidebar view or trigger a sidebar action from another screen. */
    public static void goTo(android.content.Context c, int view, int action) {
        Intent i = new Intent(c, HomeActivity.class);
        i.putExtra(EXTRA_VIEW, view);
        i.putExtra(EXTRA_ACTION, action);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        c.startActivity(i);
    }

    /** Applies "view"/"action" extras sent by goTo(). */
    private void handleNavExtras(Intent intent) {
        if (intent == null) return;
        int view = intent.getIntExtra(EXTRA_VIEW, -1);
        int action = intent.getIntExtra(EXTRA_ACTION, Sidebar.ACT_NONE);
        intent.removeExtra(EXTRA_VIEW);
        intent.removeExtra(EXTRA_ACTION);
        if (view >= 0) {
            currentView = view;
            if (sidebar != null) {
                sidebar.setSelectedId(Sidebar.idForView(view));
                sidebar.build();
            }
            buildGrid();
            buildRecentRow();
        }
        if (action != Sidebar.ACT_NONE) handleAction(action);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
        clockTick.run();
        // Focus the first tile so the D-pad works immediately.
        grid.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (grid.getChildCount() > 0 && getCurrentFocus() == null) {
                            grid.getChildAt(0).requestFocus();
                        }
                    }
                });
    }

    private void reload() {
        View oldFocus = getCurrentFocus();
        if (oldFocus != null && oldFocus.getTag() instanceof String)
            lastFocusedTag = (String) oldFocus.getTag();
        theme = Theme.current(this);
        root.setBackground(Glass.wallpaper(theme));
        boolean kids = HomePrefs.kidsMode(this);
        recentHeader.setTextColor(theme.text2);
        recentHeader.setText("RECENT");
        recentHeader.setTextSize(13);
        recentHeader.setLetterSpacing(.12f);
        recentScroller.setBackground(Glass.panel(this, theme));
        recentScroller.setElevation(Theme.dp(this, 6));
        recentScroller.setPadding(
                Theme.dp(this, 10), Theme.dp(this, 6), Theme.dp(this, 10), Theme.dp(this, 2));
        hintView.setTextColor(theme.text2);
        emptyView.setTextColor(theme.text2);
        hintView.setText(
                movingPkg != null
                        ? "D-pad to move  •  OK to place  •  Back to cancel"
                        : (kids
                                ? "PIN required to exit kids mode"
                                : "◀ Menu  •  D-pad to move  •  OK to open  •  Hold OK to"
                                        + " organize"));
        catalog.clear();
        for (AppEntry e : Apps.query(this)) catalog.put(e.pkg, e);
        applyWallpaper();
        View scrim = findViewById(R.id.wallpaper_scrim);
        scrim.setVisibility(
                wallpaperView.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        int rgb = theme.bg & 0x00FFFFFF;
        android.graphics.drawable.GradientDrawable sideShade =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[] {rgb | 0xA0000000, rgb | 0x40000000});
        android.graphics.drawable.GradientDrawable lowerShade =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[] {rgb | 0x30000000, rgb | 0x80000000});
        scrim.setBackground(
                new android.graphics.drawable.LayerDrawable(
                        new android.graphics.drawable.Drawable[] {sideShade, lowerShade}));
        sidebar =
                new Sidebar(
                        this,
                        sidebarBox,
                        Sidebar.idForView(currentView),
                        new Sidebar.Listener() {
                            @Override
                            public void onView(int view) {
                                onSidebarView(view);
                            }

                            @Override
                            public void onAction(int action) {
                                handleAction(action);
                            }
                        });
        sidebar.build();
        buildGrid();
        buildRecentRow();
        if (lastFocusedTag != null) {
            if (lastFocusedTag.startsWith("side:")) sidebar.focusItem(lastFocusedTag.substring(5));
            else if (lastFocusedTag.startsWith("recent:")) {
                View previous = recentRow.findViewWithTag(lastFocusedTag);
                if (previous != null) previous.requestFocus();
            } else focusTile(lastFocusedTag);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockTick);
        if (movingPkg != null) exitMoveMode(true);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (movingPkg != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            int kc = event.getKeyCode();
            int dir = 0;
            if (kc == KeyEvent.KEYCODE_DPAD_LEFT) dir = -1;
            else if (kc == KeyEvent.KEYCODE_DPAD_RIGHT) dir = 1;
            else if (kc == KeyEvent.KEYCODE_DPAD_UP) dir = -gridCols;
            else if (kc == KeyEvent.KEYCODE_DPAD_DOWN) dir = gridCols;
            else if (kc == KeyEvent.KEYCODE_DPAD_CENTER
                    || kc == KeyEvent.KEYCODE_ENTER
                    || kc == KeyEvent.KEYCODE_BUTTON_A) {
                exitMoveMode(true);
                return true;
            } else if (kc == KeyEvent.KEYCODE_BACK) {
                exitMoveMode(false);
                return true;
            }
            if (dir != 0) {
                moveSelected(dir);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavExtras(intent);
        // Double-press of Home jumps to favorites.
        long now = System.currentTimeMillis();
        if (now - lastHomePress < 800 && !favTileIndices.isEmpty()) {
            final int idx = favTileIndices.get(0);
            grid.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (idx < grid.getChildCount()) grid.getChildAt(idx).requestFocus();
                        }
                    });
        }
        lastHomePress = now;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EXIT_KIDS && resultCode == RESULT_OK) {
            HomePrefs.setKidsMode(this, false);
            Toast.makeText(this, "Kids mode off", Toast.LENGTH_SHORT).show();
            reload();
        }
    }

    private void applyWallpaper() {
        wallpaperView.setImageDrawable(null);
        wallpaperView.setVisibility(View.GONE);
        if (!HomePrefs.wallpaperCustom(this)) return;
        File f = new File(getFilesDir(), "wallpaper.jpg");
        if (!f.exists()) return;
        Bitmap b = decodeSampled(f.getAbsolutePath(), 1920);
        if (b != null) {
            wallpaperView.setImageBitmap(b);
            wallpaperView.setVisibility(View.VISIBLE);
        }
    }

    static Bitmap decodeSampled(String path, int maxDim) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / sample > maxDim) sample *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            return BitmapFactory.decodeFile(path, o);
        } catch (Exception e) {
            return null;
        }
    }

    // --- Left navigation bar (built by the shared Sidebar helper) ---

    private void onSidebarView(int view) {
        currentView = view;
        if (sidebar != null) {
            String id = Sidebar.idForView(view);
            sidebar.setSelectedId(id);
            sidebar.build();
            sidebar.focusItem(id);
        }
        buildGrid();
        buildRecentRow();
    }

    /** Handles a sidebar action id (also used by goTo() from other screens). */
    private void handleAction(int action) {
        if (action == Sidebar.ACT_LOCK) {
            Intent i = new Intent(this, PinActivity.class);
            i.putExtra(
                    "mode", PinStore.hasPin(this) ? PinActivity.MODE_VERIFY : PinActivity.MODE_SET);
            startActivity(i);
        } else if (action == Sidebar.ACT_KIDS) {
            enterKidsMode();
        } else if (action == Sidebar.ACT_SETTINGS) {
            if (HomePrefs.settingsLocked(this) && PinStore.hasPin(this)) {
                Intent i = new Intent(this, PinActivity.class);
                i.putExtra("mode", PinActivity.MODE_VERIFY_SETTINGS);
                startActivity(i);
            } else {
                startActivity(new Intent(this, SettingsActivity.class));
            }
        } else if (action == Sidebar.ACT_NOTIFICATIONS) {
            startActivity(new Intent(this, NotificationsActivity.class));
        } else if (action == Sidebar.ACT_QUICK_SETTINGS) {
            startActivity(new Intent(this, QuickSettingsActivity.class));
        } else if (action == Sidebar.ACT_EXIT_KIDS) {
            Intent i = new Intent(this, PinActivity.class);
            i.putExtra("mode", PinActivity.MODE_VERIFY_EXIT_KIDS);
            startActivityForResult(i, REQ_EXIT_KIDS);
        }
    }

    // --- Content grid ---

    private void buildGrid() {
        View focused = getCurrentFocus();
        String restoreTag =
                focused != null && focused.getTag() instanceof String
                        ? (String) focused.getTag()
                        : lastFocusedTag;
        grid.removeAllViews();
        favTileIndices.clear();
        if (AppTiles.available(grid) <= 1) return; // Wait for the actual post-sidebar measurement.
        int cols = AppTiles.columns(this, grid);
        grid.setColumnCount(cols);
        gridCols = cols;

        List<AppEntry> tiles = new ArrayList<>();
        Set<String> hidden = HomePrefs.hidden(this);
        Map<String, AppEntry> byPkg = new HashMap<>();
        for (AppEntry e : catalog.values()) {
            e.favorite = HomePrefs.isFav(this, e.pkg);
            byPkg.put(e.pkg, e);
        }
        boolean kids = HomePrefs.kidsMode(this);

        if (kids) {
            for (String pkg : HomePrefs.kidsApps(this)) {
                AppEntry e = byPkg.get(pkg);
                if (e != null) tiles.add(e);
            }
        } else if (currentView == Sidebar.VIEW_FAVORITES) {
            for (String pkg : HomePrefs.favOrder(this)) {
                AppEntry e = byPkg.get(pkg);
                if (e != null && !hidden.contains(pkg) && FolderStore.folderOf(this, pkg) == null) {
                    e.favorite = true;
                    tiles.add(e);
                }
            }
            if (!tiles.isEmpty()) favTileIndices.add(0);
        } else if (currentView == Sidebar.VIEW_RECENT) {
            for (String pkg : HomePrefs.recents(this)) {
                AppEntry e = byPkg.get(pkg);
                if (e != null && !hidden.contains(pkg) && FolderStore.folderOf(this, pkg) == null) {
                    tiles.add(e);
                }
            }
        } else if (currentView == Sidebar.VIEW_FOLDERS) {
            for (FolderStore.Folder f : FolderStore.getFolders(this)) {
                AppEntry fe = new AppEntry();
                fe.folderId = f.id;
                fe.label = f.name;
                tiles.add(fe);
            }
        } else {
            // Home: favorites first, then folders, then the rest.
            List<String> favOrder = HomePrefs.favOrder(this);
            for (String pkg : favOrder) {
                AppEntry e = byPkg.get(pkg);
                if (e != null && !hidden.contains(pkg) && FolderStore.folderOf(this, pkg) == null) {
                    e.favorite = true;
                    favTileIndices.add(tiles.size());
                    tiles.add(e);
                }
            }

            for (FolderStore.Folder f : FolderStore.getFolders(this)) {
                AppEntry fe = new AppEntry();
                fe.folderId = f.id;
                fe.label = f.name;
                tiles.add(fe);
            }

            List<String> order = HomePrefs.getList(this, "app_order");
            boolean orderChanged = false;
            for (AppEntry e : sortedCatalog()) {
                if (!hidden.contains(e.pkg)
                        && FolderStore.folderOf(this, e.pkg) == null
                        && !favOrder.contains(e.pkg)
                        && !order.contains(e.pkg)) {
                    order.add(e.pkg);
                    orderChanged = true;
                }
            }
            if (orderChanged) HomePrefs.setList(this, "app_order", order);
            for (String pkg : order) {
                AppEntry e = byPkg.get(pkg);
                if (e != null
                        && !hidden.contains(pkg)
                        && FolderStore.folderOf(this, pkg) == null
                        && !favOrder.contains(pkg)) {
                    tiles.add(e);
                }
            }
        }

        gridCols =
                TileMetrics.homeColumns(
                        AppTiles.available(grid),
                        getResources().getDisplayMetrics().density,
                        Theme.iconSizeDp(this),
                        tiles.size());
        grid.setColumnCount(gridCols);
        for (AppEntry e : tiles) grid.addView(makeTile(e));
        if (restoreTag != null && (focused == null || !restoreTag.startsWith("side:")))
            focusTile(restoreTag);
        TextView title = findViewById(R.id.home_title);
        title.setText(
                kids
                        ? "Kids mode"
                        : currentView == Sidebar.VIEW_FAVORITES
                                ? "Favorites"
                                : currentView == Sidebar.VIEW_RECENT
                                        ? "Recent"
                                        : currentView == Sidebar.VIEW_FOLDERS ? "Folders" : "Home");
        title.setTextColor(theme.text);
        title.setTextSize(32);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));

        if (tiles.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            if (kids) {
                emptyView.setText(
                        "No apps allowed yet.\n"
                                + "Exit kids mode → Settings → Kids mode to choose apps.");
            } else if (currentView == Sidebar.VIEW_FAVORITES) {
                emptyView.setText("No favorites yet.\nHold OK on an app → ☆ Add to favorites");
            } else if (currentView == Sidebar.VIEW_RECENT) {
                emptyView.setText("Nothing recent yet.\nOpen some apps and they'll appear here.");
            } else if (currentView == Sidebar.VIEW_FOLDERS) {
                emptyView.setText(
                        "No folders yet.\nHold OK on an app → Create folder with this app");
            } else {
                emptyView.setText("No apps found.");
            }
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    private String tileTag(AppEntry e) {
        if (e.folderId != null) return "folder:" + e.folderId;
        if (e.pkg != null) return "pkg:" + e.pkg;
        return "tile:" + e.label;
    }

    private List<AppEntry> sortedCatalog() {
        List<AppEntry> result = new ArrayList<>(catalog.values());
        java.util.Collections.sort(
                result,
                new java.util.Comparator<AppEntry>() {
                    @Override
                    public int compare(AppEntry a, AppEntry b) {
                        return a.label.compareToIgnoreCase(b.label);
                    }
                });
        return result;
    }

    private View makeTile(final AppEntry e) {
        int width = TileMetrics.width(AppTiles.available(grid), gridCols, Theme.dp(this, 8));
        View v = AppTiles.create(this, e, width, catalog);
        v.setTag(tileTag(e));
        AppTiles.sizeGridTile(this, v, AppTiles.available(grid), gridCols);
        if (movingPkg != null && movingPkg.equals(e.pkg))
            v.setBackground(Theme.movingTileBackground(this, theme));
        v.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        lastFocusedTag = tileTag(e);
                        handleTileClick(e);
                    }
                });
        v.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        lastFocusedTag = tileTag(e);
                        return onTileLongPress(e);
                    }
                });
        return v;
    }

    private void buildRecentRow() {
        recentRow.removeAllViews();
        // The recent strip only appears on the Home view; the Recent view
        // shows the same apps as full-size tiles.
        boolean show = !HomePrefs.kidsMode(this) && currentView == Sidebar.VIEW_HOME;
        if (show) {
            Set<String> hidden = HomePrefs.hidden(this);
            Map<String, AppEntry> byPkg = new HashMap<>();
            for (AppEntry e : catalog.values()) byPkg.put(e.pkg, e);
            List<AppEntry> recents = new ArrayList<>();
            for (String pkg : HomePrefs.recents(this)) {
                AppEntry e = byPkg.get(pkg);
                if (e != null && !hidden.contains(pkg) && FolderStore.folderOf(this, pkg) == null) {
                    recents.add(e);
                }
            }
            for (AppEntry e : recents) recentRow.addView(makeRecentTile(e));
            show = !recents.isEmpty();
        }
        recentHeader.setVisibility(show ? View.VISIBLE : View.GONE);
        recentScroller.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private View makeRecentTile(final AppEntry e) {
        int width = Theme.dp(this, 136);
        View v = AppTiles.create(this, e, width, catalog);
        v.setTag("recent:" + e.pkg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, -2);
        lp.setMargins(Theme.dp(this, 8), Theme.dp(this, 8), Theme.dp(this, 8), Theme.dp(this, 12));
        v.setLayoutParams(lp);
        v.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        lastFocusedTag = "recent:" + e.pkg;
                        Apps.openApp(HomeActivity.this, e);
                    }
                });
        return v;
    }

    private void handleTileClick(AppEntry e) {
        if (e.folderId != null) {
            Intent i = new Intent(this, FolderActivity.class);
            i.putExtra("folder_id", e.folderId);
            startActivity(i);
        } else if (HomePrefs.kidsMode(this)) {
            // Allowed apps open directly in kids mode (no PIN prompt).
            Apps.launch(this, e);
        } else {
            Apps.openApp(this, e);
        }
    }

    private void enterKidsMode() {
        if (!PinStore.hasPin(this)) {
            Toast.makeText(
                            this,
                            "Set a PIN first — it's needed to exit kids mode",
                            Toast.LENGTH_LONG)
                    .show();
            Intent i = new Intent(this, PinActivity.class);
            i.putExtra("mode", PinActivity.MODE_SET);
            startActivity(i);
            return;
        }
        if (HomePrefs.kidsApps(this).isEmpty()) {
            Toast.makeText(
                            this,
                            "Choose kids apps first: sidebar → Settings → Kids mode",
                            Toast.LENGTH_LONG)
                    .show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        HomePrefs.setKidsMode(this, true);
        Toast.makeText(this, "Kids mode on", Toast.LENGTH_SHORT).show();
        reload();
    }

    // --- Long-press organize ---

    private boolean onTileLongPress(final AppEntry e) {
        if (HomePrefs.kidsMode(this)) return false;
        if (e.folderId != null) {
            showFolderMenu(e);
            return true;
        }
        showAppOrganizeMenu(e, currentView != Sidebar.VIEW_RECENT);
        return true;
    }

    private void showAppOrganizeMenu(final AppEntry e, boolean allowMove) {
        final boolean fav = HomePrefs.isFav(this, e.pkg);
        final boolean locked = PinStore.hasPin(this) && PinStore.lockedApps(this).contains(e.pkg);
        final boolean hasFolders = !FolderStore.getFolders(this).isEmpty();
        List<String> items = new ArrayList<>();
        if (allowMove) items.add("Move app");
        items.add(fav ? "★ Remove from favorites" : "☆ Add to favorites");
        items.add(locked ? "🔓 Unlock app" : "🔒 Lock app");
        items.add("Create folder with this app");
        if (hasFolders) items.add("Add to folder…");
        items.add("🗑 Uninstall");
        items.add("Hide app");
        items.add("✏️ Rename");
        if (HomePrefs.recents(this).contains(e.pkg)) items.add("🕘 Remove from recent");
        items.add("App info");
        Theme.dialog(this)
                .setTitle(e.label)
                .setItems(
                        items.toArray(new String[0]),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                String item =
                                        ((AlertDialog) d)
                                                .getListView()
                                                .getAdapter()
                                                .getItem(which)
                                                .toString();
                                onOrganizeItem(e, item);
                            }
                        })
                .show();
    }

    private void onOrganizeItem(AppEntry e, String item) {
        if (item.equals("Move app")) {
            enterMoveMode(e);
        } else if (item.startsWith("★") || item.startsWith("☆")) {
            HomePrefs.setFav(this, e.pkg, !HomePrefs.isFav(this, e.pkg));
            buildGrid();
            focusTile(tileTag(e));
            Toast.makeText(
                            this,
                            HomePrefs.isFav(this, e.pkg)
                                    ? "Added to favorites"
                                    : "Removed from favorites",
                            Toast.LENGTH_SHORT)
                    .show();
        } else if (item.startsWith("🔒") || item.startsWith("🔓")) {
            if (!PinStore.hasPin(this)) {
                Toast.makeText(this, "Set a PIN first (sidebar → App Lock)", Toast.LENGTH_LONG)
                        .show();
                Intent i = new Intent(this, PinActivity.class);
                i.putExtra("mode", PinActivity.MODE_SET);
                startActivity(i);
            } else {
                boolean nowLocked = !PinStore.lockedApps(this).contains(e.pkg);
                PinStore.setLocked(this, e.pkg, nowLocked);
                buildGrid();
                focusTile(tileTag(e));
                Toast.makeText(
                                this,
                                nowLocked ? e.label + " locked" : e.label + " unlocked",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (item.equals("Create folder with this app")) {
            promptNewFolder(e);
        } else if (item.equals("Add to folder…")) {
            promptAddToFolder(e);
        } else if (item.startsWith("🗑")) {
            uninstallApp(e);
        } else if (item.startsWith("✏️")) {
            promptRenameApp(e);
        } else if (item.startsWith("🕘")) {
            List<String> r = new ArrayList<>(HomePrefs.recents(this));
            r.remove(e.pkg);
            HomePrefs.setList(this, "recents", r);
            buildGrid();
            buildRecentRow();
            Toast.makeText(this, e.label + " removed from recent", Toast.LENGTH_SHORT).show();
        } else if (item.equals("Hide app")) {
            HomePrefs.setHidden(this, e.pkg, true);
            buildGrid();
            Toast.makeText(this, e.label + " hidden", Toast.LENGTH_SHORT).show();
        } else if (item.equals("App info")) {
            try {
                Intent i =
                        new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + e.pkg));
                startActivity(i);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, "App info not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uninstallApp(AppEntry e) {
        Apps.uninstall(
                this,
                e.pkg,
                e.label,
                new Runnable() {
                    @Override
                    public void run() {
                        buildGrid();
                        buildRecentRow();
                    }
                });
    }

    private void promptRenameApp(final AppEntry e) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        String cur = HomePrefs.customLabel(this, e.pkg);
        input.setText(cur != null ? cur : e.label);
        input.selectAll();
        Theme.dialog(this)
                .setTitle("Rename app")
                .setView(input)
                .setPositiveButton(
                        "Save",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                HomePrefs.setCustomLabel(
                                        HomeActivity.this, e.pkg, input.getText().toString());
                                buildGrid();
                                buildRecentRow();
                            }
                        })
                .setNeutralButton(
                        "Reset",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                HomePrefs.setCustomLabel(HomeActivity.this, e.pkg, null);
                                buildGrid();
                                buildRecentRow();
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- Live move mode ---

    private String orderKeyFor(String pkg) {
        return HomePrefs.isFav(this, pkg) ? "fav_order" : "app_order";
    }

    private void enterMoveMode(AppEntry e) {
        movingSnapshot = new ArrayList<>(HomePrefs.getList(this, orderKeyFor(e.pkg)));
        movingPkg = e.pkg;
        hintView.setText("D-pad to move  •  OK to place  •  Back to cancel");
        buildGrid();
        focusTile(tileTag(e));
    }

    private void moveSelected(int dir) {
        if (movingPkg == null) return;
        String key = orderKeyFor(movingPkg);
        List<String> l = HomePrefs.getList(this, key);
        int i = l.indexOf(movingPkg);
        if (i < 0) return;
        int j = i + dir;
        if (j < 0) j = 0;
        if (j >= l.size()) j = l.size() - 1;
        if (j == i) return;
        l.remove(i);
        l.add(j, movingPkg);
        HomePrefs.setList(this, key, l);
        buildGrid();
        focusTile("pkg:" + movingPkg);
    }

    private void exitMoveMode(boolean commit) {
        if (movingPkg == null) return;
        if (!commit && movingSnapshot != null) {
            HomePrefs.setList(this, orderKeyFor(movingPkg), movingSnapshot);
        }
        String tag = "pkg:" + movingPkg;
        movingPkg = null;
        movingSnapshot = null;
        reload();
        focusTile(tag);
    }

    private void promptNewFolder(final AppEntry e) {
        final EditText input = new EditText(this);
        input.setText("Folder " + FolderStore.nextFolderNumber(this));
        input.selectAll();
        Theme.dialog(this)
                .setTitle("New folder")
                .setView(input)
                .setPositiveButton(
                        "Create",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                String name = input.getText().toString().trim();
                                if (name.isEmpty()) name = "Folder";
                                FolderStore.Folder f = FolderStore.create(HomeActivity.this, name);
                                FolderStore.moveTo(HomeActivity.this, e.pkg, f.id);
                                buildGrid();
                                Toast.makeText(
                                                HomeActivity.this,
                                                e.label + " moved to " + name,
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptAddToFolder(final AppEntry e) {
        final List<FolderStore.Folder> folders = FolderStore.getFolders(this);
        String[] names = new String[folders.size()];
        for (int i = 0; i < folders.size(); i++) names[i] = folders.get(i).name;
        Theme.dialog(this)
                .setTitle("Add to folder")
                .setItems(
                        names,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                FolderStore.Folder f = folders.get(which);
                                FolderStore.moveTo(HomeActivity.this, e.pkg, f.id);
                                buildGrid();
                                Toast.makeText(
                                                HomeActivity.this,
                                                e.label + " moved to " + f.name,
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        })
                .show();
    }

    private void showFolderMenu(final AppEntry e) {
        final FolderStore.Folder f = FolderStore.get(this, e.folderId);
        if (f == null) return;
        Theme.dialog(this)
                .setTitle(f.name)
                .setItems(
                        new String[] {"Rename folder", "Delete folder"},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                if (which == 0) promptRenameFolder(f);
                                else confirmDeleteFolder(f);
                            }
                        })
                .show();
    }

    private void promptRenameFolder(final FolderStore.Folder f) {
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
                                    FolderStore.rename(HomeActivity.this, f.id, name);
                                    buildGrid();
                                }
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteFolder(final FolderStore.Folder f) {
        Theme.dialog(this)
                .setTitle("Delete folder?")
                .setMessage("Apps inside will return to the home screen.")
                .setPositiveButton(
                        "Delete",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                FolderStore.delete(HomeActivity.this, f.id);
                                buildGrid();
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void focusTile(final String tag) {
        grid.post(
                new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < grid.getChildCount(); i++) {
                            if (tag.equals(grid.getChildAt(i).getTag())) {
                                grid.getChildAt(i).requestFocus();
                                return;
                            }
                        }
                    }
                });
    }
}
