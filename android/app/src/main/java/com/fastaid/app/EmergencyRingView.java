package com.fastaid.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

final class EmergencyRingView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    EmergencyRingView(Context context) {
        super(context);
        setMinimumHeight(dp(132));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) * 0.31f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(46, 229, 57, 53));
        canvas.drawCircle(cx + dp(4), cy + dp(10), radius + dp(12), paint);

        paint.setColor(Color.rgb(185, 18, 18));
        canvas.drawCircle(cx, cy + dp(2), radius + dp(11), paint);
        paint.setColor(Color.rgb(229, 57, 53));
        canvas.drawCircle(cx, cy, radius + dp(8), paint);
        paint.setColor(Color.rgb(244, 67, 54));
        canvas.drawCircle(cx, cy, radius - dp(2), paint);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(dp(31));
        canvas.drawText("SOS", cx, cy + dp(11), paint);
        paint.setTypeface(Typeface.DEFAULT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
