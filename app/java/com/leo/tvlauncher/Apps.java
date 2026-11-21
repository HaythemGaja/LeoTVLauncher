package com.leo.tvlauncher;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shared app-query + app-launch helpers. */
public class Apps {

    /** All launchable apps on the device, sorted by label (excludes this launcher). */
    public static List<AppEntry> query(Context c) {
        PackageManager pm = c.getPackageManager();
        Set<String> seen = new HashSet<>();
        List<AppEntry> out = new ArrayList<>();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(c.getPackageName()) || !seen.add(pkg)) continue;
                AppEntry e = new AppEntry();
                e.label = ri.loadLabel(pm).toString();
                e.pkg = pkg;
                try {
                    e.icon = ri.loadIcon(pm);
                } catch (Exception ignored) {
                    e.icon = null;
                }
                try {
                    e.banner = ri.activityInfo.loadBanner(pm);
                    if (e.banner == null) e.banner = pm.getApplicationBanner(pkg);
                } catch (Exception ignored) {
                    e.banner = null;
                }
                out.add(e);
            }
        }
        Collections.sort(
                out,
                new Comparator<AppEntry>() {
                    @Override
                    public int compare(AppEntry a, AppEntry b) {
                        return a.label.compareToIgnoreCase(b.label);
                    }
                });
        return out;
    }

    /** Open an app, asking for the PIN first when it is locked (honors re-lock timer). */
    public static void openApp(Activity a, AppEntry e) {
        if (e.pkg == null) return;
        boolean locked = PinStore.hasPin(a) && PinStore.lockedApps(a).contains(e.pkg);
        if (locked) {
            int mins = HomePrefs.relockMinutes(a);
            long idle = System.currentTimeMillis() - HomePrefs.lastUnlock(a);
            if (mins <= 0 || idle > mins * 60000L) {
                Intent i = new Intent(a, PinActivity.class);
                i.putExtra("mode", PinActivity.MODE_UNLOCK);
                i.putExtra("pkg", e.pkg);
                a.startActivity(i);
                return;
            }
        }
        launch(a, e);
    }

    /** Open an app directly with no PIN check. */
    public static void launch(Activity a, AppEntry e) {
        if (e.pkg == null) return;
        Intent launch = a.getPackageManager().getLaunchIntentForPackage(e.pkg);
        if (launch == null) launch = a.getPackageManager().getLeanbackLaunchIntentForPackage(e.pkg);
        if (launch != null) {
            a.startActivity(launch);
            HomePrefs.pushRecent(a, e.pkg);
        } else {
            Toast.makeText(a, "Can't open " + e.label, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Uninstall an app. System apps (preinstalled on the TV) cannot be uninstalled by any launcher,
     * so those get an honest dialog offering Hide instead. User apps go through the direct system
     * uninstall flow, falling back to the App info screen (which has an Uninstall button) on TVs
     * where the direct flow has no handler.
     *
     * @param onHidden optional callback run after the user hides a system app.
     */
    public static void uninstall(
            final Activity a, final String pkg, final String label, final Runnable onHidden) {
        if (pkg == null) return;
        if (isSystemApp(a, pkg)) {
            Theme.dialog(a)
                    .setTitle(label)
                    .setMessage(
                            "This app came with your TV and can't be uninstalled. "
                                    + "You can hide it from the home screen instead.")
                    .setPositiveButton(
                            "Hide from home",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int which) {
                                    HomePrefs.setHidden(a, pkg, true);
                                    Toast.makeText(a, label + " hidden", Toast.LENGTH_SHORT).show();
                                    if (onHidden != null) onHidden.run();
                                }
                            })
                    .setNeutralButton(
                            "App info",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int which) {
                                    try {
                                        openAppInfo(a, pkg);
                                    } catch (Exception e) {
                                        Toast.makeText(
                                                        a,
                                                        "App info not available",
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                }
                            })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        try {
            a.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg)));
            return;
        } catch (Exception ignored) {
            // Fall through to App info.
        }
        try {
            openAppInfo(a, pkg);
            Toast.makeText(a, "Choose Uninstall on the app info screen", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(a, "Can't uninstall " + label, Toast.LENGTH_SHORT).show();
        }
    }

    /** Uninstall without a hide callback. */
    public static void uninstall(Activity a, String pkg, String label) {
        uninstall(a, pkg, label, null);
    }

    /** True for preinstalled system apps, which Android never lets anyone uninstall. */
    public static boolean isSystemApp(Context c, String pkg) {
        try {
            ApplicationInfo ai = c.getPackageManager().getApplicationInfo(pkg, 0);
            return (ai.flags
                            & (ApplicationInfo.FLAG_SYSTEM
                                    | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))
                    != 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Open the system App info screen for a package. */
    public static void openAppInfo(Activity a, String pkg) {
        a.startActivity(
                new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg)));
    }

    /** True when this launcher is the device's default home app. */
    public static boolean isDefaultHome(Context c) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo ri =
                    c.getPackageManager().resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
            return ri != null
                    && ri.activityInfo != null
                    && c.getPackageName().equals(ri.activityInfo.packageName);
        } catch (Exception e) {
            return false;
        }
    }
}
