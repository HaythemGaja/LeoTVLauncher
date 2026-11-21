package com.leo.tvlauncher;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

/**
 * The Roku-style left navigation bar, shared by the home screen and every
 * settings screen. Top shows the "Leo TV" brand plus a live clock/date
 * (never focusable, so the D-pad can't get trapped), then the nav items.
 */
public class Sidebar {

    // Content views (live inside HomeActivity).
    public static final int VIEW_HOME = 0;
    public static final int VIEW_FAVORITES = 1;
    public static final int VIEW_RECENT = 2;
    public static final int VIEW_FOLDERS = 3;

    // Actions.
    public static final int ACT_NONE = 0;
    public static final int ACT_LOCK = 1;
    public static final int ACT_KIDS = 2;
    public static final int ACT_SETTINGS = 3;
    public static final int ACT_EXIT_KIDS = 4;
    public static final int ACT_NOTIFICATIONS = 5;
    public static final int ACT_QUICK_SETTINGS = 6;

    public static class Item {
        public String id;
        public String emoji;
        public String label;
        public int view = -1; // VIEW_* for content views, -1 for actions
        public int action = ACT_NONE;
    }

    public interface Listener {
        void onView(int view);
        void onAction(int action);
    }

    private static Item item(String id, String emoji, String label, int view, int action) {
        Item s = new Item();
        s.id = id;
        s.emoji = emoji;
        s.label = label;
        s.view = view;
        s.action = action;
        return s;
    }

    /** The built-in definition for a sidebar id (emoji, label, view, action). */
    public static Item defaultItem(String id) {
        if ("home".equals(id)) return item(id, "🏠", "Home", VIEW_HOME, ACT_NONE);
        if ("favorites".equals(id)) return item(id, "★", "Favorites", VIEW_FAVORITES, ACT_NONE);
        if ("recent".equals(id)) return item(id, "🕘", "Recent", VIEW_RECENT, ACT_NONE);
        if ("folders".equals(id)) return item(id, "📁", "Folders", VIEW_FOLDERS, ACT_NONE);
        if ("notifications".equals(id)) return item(id, "🔔", "Notifications", -1, ACT_NOTIFICATIONS);
        if ("quicksettings".equals(id)) return item(id, "⚡", "Quick settings", -1, ACT_QUICK_SETTINGS);
        if ("applock".equals(id)) return item(id, "🔒", "App Lock", -1, ACT_LOCK);
        if ("kids".equals(id)) return item(id, "🧒", "Kids mode", -1, ACT_KIDS);
        if ("settings".equals(id)) return item(id, "⚙️", "Settings", -1, ACT_SETTINGS);
        return null;
    }

    /** Display name ("emoji  label") for a sidebar id, for the customize screen. */
    public static String nameOf(String id) {
        Item i = defaultItem(id);
        return i == null ? id : i.emoji + "  " + i.label;
    }

    /** Sidebar id for a content view constant. */
    public static String idForView(int view) {
        if (view == VIEW_FAVORITES) return "favorites";
        if (view == VIEW_RECENT) return "recent";
        if (view == VIEW_FOLDERS) return "folders";
        return "home";
    }

    private final Activity host;
    private final LinearLayout box;
    private final Listener listener;
    private String selectedId;
    private Theme theme;
    private TextView clockTime;
    private TextView clockDate;
    private LinearLayout itemBox;

    public Sidebar(Activity host, LinearLayout box, String selectedId, Listener listener) {
        this.host = host;
        this.box = box;
        this.selectedId = selectedId;
        this.listener = listener;
    }

    public void setSelectedId(String id) {
        this.selectedId = id;
    }

    public void build() {
        theme = Theme.current(host);
        box.removeAllViews();
        box.setBackgroundColor(theme.card);
        clockTime = null;
        clockDate = null;

        // Brand (moved here from the main-screen header).
        TextView brand = new TextView(host);
        brand.setText("Leo TV");
        brand.setTextSize(22);
        brand.setTypeface(null, Typeface.BOLD);
        brand.setTextColor(theme.accent);
        brand.setFocusable(false);
        int p = Theme.dp(host, 14);
        brand.setPadding(p, Theme.dp(host, 2), p, Theme.dp(host, 2));
        box.addView(brand);

        // Clock + date header (not focusable, never traps the D-pad).
        LinearLayout clockBox = new LinearLayout(host);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setFocusable(false);
        clockBox.setClickable(false);
        clockBox.setPadding(p, Theme.dp(host, 4), p, Theme.dp(host, 16));
        clockTime = new TextView(host);
        clockTime.setTextSize(24);
        clockTime.setTypeface(null, Typeface.BOLD);
        clockTime.setTextColor(theme.text);
        clockTime.setFocusable(false);
        clockDate = new TextView(host);
        clockDate.setTextSize(14);
        clockDate.setTextColor(theme.text2);
        clockDate.setFocusable(false);
        clockBox.addView(clockTime);
        clockBox.addView(clockDate);
        box.addView(clockBox);
        tick();

        // Scrollable item list: on small screens the items can overflow,
        // and the ScrollView follows D-pad focus automatically.
        ScrollView scroller = new ScrollView(host);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroller.setLayoutParams(slp);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        itemBox = new LinearLayout(host);
        itemBox.setOrientation(LinearLayout.VERTICAL);
        itemBox.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        scroller.addView(itemBox);
        box.addView(scroller);

        if (HomePrefs.kidsMode(host)) {
            addItem(item("exitkids", "🚪", "Exit kids mode", -1, ACT_EXIT_KIDS),
                    "exitkids".equals(selectedId));
        } else {
            Set<String> hidden = HomePrefs.sidebarHidden(host);
            for (String id : HomePrefs.sidebarOrder(host)) {
                if (hidden.contains(id)) continue;
                Item s = defaultItem(id);
                if (s != null) addItem(s, id.equals(selectedId));
            }
        }
    }

    private void addItem(final Item s, boolean selected) {
        final TextView tv = new TextView(host);
        tv.setText(s.emoji + "  " + s.label);
        tv.setTextSize(19);
        int pad = Theme.dp(host, 14);
        tv.setPadding(pad, pad, pad, pad);
        tv.setFocusable(true);
        tv.setFocusableInTouchMode(true);
        tv.setClickable(true);
        tv.setTag("side:" + s.id);
        tv.setBackground(Theme.tileBackground(host, theme));
        tv.setTextColor(selected ? theme.accent : theme.text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, Theme.dp(host, 8));
        tv.setLayoutParams(lp);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (s.view >= 0) listener.onView(s.view);
                else listener.onAction(s.action);
            }
        });
        itemBox.addView(tv);
    }

    /** Move D-pad focus to a sidebar item by id. */
    public void focusItem(String id) {
        if (itemBox == null) return;
        for (int i = 0; i < itemBox.getChildCount(); i++) {
            View v = itemBox.getChildAt(i);
            if (("side:" + id).equals(v.getTag())) {
                v.requestFocus();
                return;
            }
        }
    }

    /** Refresh the clock/date text. Hosts call this on a ~30s tick. */
    public void tick() {
        Date now = new Date();
        if (clockTime != null) {
            clockTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        }
        if (clockDate != null) {
            clockDate.setText(
                    new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now));
        }
    }
}
