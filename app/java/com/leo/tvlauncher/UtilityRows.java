package com.leo.tvlauncher;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Title and description stack rather than competing in a trailing-value column. */
public final class UtilityRows {
    private UtilityRows() {}

    public static View create(
            Context c,
            String title,
            String description,
            Drawable icon,
            View.OnClickListener click) {
        Theme t = Theme.current(c);
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int p = Theme.dp(c, 16);
        row.setPadding(p, p, p, p);
        row.setBackground(Theme.tileBackground(c, t));
        row.setFocusable(click != null);
        row.setClickable(click != null);
        if (click != null) row.setOnClickListener(click);
        if (icon != null) {
            ImageView image = new ImageView(c);
            image.setImageDrawable(icon);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(Theme.dp(c, 32), Theme.dp(c, 32));
            lp.setMarginEnd(p);
            row.addView(image, lp);
        }
        LinearLayout text = new LinearLayout(c);
        text.setOrientation(LinearLayout.VERTICAL);
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        TextView label = new TextView(c);
        label.setText(title);
        label.setTextSize(19);
        label.setTextColor(t.text);
        label.setTypeface(Typeface.create("sans-serif-medium", 0));
        text.addView(label);
        TextView sub = new TextView(c);
        sub.setText(description);
        sub.setTextSize(15);
        sub.setTextColor(t.text2);
        sub.setPadding(0, Theme.dp(c, 5), 0, 0);
        text.addView(sub);
        if (description == null || description.isEmpty()) sub.setVisibility(View.GONE);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.bottomMargin = Theme.dp(c, 10);
        row.setLayoutParams(rowLp);
        Theme.focusMotion(row, 1.012f);
        return row;
    }
}
