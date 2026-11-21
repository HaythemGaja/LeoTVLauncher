package com.leo.tvlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Opens the user's chosen app when the TV boots — but only when Leo is the
 * default launcher, so it never hijacks boot for another launcher.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        String pkg = HomePrefs.bootApp(c);
        if (pkg == null || pkg.isEmpty()) return;
        if (!Apps.isDefaultHome(c)) return;
        try {
            Intent launch = c.getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch == null) return;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(launch);
            HomePrefs.pushRecent(c, pkg);
        } catch (Exception ignored) {
        }
    }
}
