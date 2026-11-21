package com.leo.tvlauncher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
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

    private static final int REQ_WALLPAPER_PERM = 100;
    private static final int REQ_PICK_IMAGE = 101;
    private static final int REQ_HOME_ROLE = 102;
    private static final int REQ_BACKUP_CREATE = 103;
    private static final int REQ_RESTORE_OPEN = 104;

    private LinearLayout rows;
    private Theme theme;
    private LayoutInflater inflater;

    @Override
    protected String sidebarSelectedId() {
        return "settings";
    }

    private static class Row {
        String label;
        boolean isHeader;
        boolean checked;
        String kind;   // theme, shape, wall, relock
        String value;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSidebarContent(R.layout.activity_settings);
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

    private void buildRows() {
        rows.removeAllViews();
        List<Row> list = new ArrayList<>();

        list.add(header("General"));
        if (!isDefaultHome()) {
            Row sh = new Row();
            sh.label = "Set as default launcher";
            sh.kind = "sethome";
            sh.value = "";
            list.add(sh);
        }
        Row ma = new Row();
        ma.label = "Manage apps";
        ma.kind = "manageapps";
        ma.value = "";
        list.add(ma);
        Row as = new Row();
        as.label = "Android settings";
        as.kind = "syssettings";
        as.value = "";
        list.add(as);
        Row na = new Row();
        na.label = "Notification access";
        na.kind = "notifaccess";
        na.value = "";
        list.add(na);
        Row ax = new Row();
        ax.label = "Accessibility";
        ax.kind = "accessibility";
        ax.value = "";
        list.add(ax);
        Row ol = new Row();
        ol.label = "Override current launcher";
        ol.kind = "overridelauncher";
        ol.value = "";
        list.add(ol);
        Row sb = new Row();
        sb.label = "Customize sidebar";
        sb.kind = "sidebaredit";
        sb.value = "";
        list.add(sb);
        Row af = new Row();
        af.label = "Auto-create folders";
        af.kind = "autofolders";
        af.value = "";
        list.add(af);
        Row ba = new Row();
        ba.label = "Open app on TV start (" + bootAppName() + ")";
        ba.kind = "bootapp";
        ba.value = "";
        list.add(ba);
        Row sl = new Row();
        sl.label = "Lock settings with PIN";
        sl.kind = "settingslock";
        sl.value = "";
        sl.checked = HomePrefs.settingsLocked(this);
        list.add(sl);

        list.add(header("Background & theme"));
        String curTheme = Theme.current(this).id;
        for (Theme t : Theme.PRESETS) {
            Row r = new Row();
            r.label = t.name;
            r.kind = "theme";
            r.value = t.id;
            r.checked = t.id.equals(curTheme);
            list.add(r);
        }

        list.add(header("App icon shape"));
        String curShape = Theme.iconShape(this);
        for (int i = 0; i < Theme.SHAPES.length; i++) {
            Row r = new Row();
            r.label = Theme.SHAPE_NAMES[i];
            r.kind = "shape";
            r.value = Theme.SHAPES[i];
            r.checked = Theme.SHAPES[i].equals(curShape);
            list.add(r);
        }

        list.add(header("App icon size"));
        int[] sizeDps = {40, 56, 72, 88, 104};
        String[] sizeNames = {"Extra small", "Small", "Medium", "Large", "Extra large"};
        int curSize = Theme.iconSizeDp(this);
        for (int i = 0; i < sizeDps.length; i++) {
            Row r = new Row();
            r.label = sizeNames[i];
            r.kind = "iconsize";
            r.value = String.valueOf(sizeDps[i]);
            r.checked = sizeDps[i] == curSize;
            list.add(r);
        }

        list.add(header("App title size"));
        int[] labelSps = {-1, 13, 15, 18, 21};
        String[] labelNames = {"Auto (follow icon size)", "Small", "Medium", "Large", "Extra large"};
        boolean autoLabel = Theme.labelSizeAuto(this);
        int curLabel = Theme.labelSizeSp(this);
        for (int i = 0; i < labelSps.length; i++) {
            Row r = new Row();
            r.label = labelNames[i];
            r.kind = "labelsize";
            r.value = String.valueOf(labelSps[i]);
            r.checked = labelSps[i] < 0 ? autoLabel : (!autoLabel && labelSps[i] == curLabel);
            list.add(r);
        }

        list.add(header("Kids mode"));
        Row ka = new Row();
        int allowed = HomePrefs.kidsApps(this).size();
        ka.label = "Allowed apps (" + allowed + " selected)";
        ka.kind = "kidsapps";
        ka.value = "";
        list.add(ka);

        list.add(header("Wallpaper"));
        Row w1 = new Row();
        w1.label = "Theme color";
        w1.kind = "wall";
        w1.value = "theme";
        w1.checked = !HomePrefs.wallpaperCustom(this);
        list.add(w1);
        Row w2 = new Row();
        w2.label = "Custom image…";
        w2.kind = "wall";
        w2.value = "custom";
        w2.checked = HomePrefs.wallpaperCustom(this);
        list.add(w2);

        list.add(header("Re-lock locked apps"));
        String[][] relock = {
                {"Always ask for PIN", "0"},
                {"After 1 minute", "1"},
                {"After 5 minutes", "5"},
                {"After 15 minutes", "15"},
                {"After 1 hour", "60"},
        };
        int curRelock = HomePrefs.relockMinutes(this);
        for (String[] o : relock) {
            Row r = new Row();
            r.label = o[0];
            r.kind = "relock";
            r.value = o[1];
            r.checked = Integer.parseInt(o[1]) == curRelock;
            list.add(r);
        }

        list.add(header("Backup & restore"));
        Row bk = new Row();
        bk.label = "Backup settings to file";
        bk.kind = "backup";
        bk.value = "";
        list.add(bk);
        Row rs = new Row();
        rs.label = "Restore settings from file";
        rs.kind = "restore";
        rs.value = "";
        list.add(rs);

        for (final Row r : list) {
            View v = inflater.inflate(R.layout.item_lock, rows, false);
            v.setFocusable(!r.isHeader);
            v.setFocusableInTouchMode(!r.isHeader);
            v.setClickable(!r.isHeader);
            if (!r.isHeader) {
                v.setBackground(Theme.tileBackground(this, theme));
                v.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) { onRowClick(r); }
                });
            }
            ImageView icon = v.findViewById(R.id.lock_icon);
            TextView label = v.findViewById(R.id.lock_label);
            TextView state = v.findViewById(R.id.lock_state);
            label.setText(r.label);
            label.setTextSize(r.isHeader ? 16 : 20);
            label.setTextColor(r.isHeader ? theme.text2 : theme.text);
            if (r.isHeader) {
                icon.setVisibility(View.GONE);
                state.setText("");
            } else {
                icon.setVisibility(View.VISIBLE);
                icon.setImageDrawable(null);
                if ("theme".equals(r.kind)) {
                    icon.setBackgroundColor(accentOf(r.value));
                } else {
                    icon.setBackgroundColor(0x00000000);
                }
                state.setText(r.checked ? "✓" : "");
                state.setTextColor(theme.accent);
            }
            rows.addView(v);
        }
    }

    private Row header(String label) {
        Row r = new Row();
        r.label = label;
        r.isHeader = true;
        return r;
    }

    private int accentOf(String themeId) {
        for (Theme t : Theme.PRESETS) if (t.id.equals(themeId)) return t.accent;
        return 0xFFFFB300;
    }

    private void onRowClick(Row r) {
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
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (ActivityNotFoundException ex) {
                toast("Notification settings not available");
            }
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
            toast(HomePrefs.settingsLocked(this)
                    ? "Settings locked with PIN" : "Settings unlocked");
        } else if ("backup".equals(r.kind)) {
            startBackup();
            return;
        } else if ("restore".equals(r.kind)) {
            startRestore();
            return;
        }
        theme = Theme.applyBase(this, (TextView) findViewById(R.id.settings_title));
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
        new AlertDialog.Builder(this)
                .setTitle("Create these folders?")
                .setMessage(sb.toString().trim()
                        + "\nFavorites and apps already in folders stay where they are.")
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
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
            if (apps.get(i).pkg.equals(cur)) { checked = i + 1; break; }
        }
        new AlertDialog.Builder(this)
                .setTitle("Open app on TV start")
                .setSingleChoiceItems(names.toArray(new String[0]), checked,
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int which) {
                                HomePrefs.setBootApp(SettingsActivity.this,
                                        which == 0 ? null : apps.get(which - 1).pkg);
                                d.dismiss();
                                toast(which == 0 ? "No app opens on start"
                                        : apps.get(which - 1).label + " opens on TV start");
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
            ResolveInfo ri = getPackageManager().resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
            return ri != null && ri.activityInfo != null
                    && getPackageName().equals(ri.activityInfo.packageName);
        } catch (Exception e) {
            return false;
        }
    }

    private void openHomeSettings() {
        requestHomeRole(false);
    }

    /**
     * Aggressively make Leo the default launcher: one-tap system dialog
     * first, then the system Home-app picker (choose Leo TV > Always),
     * then the Home settings page as a last resort.
     */
    private void overrideLauncher() {
        new AlertDialog.Builder(this)
                .setTitle("Override current launcher")
                .setMessage("This makes Leo TV your home screen, replacing the " +
                        "current launcher. You'll confirm it on the next screen.")
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
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
            try {
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            } catch (ActivityNotFoundException e) {
                toast("Open TV Settings > Apps > Default apps > Home app");
            }
        }
    }

    /** System Home-app picker: the user picks Leo TV and chooses Always. */
    private void openHomePicker() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(home, "Choose Leo TV as your launcher"));
            toast("Pick Leo TV, then choose Always");
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            } catch (ActivityNotFoundException ex) {
                toast("Open TV Settings > Apps > Default apps > Home app");
            }
        }
    }

    // --- Wallpaper picking ---

    private void pickWallpaper() {
        String perm = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{perm}, REQ_WALLPAPER_PERM);
            return;
        }
        openPicker();
    }

    private void openPicker() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(i, "Choose wallpaper"), REQ_PICK_IMAGE);
        } catch (Exception e) {
            toast("No gallery app found");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_WALLPAPER_PERM
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openPicker();
        } else if (requestCode == REQ_WALLPAPER_PERM) {
            toast("Permission needed to pick an image");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_HOME_ROLE) {
            if (isDefaultHome()) {
                toast("Leo is now your default launcher");
            } else {
                // The one-tap dialog didn't stick — escalate to the Home picker.
                openHomePicker();
            }
            buildRows();
        } else if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            if (saveWallpaper(data.getData())) {
                HomePrefs.setWallpaperMode(this, "custom");
                toast("Wallpaper set");
            } else {
                toast("Couldn't load that image");
            }
            buildRows();
        } else if (requestCode == REQ_BACKUP_CREATE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            doBackup(data.getData());
        } else if (requestCode == REQ_RESTORE_OPEN && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
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
