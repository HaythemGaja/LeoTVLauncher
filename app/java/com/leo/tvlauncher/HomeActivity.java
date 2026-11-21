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
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.text.InputType;
import android.view.LayoutInflater;
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
 * Lean, D-pad friendly TV home screen with a Roku-style left navigation bar.
 * Plain GridLayout (no GridView): every tile is a real focused view, so the
 * remote's D-pad and OK button always work. Long-press a tile to organize.
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
    private final Map<String, Bitmap> iconCache = new HashMap<>();
    private final List<Integer> favTileIndices = new ArrayList<>();
    private long lastHomePress = 0;
    private Theme theme;
    private final LayoutInflater[] inflater = new LayoutInflater[1];
    private int currentView = Sidebar.VIEW_HOME;
    // Live move mode state.
    private String movingPkg = null;
    private List<String> movingSnapshot = null;
    private int gridCols = 4;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (sidebar != null) sidebar.tick();
            clockHandler.postDelayed(this, 30000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        inflater[0] = LayoutInflater.from(this);
        root = findViewById(R.id.home_root);
        grid = findViewById(R.id.app_grid);
        sidebarBox = findViewById(R.id.sidebar);
        wallpaperView = findViewById(R.id.home_wallpaper);
        hintView = findViewById(R.id.home_hint);
        emptyView = findViewById(R.id.empty_view);
        recentHeader = findViewById(R.id.recent_header);
        recentScroller = findViewById(R.id.recent_scroller);
        recentRow = findViewById(R.id.recent_row);
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
        grid.post(new Runnable() {
            @Override public void run() {
                if (grid.getChildCount() > 0 && getCurrentFocus() == null) {
                    grid.getChildAt(0).requestFocus();
                }
            }
        });
    }

    private void reload() {
        theme = Theme.current(this);
        root.setBackgroundColor(theme.bg);
        boolean kids = HomePrefs.kidsMode(this);
        recentHeader.setTextColor(theme.text2);
        hintView.setTextColor(theme.text2);
        emptyView.setTextColor(theme.text2);
        hintView.setText(movingPkg != null ? "D-pad to move  •  OK to place  •  Back to cancel"
                : (kids ? "PIN required to exit kids mode"
                : "◀ Menu  •  D-pad to move  •  OK to open  •  Hold OK to organize"));
        iconCache.clear();
        applyWallpaper();
        sidebar = new Sidebar(this, sidebarBox, Sidebar.idForView(currentView),
                new Sidebar.Listener() {
                    @Override public void onView(int view) { onSidebarView(view); }
                    @Override public void onAction(int action) { handleAction(action); }
                });
        sidebar.build();
        buildGrid();
        buildRecentRow();
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
            grid.post(new Runnable() {
                @Override public void run() {
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
            i.putExtra("mode", PinStore.hasPin(this) ? PinActivity.MODE_VERIFY : PinActivity.MODE_SET);
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
        grid.removeAllViews();
        favTileIndices.clear();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int cols = Math.max(3, (int) (dm.widthPixels / dm.density / 190));
        grid.setColumnCount(cols);
        gridCols = cols;

        List<AppEntry> tiles = new ArrayList<>();
        Set<String> hidden = HomePrefs.hidden(this);
        Map<String, AppEntry> byPkg = new HashMap<>();
        for (AppEntry e : Apps.query(this)) byPkg.put(e.pkg, e);
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
            for (AppEntry e : Apps.query(this)) {
                if (!hidden.contains(e.pkg) && FolderStore.folderOf(this, e.pkg) == null
                        && !favOrder.contains(e.pkg) && !order.contains(e.pkg)) {
                    order.add(e.pkg);
                    orderChanged = true;
                }
            }
            if (orderChanged) HomePrefs.setList(this, "app_order", order);
            for (String pkg : order) {
                AppEntry e = byPkg.get(pkg);
                if (e != null && !hidden.contains(pkg)
                        && FolderStore.folderOf(this, pkg) == null && !favOrder.contains(pkg)) {
                    tiles.add(e);
                }
            }
        }

        for (AppEntry e : tiles) grid.addView(makeTile(e));

        if (tiles.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            if (kids) {
                emptyView.setText("No apps allowed yet.\nExit kids mode → Settings → Kids mode to choose apps.");
            } else if (currentView == Sidebar.VIEW_FAVORITES) {
                emptyView.setText("No favorites yet.\nHold OK on an app → ☆ Add to favorites");
            } else if (currentView == Sidebar.VIEW_RECENT) {
                emptyView.setText("Nothing recent yet.\nOpen some apps and they'll appear here.");
            } else if (currentView == Sidebar.VIEW_FOLDERS) {
                emptyView.setText("No folders yet.\nHold OK on an app → Create folder with this app");
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

    private View makeTile(final AppEntry e) {
        View v = inflater[0].inflate(R.layout.item_app, grid, false);
        v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        v.setClickable(true);
        v.setTag(tileTag(e));
        int iconDp = Theme.iconSizeDp(this);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = Theme.dp(this, iconDp + 88);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(8, 8, 8, 8);
        v.setLayoutParams(lp);
        if (movingPkg != null && movingPkg.equals(e.pkg)) {
            v.setBackground(Theme.movingTileBackground(this, theme));
        } else {
            v.setBackground(Theme.tileBackground(this, theme));
        }
        v.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { handleTileClick(e); }
        });
        v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View view) { return onTileLongPress(e); }
        });

        ImageView icon = v.findViewById(R.id.app_icon);
        TextView emoji = v.findViewById(R.id.lock_emoji);
        TextView label = v.findViewById(R.id.app_label);
        label.setTextColor(theme.text);
        label.setTextSize(Theme.labelSizeSp(this));
        if (e.isTile()) {
            icon.setVisibility(View.GONE);
            emoji.setVisibility(View.VISIBLE);
            emoji.setText(e.tileEmoji());
            label.setText(e.label);
        } else {
            emoji.setVisibility(View.GONE);
            icon.setVisibility(View.VISIBLE);
            boolean locked = PinStore.hasPin(this)
                    && PinStore.lockedApps(this).contains(e.pkg);
            String prefix = (locked ? "🔒 " : "") + (e.favorite ? "★ " : "");
            label.setText(prefix + HomePrefs.displayLabel(this, e));
            int px = Theme.dp(this, iconDp);
            icon.getLayoutParams().width = px;
            icon.getLayoutParams().height = px;
            icon.setImageBitmap(shapedIcon(e, iconDp));
        }
        return v;
    }

    private Bitmap shapedIcon(AppEntry e, int iconDp) {
        String shape = Theme.iconShape(this);
        String key = e.pkg + "|" + shape + "|" + iconDp;
        Bitmap b = iconCache.get(key);
        if (b == null && e.icon != null) {
            b = Theme.shapedIcon(e.icon, Theme.dp(this, iconDp), shape);
            iconCache.put(key, b);
        }
        return b;
    }

    private void buildRecentRow() {
        recentRow.removeAllViews();
        // The recent strip only appears on the Home view; the Recent view
        // shows the same apps as full-size tiles.
        boolean show = !HomePrefs.kidsMode(this) && currentView == Sidebar.VIEW_HOME;
        if (show) {
            Set<String> hidden = HomePrefs.hidden(this);
            Map<String, AppEntry> byPkg = new HashMap<>();
            for (AppEntry e : Apps.query(this)) byPkg.put(e.pkg, e);
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
        View v = inflater[0].inflate(R.layout.item_app, recentRow, false);
        v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        v.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                Theme.dp(this, 132), Theme.dp(this, 108));
        lp.setMargins(6, 6, 6, 6);
        v.setLayoutParams(lp);
        v.setBackground(Theme.tileBackground(this, theme));
        v.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { Apps.openApp(HomeActivity.this, e); }
        });
        ImageView icon = v.findViewById(R.id.app_icon);
        TextView emoji = v.findViewById(R.id.lock_emoji);
        TextView label = v.findViewById(R.id.app_label);
        label.setTextColor(theme.text);
        label.setTextSize(Theme.labelSizeSp(this));
        label.setMaxLines(1);
        emoji.setVisibility(View.GONE);
        icon.setVisibility(View.VISIBLE);
        int px = Theme.dp(this, 52);
        icon.getLayoutParams().width = px;
        icon.getLayoutParams().height = px;
        icon.setImageBitmap(shapedIcon(e, 52));
        label.setText(HomePrefs.displayLabel(this, e));
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
            Toast.makeText(this, "Set a PIN first — it's needed to exit kids mode",
                    Toast.LENGTH_LONG).show();
            Intent i = new Intent(this, PinActivity.class);
            i.putExtra("mode", PinActivity.MODE_SET);
            startActivity(i);
            return;
        }
        if (HomePrefs.kidsApps(this).isEmpty()) {
            Toast.makeText(this, "Choose kids apps first: sidebar → Settings → Kids mode",
                    Toast.LENGTH_LONG).show();
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
        final boolean locked = PinStore.hasPin(this)
                && PinStore.lockedApps(this).contains(e.pkg);
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
        new AlertDialog.Builder(this)
                .setTitle(e.label)
                .setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        String item = ((AlertDialog) d).getListView()
                                .getAdapter().getItem(which).toString();
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
            Toast.makeText(this, HomePrefs.isFav(this, e.pkg)
                    ? "Added to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
        } else if (item.startsWith("🔒") || item.startsWith("🔓")) {
            if (!PinStore.hasPin(this)) {
                Toast.makeText(this, "Set a PIN first (sidebar → App Lock)",
                        Toast.LENGTH_LONG).show();
                Intent i = new Intent(this, PinActivity.class);
                i.putExtra("mode", PinActivity.MODE_SET);
                startActivity(i);
            } else {
                boolean nowLocked = !PinStore.lockedApps(this).contains(e.pkg);
                PinStore.setLocked(this, e.pkg, nowLocked);
                buildGrid();
                focusTile(tileTag(e));
                Toast.makeText(this, nowLocked ? e.label + " locked" : e.label + " unlocked",
                        Toast.LENGTH_SHORT).show();
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
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + e.pkg));
                startActivity(i);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, "App info not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uninstallApp(AppEntry e) {
        Apps.uninstall(this, e.pkg, e.label, new Runnable() {
            @Override public void run() {
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
        new AlertDialog.Builder(this)
                .setTitle("Rename app")
                .setView(input)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        HomePrefs.setCustomLabel(HomeActivity.this, e.pkg,
                                input.getText().toString());
                        buildGrid();
                        buildRecentRow();
                    }
                })
                .setNeutralButton("Reset", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
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
        new AlertDialog.Builder(this)
                .setTitle("New folder")
                .setView(input)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) name = "Folder";
                        FolderStore.Folder f = FolderStore.create(HomeActivity.this, name);
                        FolderStore.moveTo(HomeActivity.this, e.pkg, f.id);
                        buildGrid();
                        Toast.makeText(HomeActivity.this,
                                e.label + " moved to " + name, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptAddToFolder(final AppEntry e) {
        final List<FolderStore.Folder> folders = FolderStore.getFolders(this);
        String[] names = new String[folders.size()];
        for (int i = 0; i < folders.size(); i++) names[i] = folders.get(i).name;
        new AlertDialog.Builder(this)
                .setTitle("Add to folder")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        FolderStore.Folder f = folders.get(which);
                        FolderStore.moveTo(HomeActivity.this, e.pkg, f.id);
                        buildGrid();
                        Toast.makeText(HomeActivity.this,
                                e.label + " moved to " + f.name, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showFolderMenu(final AppEntry e) {
        final FolderStore.Folder f = FolderStore.get(this, e.folderId);
        if (f == null) return;
        new AlertDialog.Builder(this)
                .setTitle(f.name)
                .setItems(new String[]{"Rename folder", "Delete folder"},
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int which) {
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
        new AlertDialog.Builder(this)
                .setTitle("Rename folder")
                .setView(input)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
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
        new AlertDialog.Builder(this)
                .setTitle("Delete folder?")
                .setMessage("Apps inside will return to the home screen.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        FolderStore.delete(HomeActivity.this, f.id);
                        buildGrid();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void focusTile(final String tag) {
        grid.post(new Runnable() {
            @Override public void run() {
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
