package com.leo.tvlauncher;

import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Theme, icon shape, wallpaper, re-lock timer. Plain rows for reliable D-pad. */
public class SettingsActivity extends BaseSidebarActivity {

    private static final int REQ_PICK_IMAGE = 101;
    private static final int REQ_HOME_ROLE = 102;
    private static final int REQ_BACKUP_CREATE = 103;
    private static final int REQ_RESTORE_OPEN = 104;

    private LinearLayout rows;
    private Theme theme;
    private String category = "Appearance";
    private static final String[] CATEGORIES = {
        "Appearance", "Layout & apps", "Family controls", "System", "Backup"
    };

    @Override
    protected String sidebarSelectedId() {
        return "settings";
    }

    private static class Row {
        String label;
        String kind; // theme, shape, wall, relock
        String value;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSidebarContent(R.layout.activity_settings);
        rows = findViewById(R.id.settings_rows);
        if (savedInstanceState != null)
            category = savedInstanceState.getString("category", "Appearance");
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
                            // Focus first real option, not the header.
                            for (int i = 0; i < rows.getChildCount(); i++) {
                                View v = rows.getChildAt(i);
                                if (v.isFocusable()) {
                                    v.requestFocus();
                                    break;
                                }
                            }
                        }
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("category", category);
    }

    private void buildRows() {
        View focused = getCurrentFocus();
        Object focusTag = focused == null ? null : focused.getTag();
        rows.removeAllViews();
        LinearLayout tabs = findViewById(R.id.settings_categories);
        tabs.removeAllViews();
        for (final String name : CATEGORIES) {
            TextView tab = new TextView(this);
            tab.setText(name);
            tab.setTextSize(15);
            tab.setTextColor(theme.text);
            tab.setPadding(
                    Theme.dp(this, 14), Theme.dp(this, 12), Theme.dp(this, 14), Theme.dp(this, 12));
            tab.setBackground(Theme.tileBackground(this, theme));
            tab.setSelected(name.equals(category));
            if (name.equals(category)) tab.setTextColor(theme.accent);
            tab.setFocusable(true);
            tab.setClickable(true);
            tab.setTag("category:" + name);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMarginEnd(Theme.dp(this, 8));
            tabs.addView(tab, lp);
            tab.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            category = name;
                            buildRows();
                        }
                    });
        }
        if ("Appearance".equals(category)) {
            addSummary("Theme", Theme.current(this).name, "choosetheme");
            addSummary("Icon fallback shape", shapeName(), "chooseshape");
            addSummary(
                    "Wallpaper",
                    HomePrefs.wallpaperCustom(this) ? "Custom image" : "Theme color",
                    "choosewall");
            addSummary("Reduce motion", Theme.reducedMotion(this) ? "On" : "Off", "reducemotion");
        } else if ("Layout & apps".equals(category)) {
            addSummary(
                    "Layout density",
                    Theme.iconSizeDp(this) + " dp icon fallback · cards scale together",
                    "choosesize");
            addSummary(
                    "App title size",
                    Theme.labelSizeAuto(this)
                            ? "Auto · readable at TV distance"
                            : Theme.labelSizeSp(this) + " sp",
                    "chooselabel");
            addSummary("Manage apps", "Favorites, hidden apps and names", "manageapps");
            addSummary("Customize sidebar", "Keep, hide and reorder destinations", "sidebaredit");
            addSummary("Auto-create folders", "Review suggestions before applying", "autofolders");
            addSummary("Open app on TV start", bootAppName(), "bootapp");
        } else if ("Family controls".equals(category)) {
            addSummary(
                    "Allowed kids apps", HomePrefs.kidsApps(this).size() + " selected", "kidsapps");
            addSummary(
                    "Lock settings with PIN",
                    HomePrefs.settingsLocked(this) ? "On" : "Off",
                    "settingslock");
            int minutes = HomePrefs.relockMinutes(this);
            addSummary(
                    "Re-lock locked apps",
                    minutes == 0 ? "Always ask for PIN" : "After " + minutes + " minutes",
                    "chooserelock");
        } else if ("System".equals(category)) {
            addSummary(
                    "Default Home app",
                    isDefaultHome()
                            ? "Leo TV is your default launcher"
                            : "Not Leo TV · select Leo TV in Android settings",
                    "sethome");
            addSummary("Android settings", "Open device settings", "syssettings");
            addSummary(
                    "Notification access",
                    NotifListener.isEnabled(this)
                            ? "Allowed · manage or revoke in Android settings"
                            : "Off · optional, read and clear TV notifications",
                    "notifaccess");
            addSummary(
                    "Access & device limits",
                    "What Leo needs and what Android controls",
                    "accessinfo");
            addSummary("Accessibility", "Open device accessibility settings", "accessibility");
            addSummary(
                    "Override current launcher",
                    "Review supported device options",
                    "overridelauncher");
        } else {
            addSummary("Backup settings to file", "Save your existing settings as JSON", "backup");
            addSummary(
                    "Restore settings from file",
                    "Replaces your current launcher settings",
                    "restore");
        }
        if (focusTag != null) {
            View previous = tabs.findViewWithTag(focusTag);
            if (previous == null) previous = rows.findViewWithTag(focusTag);
            if (previous != null) previous.requestFocus();
        }
    }

    private String shapeName() {
        String shape = Theme.iconShape(this);
        for (int i = 0; i < Theme.SHAPES.length; i++)
            if (shape.equals(Theme.SHAPES[i])) return Theme.SHAPE_NAMES[i];
        return "Rounded square";
    }

    private void addSummary(String title, String value, String kind) {
        final Row row = new Row();
        row.label = title;
        row.kind = kind;
        row.value = "";
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        int pad = Theme.dp(this, 18);
        v.setPadding(pad, Theme.dp(this, 14), pad, Theme.dp(this, 14));
        v.setBackground(Theme.tileBackground(this, theme));
        v.setFocusable(true);
        v.setClickable(true);
        v.setTag("setting:" + kind);
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(theme.text);
        label.setTextSize(19);
        label.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
        TextView summary = new TextView(this);
        summary.setText(value);
        summary.setTextColor(theme.text2);
        summary.setTextSize(15);
        summary.setPadding(0, Theme.dp(this, 5), 0, 0);
        v.addView(label);
        v.addView(summary);
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        summary.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        v.setContentDescription(title + ", " + value);
        Theme.focusMotion(v, 1.012f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = Theme.dp(this, 10);
        rows.addView(v, lp);
        v.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onRowClick(row);
                    }
                });
    }

    private void choose(
            final String title,
            final String kind,
            final String[] labels,
            final String[] values,
            String current) {
        int checked = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) checked = i;
        final int[] selected = {checked};
        Theme.dialog(this)
                .setTitle(title)
                .setSingleChoiceItems(
                        labels,
                        checked,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                selected[0] = which;
                            }
                        })
                .setPositiveButton(
                        "Apply",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                Row r = new Row();
                                r.kind = kind;
                                r.value = values[selected[0]];
                                onRowClick(r);
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** A staged live preview: the persistent theme changes only when Apply is pressed. */
    private void chooseTheme() {
        final int[] selected = {0};
        for (int i = 0; i < Theme.PRESETS.length; i++)
            if (Theme.PRESETS[i].id.equals(Theme.current(this).id)) selected[0] = i;
        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(
                Theme.dp(this, 24), Theme.dp(this, 20), Theme.dp(this, 24), Theme.dp(this, 20));
        final TextView heading = new TextView(this);
        heading.setText("Leo TV · Home");
        heading.setTextSize(25);
        preview.addView(heading);
        final TextView sample = new TextView(this);
        sample.setText("Favorites     Recent     Folders");
        sample.setTextSize(17);
        sample.setPadding(
                Theme.dp(this, 16), Theme.dp(this, 22), Theme.dp(this, 16), Theme.dp(this, 22));
        LinearLayout.LayoutParams sampleLp = new LinearLayout.LayoutParams(-1, -2);
        sampleLp.topMargin = Theme.dp(this, 18);
        preview.addView(sample, sampleLp);
        final Runnable paint =
                new Runnable() {
                    @Override
                    public void run() {
                        Theme t = Theme.PRESETS[selected[0]];
                        preview.setBackgroundColor(t.bg);
                        heading.setTextColor(t.text);
                        sample.setTextColor(t.text);
                        sample.setBackground(
                                Theme.surface(SettingsActivity.this, t.cardFocus, t.accent, 2));
                    }
                };
        paint.run();
        String[] names = new String[Theme.PRESETS.length];
        for (int i = 0; i < names.length; i++) names[i] = Theme.PRESETS[i].name;
        Theme.dialog(this)
                .setTitle("Theme preview")
                .setView(preview)
                .setSingleChoiceItems(
                        names,
                        selected[0],
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                selected[0] = which;
                                paint.run();
                            }
                        })
                .setPositiveButton(
                        "Apply",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                Row r = new Row();
                                r.kind = "theme";
                                r.value = Theme.PRESETS[selected[0]].id;
                                onRowClick(r);
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onRowClick(Row r) {
        if ("choosetheme".equals(r.kind)) {
            chooseTheme();
            return;
        }
        if ("chooseshape".equals(r.kind)) {
            choose(
                    "Icon fallback shape",
                    "shape",
                    Theme.SHAPE_NAMES,
                    Theme.SHAPES,
                    Theme.iconShape(this));
            return;
        }
        if ("choosesize".equals(r.kind)) {
            choose(
                    "Layout density",
                    "iconsize",
                    new String[] {
                        "Extra compact", "Compact", "Comfortable", "Large", "Extra large"
                    },
                    new String[] {"40", "56", "72", "88", "104"},
                    String.valueOf(Theme.iconSizeDp(this)));
            return;
        }
        if ("chooselabel".equals(r.kind)) {
            choose(
                    "App title size",
                    "labelsize",
                    new String[] {
                        "Auto",
                        "Small · 13 sp",
                        "Medium · 15 sp",
                        "Large · 18 sp",
                        "Extra large · 21 sp"
                    },
                    new String[] {"-1", "13", "15", "18", "21"},
                    Theme.labelSizeAuto(this) ? "-1" : String.valueOf(Theme.labelSizeSp(this)));
            return;
        }
        if ("chooserelock".equals(r.kind)) {
            choose(
                    "Re-lock locked apps",
                    "relock",
                    new String[] {
                        "Always ask",
                        "After 1 minute",
                        "After 5 minutes",
                        "After 15 minutes",
                        "After 1 hour"
                    },
                    new String[] {"0", "1", "5", "15", "60"},
                    String.valueOf(HomePrefs.relockMinutes(this)));
            return;
        }
        if ("choosewall".equals(r.kind)) {
            choose(
                    "Wallpaper",
                    "wall",
                    new String[] {"Theme color", "Custom image…"},
                    new String[] {"theme", "custom"},
                    HomePrefs.wallpaperCustom(this) ? "custom" : "theme");
            return;
        }
        if ("reducemotion".equals(r.kind)) {
            Theme.setReducedMotion(this, !Theme.reducedMotion(this));
            buildRows();
            return;
        }
        if ("sethome".equals(r.kind)) {
            openHomeSettings();
            return;
        } else if ("manageapps".equals(r.kind)) {
            startActivity(new Intent(this, ManageAppsActivity.class));
            return;
        } else if ("syssettings".equals(r.kind)) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, "System settings not available", Toast.LENGTH_SHORT).show();
            }
            return;
        } else if ("notifaccess".equals(r.kind)) {
            AccessSettings.notificationAccess(this);
            return;
        } else if ("accessinfo".equals(r.kind)) {
            Theme.dialog(this)
                    .setTitle("Access & device limits")
                    .setMessage(
                            "Home: choose Leo TV as the default Home app in Android's confirmation"
                                + " screen. Leo cannot force this on a TV that blocks other"
                                + " launchers.\n\n"
                                + "Notifications: optional manual access, only for the"
                                + " Notifications screen.\n\n"
                                + "Wallpaper and backup: you select individual files; no broad"
                                + " photo or storage grant is needed.\n\n"
                                + "Apps: Leo discovers launchable apps, not every installed"
                                + " package. Recents are apps opened through Leo, so usage access"
                                + " is not needed.\n\n"
                                + "Boot app: requires Leo as default Home. Android or the TV vendor"
                                + " may block opening an app at boot.\n\n"
                                + "Leo does not need overlay, usage access, an accessibility"
                                + " service or device administration. Its PIN and kids mode only"
                                + " protect navigation within Leo, not other launchers or Android"
                                + " settings. Sleep and restart may be blocked by the TV; extra"
                                + " permissions cannot make them universal.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        } else if ("accessibility".equals(r.kind)) {
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (ActivityNotFoundException ex) {
                toast("Accessibility settings not available");
            }
            return;
        } else if ("overridelauncher".equals(r.kind)) {
            overrideLauncher();
            return;
        } else if ("theme".equals(r.kind)) {
            Theme.setCurrent(this, r.value);
            toast("Theme applied");
        } else if ("shape".equals(r.kind)) {
            Theme.setIconShape(this, r.value);
            toast("Icon shape applied");
        } else if ("iconsize".equals(r.kind)) {
            Theme.setIconSizeDp(this, Integer.parseInt(r.value));
            toast("Icon size applied");
        } else if ("labelsize".equals(r.kind)) {
            int sp = Integer.parseInt(r.value);
            Theme.setLabelSizeSp(this, sp);
            toast(sp < 0 ? "Title size follows icon size" : "Title size applied");
        } else if ("kidsapps".equals(r.kind)) {
            startActivity(new Intent(this, KidsSetupActivity.class));
        } else if ("wall".equals(r.kind)) {
            if ("custom".equals(r.value)) {
                pickWallpaper();
                return;
            }
            HomePrefs.setWallpaperMode(this, "theme");
            toast("Wallpaper cleared");
        } else if ("relock".equals(r.kind)) {
            HomePrefs.setRelockMinutes(this, Integer.parseInt(r.value));
            toast("Re-lock timer updated");
        } else if ("sidebaredit".equals(r.kind)) {
            startActivity(new Intent(this, SidebarCustomActivity.class));
        } else if ("autofolders".equals(r.kind)) {
            promptAutoFolders();
            return;
        } else if ("bootapp".equals(r.kind)) {
            promptBootApp();
            return;
        } else if ("settingslock".equals(r.kind)) {
            if (!HomePrefs.settingsLocked(this) && !PinStore.hasPin(this)) {
                toast("Set a PIN first (sidebar → App Lock)");
                return;
            }
            HomePrefs.setSettingsLocked(this, !HomePrefs.settingsLocked(this));
            toast(
                    HomePrefs.settingsLocked(this)
                            ? "Settings locked with PIN"
                            : "Settings unlocked");
        } else if ("backup".equals(r.kind)) {
            startBackup();
            return;
        } else if ("restore".equals(r.kind)) {
            startRestore();
            return;
        }
        theme = Theme.applyBase(this, (TextView) findViewById(R.id.settings_title));
        refreshSidebarTheme();
        buildRows();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // --- Auto folders ---

    private void promptAutoFolders() {
        final List<AutoFolders.Plan> plans = AutoFolders.suggest(this);
        if (plans.isEmpty()) {
            toast("Nothing to sort — apps are already organized");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (AutoFolders.Plan p : plans) {
            sb.append(p.folderName).append(" (").append(p.apps.size()).append("): ");
            for (int i = 0; i < p.apps.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(p.apps.get(i).label);
            }
            sb.append("\n\n");
        }
        Theme.dialog(this)
                .setTitle("Create these folders?")
                .setMessage(
                        sb.toString().trim()
                                + "\nFavorites and apps already in folders stay where they are.")
                .setPositiveButton(
                        "Create",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                int moved = AutoFolders.apply(SettingsActivity.this, plans);
                                toast(moved + " apps sorted into " + plans.size() + " folders");
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- Boot app ---

    private String bootAppName() {
        String pkg = HomePrefs.bootApp(this);
        if (pkg == null) return "none";
        for (AppEntry e : Apps.query(this)) {
            if (pkg.equals(e.pkg)) return HomePrefs.displayLabel(this, e);
        }
        return "none";
    }

    private void promptBootApp() {
        final List<AppEntry> apps = Apps.query(this);
        final String cur = HomePrefs.bootApp(this);
        List<String> names = new ArrayList<>();
        names.add("None");
        for (AppEntry e : apps) names.add(HomePrefs.displayLabel(this, e));
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            if (apps.get(i).pkg.equals(cur)) {
                checked = i + 1;
                break;
            }
        }
        Theme.dialog(this)
                .setTitle("Open app on TV start")
                .setSingleChoiceItems(
                        names.toArray(new String[0]),
                        checked,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                HomePrefs.setBootApp(
                                        SettingsActivity.this,
                                        which == 0 ? null : apps.get(which - 1).pkg);
                                d.dismiss();
                                toast(
                                        which == 0
                                                ? "No app opens on start"
                                                : apps.get(which - 1).label
                                                        + " selected. Requires Leo as default Home;"
                                                        + " some TVs block boot launches.");
                                buildRows();
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- Backup & restore ---

    private void startBackup() {
        try {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");
            i.putExtra(Intent.EXTRA_TITLE, "leo-tv-backup.json");
            startActivityForResult(i, REQ_BACKUP_CREATE);
        } catch (ActivityNotFoundException e) {
            toast("No file app available for backup");
        }
    }

    private void startRestore() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");
            startActivityForResult(i, REQ_RESTORE_OPEN);
        } catch (ActivityNotFoundException e) {
            toast("No file app available");
        }
    }

    private void doBackup(android.net.Uri uri) {
        try {
            OutputStream out = getContentResolver().openOutputStream(uri);
            if (out == null) throw new Exception("no stream");
            out.write(BackupUtil.export(this).toString().getBytes("UTF-8"));
            out.close();
            toast("Backup saved");
        } catch (Exception e) {
            toast("Backup failed");
        }
    }

    private void doRestore(android.net.Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) throw new Exception("no stream");
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] tmp = new byte[8192];
            int n;
            while ((n = in.read(tmp)) > 0) buf.write(tmp, 0, n);
            in.close();
            String err = BackupUtil.importData(this, new String(buf.toByteArray(), "UTF-8"));
            if (err == null) {
                toast("Settings restored");
                theme = Theme.applyBase(this, (TextView) findViewById(R.id.settings_title));
                buildRows();
            } else {
                toast(err);
            }
        } catch (Exception e) {
            toast("Restore failed");
        }
    }

    private boolean isDefaultHome() {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo ri =
                    getPackageManager().resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
            return ri != null
                    && ri.activityInfo != null
                    && getPackageName().equals(ri.activityInfo.packageName);
        } catch (Exception e) {
            return false;
        }
    }

    private void openHomeSettings() {
        requestHomeRole(false);
    }

    /** Explain the change before opening Android's user-controlled Home selection. */
    private void overrideLauncher() {
        Theme.dialog(this)
                .setTitle("Override current launcher")
                .setMessage(
                        "This makes Leo TV your home screen, replacing the "
                                + "current launcher. You'll confirm it on the next screen.")
                .setPositiveButton(
                        "Continue",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                requestHomeRole(true);
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestHomeRole(boolean aggressive) {
        // Android 10+: ask the system directly with a one-tap dialog.
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                RoleManager rm = getSystemService(RoleManager.class);
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    startActivityForResult(
                            rm.createRequestRoleIntent(RoleManager.ROLE_HOME), REQ_HOME_ROLE);
                    return;
                }
            } catch (Exception ignored) {
                // Fall through to the picker.
            }
        }
        if (aggressive) openHomePicker();
        else {
            openHomePicker();
        }
    }

    /** Android's settings control default Home; a generic chooser cannot set it reliably. */
    private void openHomePicker() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (ActivityNotFoundException noHomeSettings) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
            } catch (ActivityNotFoundException noDefaultSettings) {
                toast(
                        "Open TV Settings > Apps > Default apps > Home app. Some TVs do not allow"
                            + " changing the Home app.");
            }
        }
    }

    // --- Wallpaper picking ---

    private void pickWallpaper() {
        // The picker grants access to the selected URI; the image is copied internally.
        openPicker();
    }

    private void openPicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(i, "Choose wallpaper"), REQ_PICK_IMAGE);
        } catch (Exception e) {
            toast("No gallery app found");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_HOME_ROLE) {
            if (isDefaultHome()) {
                toast("Leo is now your default launcher");
            } else {
                // Respect denial/cancellation instead of opening another grant prompt.
                toast("Default launcher unchanged. You can try again from System settings.");
            }
            buildRows();
        } else if (requestCode == REQ_PICK_IMAGE
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            if (saveWallpaper(data.getData())) {
                HomePrefs.setWallpaperMode(this, "custom");
                toast("Wallpaper set");
            } else {
                toast("Couldn't load that image");
            }
            buildRows();
        } else if (requestCode == REQ_BACKUP_CREATE
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            doBackup(data.getData());
        } else if (requestCode == REQ_RESTORE_OPEN
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            doRestore(data.getData());
        }
    }

    private boolean saveWallpaper(Uri uri) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in1 = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(in1, null, o);
            if (in1 != null) in1.close();
            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / sample > 1920) sample *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            InputStream in2 = getContentResolver().openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(in2, null, o);
            if (in2 != null) in2.close();
            if (b == null) return false;
            FileOutputStream fos = new FileOutputStream(new File(getFilesDir(), "wallpaper.jpg"));
            b.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
