package com.leo.tvlauncher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
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

        Theme t = Theme.applyBase(this, titleView);
        dotsView.setTextColor(t.accent);

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
            case MODE_VERIFY_EXIT_KIDS: titleView.setText("Enter arrow PIN to exit kids mode"); break;
            case MODE_VERIFY_SETTINGS: titleView.setText("Enter arrow PIN to open settings"); break;
        }
    }

    /** 3x3 cross of arrow buttons (visual + touch input; remotes use dispatchKeyEvent). */
    private void buildArrows() {
        GridLayout g = findViewById(R.id.pin_grid);
        g.removeAllViews();
        String[][] cells = {
                {null, "▲|U", null},
                {"◀|L", "⌫|DEL", "▶|R"},
                {null, "▼|D", null},
        };
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (cells[r][c] == null) continue;
                String[] parts = cells[r][c].split("\\|");
                final String action = parts[1];
                Button b = new Button(this);
                b.setText(parts[0]);
                b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
                b.setTextColor(Color.WHITE);
                b.setBackgroundResource(R.drawable.pin_btn);
                b.setFocusable(false);
                b.setClickable(true);
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = 0;
                lp.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 84,
                        getResources().getDisplayMetrics());
                lp.rowSpec = GridLayout.spec(r);
                lp.columnSpec = GridLayout.spec(c, 1f);
                lp.setMargins(10, 10, 10, 10);
                b.setLayoutParams(lp);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if ("DEL".equals(action)) deleteOne();
                        else onDirection(action);
                    }
                });
                g.addView(b);
            }
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
