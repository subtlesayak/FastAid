package com.fastaid.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MapPreviewView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path route = new Path();
    private final List<AidPlace> places = new ArrayList<>();
    private double originLat = 28.6328;
    private double originLng = 77.2197;

    MapPreviewView(Context context) {
        super(context);
    }

    void setPlaces(List<AidPlace> newPlaces, double latitude, double longitude) {
        places.clear();
        if (newPlaces != null) {
            int max = Math.min(newPlaces.size(), 6);
            for (int index = 0; index < max; index++) {
                AidPlace place = newPlaces.get(index);
                if (place.hasCoordinates()) {
                    places.add(place);
                }
            }
        }
        originLat = latitude;
        originLng = longitude;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        canvas.drawColor(Color.rgb(239, 240, 242));

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(243, 224, 196));
        canvas.drawRect(width * 0.66f, 0, width, height * 0.55f, paint);
        paint.setColor(Color.rgb(218, 238, 208));
        canvas.drawRect(width * 0.25f, height * 0.48f, width * 0.54f, height * 0.82f, paint);
        paint.setColor(Color.rgb(232, 238, 246));
        canvas.drawRect(0, 0, width * 0.38f, height * 0.24f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(dp(12));
        drawRoad(canvas, -20, height * 0.22f, width + 40, height * 0.72f);
        drawRoad(canvas, -20, height * 0.76f, width + 40, height * 0.30f);
        drawRoad(canvas, width * 0.12f, -20, width * 0.64f, height + 20);
        drawRoad(canvas, width * 0.86f, -20, width * 0.34f, height + 20);
        drawRoad(canvas, -20, height * 0.58f, width + 40, height * 0.58f);

        paint.setColor(Color.rgb(210, 214, 219));
        paint.setStrokeWidth(dp(2));
        drawRoad(canvas, -20, height * 0.22f, width + 40, height * 0.72f);
        drawRoad(canvas, -20, height * 0.76f, width + 40, height * 0.30f);
        drawRoad(canvas, width * 0.12f, -20, width * 0.64f, height + 20);
        drawRoad(canvas, width * 0.86f, -20, width * 0.34f, height + 20);
        drawRoad(canvas, -20, height * 0.58f, width + 40, height * 0.58f);

        drawRoute(canvas, width, height);
        drawDynamicPois(canvas, width, height);
        drawUserLocation(canvas, width * 0.52f, height * 0.51f);
    }

    private void drawRoute(Canvas canvas, int width, int height) {
        if (places.isEmpty()) {
            return;
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(Color.rgb(229, 57, 53));
        route.reset();
        route.moveTo(width * 0.52f, height * 0.51f);
        for (int index = 0; index < Math.min(places.size(), 3); index++) {
            float[] point = pointForPlace(places.get(index), width, height, index);
            route.lineTo(point[0], point[1]);
        }
        canvas.drawPath(route, paint);
    }

    private void drawDynamicPois(Canvas canvas, int width, int height) {
        if (places.isEmpty()) {
            drawPoi(canvas, width * 0.18f, height * 0.24f, Color.rgb(30, 136, 229), "Police", "0.8 km");
            drawPoi(canvas, width * 0.75f, height * 0.28f, Color.rgb(229, 57, 53), "Hospital", "1.2 km");
            drawPoi(canvas, width * 0.15f, height * 0.66f, Color.rgb(67, 160, 71), "Fuel", "1.0 km");
            drawPoi(canvas, width * 0.72f, height * 0.62f, Color.rgb(67, 160, 71), "Repair", "0.6 km");
            return;
        }

        int count = Math.min(places.size(), 5);
        for (int index = 0; index < count; index++) {
            AidPlace place = places.get(index);
            float[] point = pointForPlace(place, width, height, index);
            drawPoi(canvas, point[0], point[1], colorFor(place.category), shortName(place), place.distance);
        }
    }

    private float[] pointForPlace(AidPlace place, int width, int height, int index) {
        double latDelta = place.latitude - originLat;
        double lngDelta = place.longitude - originLng;
        float x = (float) (width * 0.52f + lngDelta * 3800f);
        float y = (float) (height * 0.51f - latDelta * 4800f);
        float[][] fallback = new float[][]{
                {width * 0.18f, height * 0.24f},
                {width * 0.76f, height * 0.28f},
                {width * 0.16f, height * 0.68f},
                {width * 0.76f, height * 0.63f},
                {width * 0.46f, height * 0.78f}
        };
        if (x < dp(24) || x > width - dp(110) || y < dp(78) || y > height - dp(44)) {
            return fallback[index % fallback.length];
        }
        return new float[]{x, y};
    }

    private void drawUserLocation(Canvas canvas, float x, float y) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(80, 30, 136, 229));
        canvas.drawCircle(x, y, dp(38), paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, dp(15), paint);
        paint.setColor(Color.rgb(30, 136, 229));
        canvas.drawCircle(x, y, dp(11), paint);
    }

    private void drawRoad(Canvas canvas, float startX, float startY, float endX, float endY) {
        canvas.drawLine(startX, startY, endX, endY, paint);
    }

    private void drawPoi(Canvas canvas, float x, float y, int color, String title, String subtitle) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x + dp(2), y + dp(2), dp(11), paint);
        paint.setColor(color);
        canvas.drawCircle(x, y, dp(9), paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, dp(4), paint);

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(dp(10));
        paint.setColor(color);
        canvas.drawText(title, x + dp(13), y - dp(1), paint);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(dp(9));
        canvas.drawText(subtitle, x + dp(13), y + dp(12), paint);
    }

    private String shortName(AidPlace place) {
        String type = place.category == null ? "Aid" : place.category.toLowerCase(Locale.US);
        if (type.contains("hospital") || type.contains("pharmacy")) return "Medical";
        if (type.contains("police")) return "Police";
        if (type.contains("fire")) return "Fire";
        if (type.contains("gas")) return "Fuel";
        if (type.contains("repair") || type.contains("tire") || type.contains("tow")) return "Repair";
        if (type.contains("electric")) return "EV";
        if (type.contains("ngo")) return "NGO";
        return "Aid";
    }

    private int colorFor(String category) {
        String type = category == null ? "" : category.toLowerCase(Locale.US);
        if (type.contains("hospital") || type.contains("pharmacy") || type.contains("fire")) return Color.rgb(229, 57, 53);
        if (type.contains("police")) return Color.rgb(30, 136, 229);
        if (type.contains("gas") || type.contains("electric")) return Color.rgb(251, 140, 0);
        if (type.contains("ngo")) return Color.rgb(21, 101, 192);
        return Color.rgb(67, 160, 71);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
