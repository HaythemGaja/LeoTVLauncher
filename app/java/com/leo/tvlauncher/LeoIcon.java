package com.leo.tvlauncher;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Small, density-independent line symbols. No firmware-dependent emoji glyphs. */
public final class LeoIcon extends Drawable {
    public static final int FOLDER = 0,
            LOCK = 1,
            STAR = 2,
            SETTINGS = 3,
            WIFI = 4,
            DISPLAY = 5,
            SOUND = 6,
            STORAGE = 7,
            CLOCK = 8,
            INFO = 9,
            POWER = 10,
            RESTART = 11;
    private final int kind;
    private final Paint paint = new Paint(3);
    private int color;

    public LeoIcon(int kind, int color) {
        this.kind = kind;
        this.color = color;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.7f);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        if (kind == LOCK) {
            canvas.drawRoundRect(5, 10, 19, 22, 3, 3, paint);
            canvas.drawArc(8, 2, 16, 15, 180, 180, false, paint);
            canvas.drawLine(12, 15, 12, 18, paint);
        } else if (kind == FOLDER) {
            Path p = new Path();
            p.moveTo(3, 6);
            p.lineTo(10, 6);
            p.lineTo(12, 9);
            p.lineTo(21, 9);
            p.lineTo(21, 21);
            p.lineTo(3, 21);
            p.close();
            canvas.drawPath(p, paint);
        } else if (kind == STAR) {
            Path p = new Path();
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 5;
                float r = i % 2 == 0 ? 10 : 4.5f;
                float x = 12 + (float) Math.cos(a) * r, y = 12 + (float) Math.sin(a) * r;
                if (i == 0) p.moveTo(x, y);
                else p.lineTo(x, y);
            }
            p.close();
            canvas.drawPath(p, paint);
        } else if (kind == WIFI) {
            canvas.drawArc(2, 5, 22, 21, 220, 100, false, paint);
            canvas.drawArc(6, 10, 18, 21, 220, 100, false, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(12, 19, 1.5f, paint);
        } else if (kind == DISPLAY) {
            canvas.drawRoundRect(2, 3, 22, 17, 2, 2, paint);
            canvas.drawLine(12, 17, 12, 21, paint);
            canvas.drawLine(7, 21, 17, 21, paint);
        } else if (kind == SOUND) {
            Path p = new Path();
            p.moveTo(3, 9);
            p.lineTo(7, 9);
            p.lineTo(12, 5);
            p.lineTo(12, 19);
            p.lineTo(7, 15);
            p.lineTo(3, 15);
            p.close();
            canvas.drawPath(p, paint);
            canvas.drawArc(11, 6, 22, 18, -65, 130, false, paint);
        } else if (kind == STORAGE) {
            canvas.drawRoundRect(3, 4, 21, 21, 2, 2, paint);
            canvas.drawLine(3, 15, 21, 15, paint);
            canvas.drawCircle(17, 18, 1, paint);
        } else if (kind == CLOCK) {
            canvas.drawCircle(12, 12, 9, paint);
            canvas.drawLine(12, 6, 12, 12, paint);
            canvas.drawLine(12, 12, 16, 14, paint);
        } else if (kind == INFO) {
            canvas.drawCircle(12, 12, 9, paint);
            canvas.drawLine(12, 11, 12, 17, paint);
            canvas.drawPoint(12, 7, paint);
        } else if (kind == POWER) {
            canvas.drawArc(3, 3, 21, 21, -55, 290, false, paint);
            canvas.drawLine(12, 2, 12, 11, paint);
        } else if (kind == RESTART) {
            canvas.drawArc(4, 4, 21, 21, 10, 290, false, paint);
            canvas.drawLine(16, 2, 19, 6, paint);
            canvas.drawLine(19, 6, 14, 7, paint);
        } else {
            canvas.drawLine(3, 7, 21, 7, paint);
            canvas.drawLine(3, 17, 21, 17, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(8, 7, 3, paint);
            canvas.drawCircle(16, 17, 3, paint);
        }
        canvas.restore();
    }

    @Override
    public void setAlpha(int a) {
        paint.setAlpha(a);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter f) {
        paint.setColorFilter(f);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
