package com.leo.tvlauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

/** Manual grants stay under the user's control in Android settings. */
final class AccessSettings {
    private AccessSettings() {}

    static void notificationAccess(final Activity activity) {
        Theme.dialog(activity)
                .setTitle("Notification access")
                .setMessage(
                        "Optional: lets Leo read and clear the TV's notifications for the"
                            + " Notifications screen. On the next screen, select Leo TV and allow"
                            + " access. You can turn it off there at any time. Some TVs do not"
                            + " support notification listeners.")
                .setPositiveButton(
                        "Open Android settings",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Build.VERSION.SDK_INT >= 30) {
                                    Intent detail =
                                            new Intent(
                                                    Settings
                                                            .ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
                                    detail.putExtra(
                                            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                                            new ComponentName(activity, NotifListener.class)
                                                    .flattenToString());
                                    try {
                                        activity.startActivity(detail);
                                        return;
                                    } catch (ActivityNotFoundException ignored) {
                                    }
                                }
                                try {
                                    activity.startActivity(
                                            new Intent(
                                                    Settings
                                                            .ACTION_NOTIFICATION_LISTENER_SETTINGS));
                                } catch (ActivityNotFoundException unavailable) {
                                    Toast.makeText(
                                                    activity,
                                                    "This TV has no notification-access settings."
                                                        + " Check device settings; the launcher"
                                                        + " works without this optional feature.",
                                                    Toast.LENGTH_LONG)
                                            .show();
                                }
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
