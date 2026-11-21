package com.leo.tvlauncher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * Base class for every settings-style screen (Settings, Manage apps,
 * Customize sidebar, Kids setup, Folders, Notifications). It keeps the
 * Roku-style sidebar visible so navigation is one D-pad press away,
 * with a live clock/date in the sidebar header.
 *
 * Subclasses call setSidebarContent(layoutRes) instead of setContentView,
 * then work with their views exactly as before.
 */
public abstract class BaseSidebarActivity extends Activity {

    private static final int REQ_EXIT_KIDS = 900;

    private LinearLayout sidebarBox;
    private Sidebar sidebar;
    protected Theme theme;
    private final Handler clockHandler = new Handler();
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (sidebar != null) sidebar.tick();
            clockHandler.postDelayed(this, 30000);
        }
    };

    protected void setSidebarContent(int layoutRes) {
        setContentView(R.layout.activity_sidebar_host);
        sidebarBox = findViewById(R.id.sidebar);
        FrameLayout content = findViewById(R.id.sidebar_content);
        getLayoutInflater().inflate(layoutRes, content, true);
    }

    /** The sidebar id to highlight on this screen ("settings", "folders", ...). */
    protected abstract String sidebarSelectedId();

    @Override
    protected void onResume() {
        super.onResume();
        theme = Theme.current(this);
        findViewById(R.id.sidebar_host_root).setBackgroundColor(theme.bg);
        sidebar = new Sidebar(this, sidebarBox, sidebarSelectedId(),
                new Sidebar.Listener() {
                    @Override public void onView(int view) {
                        HomeActivity.goTo(BaseSidebarActivity.this, view, Sidebar.ACT_NONE);
                    }
                    @Override public void onAction(int action) {
                        onSidebarAction(action);
                    }
                });
        sidebar.build();
        clockTick.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockTick);
    }

    protected void onSidebarAction(int action) {
        if (action == Sidebar.ACT_EXIT_KIDS) {
            Intent i = new Intent(this, PinActivity.class);
            i.putExtra("mode", PinActivity.MODE_VERIFY_EXIT_KIDS);
            startActivityForResult(i, REQ_EXIT_KIDS);
        } else if (action == Sidebar.ACT_SETTINGS
                && "settings".equals(sidebarSelectedId())) {
            // Already on a settings screen; nothing to do.
        } else if (action == Sidebar.ACT_NOTIFICATIONS) {
            startActivity(new Intent(this, NotificationsActivity.class));
        } else if (action == Sidebar.ACT_QUICK_SETTINGS) {
            startActivity(new Intent(this, QuickSettingsActivity.class));
        } else {
            // Content views, App Lock and Kids mode all live in HomeActivity.
            HomeActivity.goTo(this, -1, action);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EXIT_KIDS && resultCode == RESULT_OK) {
            HomePrefs.setKidsMode(this, false);
            HomeActivity.goTo(this, Sidebar.VIEW_HOME, Sidebar.ACT_NONE);
        }
    }
}
