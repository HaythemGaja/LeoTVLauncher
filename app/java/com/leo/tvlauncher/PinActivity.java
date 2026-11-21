package com.leo.tvlauncher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Arrow PIN pad. The PIN is a sequence of 4 arrow-key presses on the remote
 * (up / down / left / right), so nobody watching the screen can read it.
 * Progress dots never reveal which arrows were pressed.
 * Back deletes one arrow (or exits when empty).
 *
 * Modes: SET -> CONFIRM, UNLOCK (open a locked app), VERIFY (enter App Lock),
 * folder SET/CONFIRM/UNLOCK, VERIFY_EXIT_KIDS.
 */
public class PinActivity extends Activity {

    public static final int MODE_SET = 1;
    public static final int MODE_CONFIRM = 2;
    public static final int MODE_UNLOCK = 3;
    public static final int MODE_VERIFY = 4;
    public static final int MODE_SET_FOLDER = 5;
    public static final int MODE_CONFIRM_FOLDER = 6;
    public static final int MODE_UNLOCK_FOLDER = 7;
    public static final int MODE_VERIFY_EXIT_KIDS = 8;
    public static final int MODE_VERIFY_SETTINGS = 9;

    private static final int PIN_LEN = 4;

    private int mode;
    private String firstPin;
    private String targetPkg;
    private String folderId;
    private final StringBuilder pin = new StringBuilder();
    private final Handler handler = new Handler();

    private TextView titleView;
    private TextView dotsView;
    private TextView errorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        mode = getIntent().getIntExtra("mode", MODE_SET);
        targetPkg = getIntent().getStringExtra("pkg");
        folderId = getIntent().getStringExtra("folder_id");

        // Old digit PINs can never be entered on the arrow pad, so reset once.
        if (PinStore.needsMigration(this)) {
            boolean folderMode = (mode == MODE_UNLOCK_FOLDER);
            PinStore.migrate(this);
            Toast.makeText(this, "PINs are now arrow presses — set a new one",
                    Toast.LENGTH_LONG).show();
            if (folderMode) {
                finish();
                return;
            }
            mode = MODE_SET;
        }

        titleView = findViewById(R.id.pin_title);
        dotsView = findViewById(R.id.pin_dots);
        errorView = findViewById(R.id.pin_error);

        Theme t = Theme.current(this);
        View panel = findViewById(R.id.pin_panel);
        Theme.styleTree(panel, t);
        panel.setBackground(Theme.surface(this, t.card, t.cardFocus, 1));
        android.view.ViewGroup.LayoutParams panelLp = panel.getLayoutParams();
        panelLp.width =
                Math.min(
                        Theme.dp(this, 520),
                        getResources().getDisplayMetrics().widthPixels - Theme.dp(this, 48));
        panel.setLayoutParams(panelLp);
        titleView.setTextColor(t.text);
        titleView.setTextSize(24);
        dotsView.setTextColor(t.accent);
        errorView.setTextColor("light".equals(t.id) ? 0xFFB3261E : 0xFFFF7272);
        ((TextView) findViewById(R.id.pin_help)).setTextColor(t.text2);
        ((android.widget.ImageView) findViewById(R.id.pin_mark))
                .setImageDrawable(new LeoIcon(LeoIcon.LOCK, t.accent));

        updateTitle();
        buildArrows();
        updateDots();
    }

    /** Remote arrow keys are PIN input, not navigation. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            String d = null;
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP: d = "U"; break;
                case KeyEvent.KEYCODE_DPAD_DOWN: d = "D"; break;
                case KeyEvent.KEYCODE_DPAD_LEFT: d = "L"; break;
                case KeyEvent.KEYCODE_DPAD_RIGHT: d = "R"; break;
                case KeyEvent.KEYCODE_BACK:
                    if (pin.length() > 0) {
                        deleteOne();
                    } else {
                        finish();
                    }
                    return true;
            }
            if (d != null) {
                onDirection(d);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void updateTitle() {
        switch (mode) {
            case MODE_SET: titleView.setText("Press 4 arrows for your PIN"); break;
            case MODE_CONFIRM: titleView.setText("Repeat the 4 arrows"); break;
            case MODE_UNLOCK: titleView.setText("Enter arrow PIN to open app"); break;
            case MODE_VERIFY: titleView.setText("Enter your arrow PIN"); break;
            case MODE_SET_FOLDER: titleView.setText("Press 4 arrows for the folder PIN"); break;
            case MODE_CONFIRM_FOLDER: titleView.setText("Repeat the folder arrows"); break;
            case MODE_UNLOCK_FOLDER: titleView.setText("Enter folder arrow PIN"); break;
            case MODE_VERIFY_EXIT_KIDS:
                titleView.setText("Enter arrow PIN to exit kids mode");
                break;
            case MODE_VERIFY_SETTINGS: titleView.setText("Enter arrow PIN to open settings"); break;
        }
    }

    /** Touch remains available; physical remote arrows still enter the same four-arrow PIN. */
    private void buildArrows() {
        GridLayout grid = findViewById(R.id.pin_grid);
        grid.removeAllViews();
        grid.setColumnCount(5);
        String[][] keys = {
            {"↑", "U", "Up"},
            {"↓", "D", "Down"},
            {"←", "L", "Left"},
            {"→", "R", "Right"},
            {"⌫", "DEL", "Delete"}
        };
        Theme t = Theme.current(this);
        for (int i = 0; i < keys.length; i++) {
            final String action = keys[i][1];
            Button button = new Button(this);
            button.setText(keys[i][0]);
            button.setContentDescription(keys[i][2]);
            button.setTextSize(25);
            button.setTextColor(t.text);
            button.setBackground(Theme.tileBackground(this, t));
            button.setFocusable(false);
            button.setClickable(true);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = Theme.dp(this, 52);
            lp.columnSpec = GridLayout.spec(i, 1f);
            lp.setMargins(
                    Theme.dp(this, 4), Theme.dp(this, 4), Theme.dp(this, 4), Theme.dp(this, 4));
            grid.addView(button, lp);
            button.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if ("DEL".equals(action)) deleteOne();
                            else onDirection(action);
                        }
                    });
        }
    }

    private void onDirection(String d) {
        errorView.setText("");
        if (pin.length() < PIN_LEN) pin.append(d);
        updateDots();
        if (pin.length() == PIN_LEN) {
            handler.postDelayed(new Runnable() {
                @Override public void run() { submit(); }
            }, 220);
        }
    }

    private void deleteOne() {
        if (pin.length() > 0) {
            pin.deleteCharAt(pin.length() - 1);
            errorView.setText("");
            updateDots();
        }
    }

    private void updateDots() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PIN_LEN; i++) {
            if (i > 0) sb.append(' ');
            sb.append(i < pin.length() ? '\u25CF' : '\u25CB');
        }
        dotsView.setText(sb.toString());
    }

    private void submit() {
        if (pin.length() != PIN_LEN) return;
        String entered = pin.toString();
        switch (mode) {
            case MODE_SET:
                firstPin = entered;
                mode = MODE_CONFIRM;
                pin.setLength(0);
                updateTitle();
                updateDots();
                break;
            case MODE_CONFIRM:
                if (entered.equals(firstPin)) {
                    PinStore.setPin(this, entered);
                    Toast.makeText(this, "PIN set", Toast.LENGTH_SHORT).show();
                    openLockActivity();
                } else {
                    fail("Arrows don't match — start over");
                    mode = MODE_SET;
                    updateTitle();
                }
                break;
            case MODE_UNLOCK:
                if (PinStore.checkPin(this, entered)) {
                    HomePrefs.setLastUnlock(this, System.currentTimeMillis());
                    Intent launch = getPackageManager().getLaunchIntentForPackage(targetPkg);
                    finish();
                    if (launch != null) {
                        startActivity(launch);
                        HomePrefs.pushRecent(this, targetPkg);
                    } else {
                        Toast.makeText(this, "Can't open app", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    fail("Wrong PIN — try again");
                }
                break;
            case MODE_VERIFY:
                if (PinStore.checkPin(this, entered)) {
                    openLockActivity();
                } else {
                    fail("Wrong PIN — try again");
                }
                break;
            case MODE_SET_FOLDER:
                firstPin = entered;
                mode = MODE_CONFIRM_FOLDER;
                pin.setLength(0);
                updateTitle();
                updateDots();
                break;
            case MODE_CONFIRM_FOLDER:
                if (entered.equals(firstPin)) {
                    FolderStore.setPin(this, folderId, PinStore.makeHash(entered));
                    Toast.makeText(this, "Folder PIN set", Toast.LENGTH_SHORT).show();
                    openFolderActivity();
                } else {
                    fail("Arrows don't match — start over");
                    mode = MODE_SET_FOLDER;
                    updateTitle();
                }
                break;
            case MODE_UNLOCK_FOLDER:
                if (FolderStore.checkPin(this, folderId, entered)) {
                    openFolderActivity();
                } else {
                    fail("Wrong PIN — try again");
                }
                break;
            case MODE_VERIFY_EXIT_KIDS:
                if (PinStore.checkPin(this, entered)) {
                    setResult(RESULT_OK);
                    finish();
                } else {
                    fail("Wrong PIN — try again");
                }
                break;
            case MODE_VERIFY_SETTINGS:
                if (PinStore.checkPin(this, entered)) {
                    finish();
                    startActivity(new Intent(this, SettingsActivity.class));
                } else {
                    fail("Wrong PIN — try again");
                }
                break;
        }
    }

    private void fail(String msg) {
        errorView.setText(msg);
        pin.setLength(0);
        updateDots();
    }

    private void openLockActivity() {
        Intent i = new Intent(this, LockActivity.class);
        i.putExtra("verified", true);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    private void openFolderActivity() {
        Intent i = new Intent(this, FolderActivity.class);
        i.putExtra("folder_id", folderId);
        i.putExtra("verified", true);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
