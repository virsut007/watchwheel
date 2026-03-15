/*
 * Copyright 2024 WatchWheel Contributors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ginkage.wearmouse.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

/**
 * Multitouch racing control overlay for round Wear OS screens.
 * Gyro steering (display only), multitouch THR/BRK + tap DRS/ERS.
 *
 * - Top:        Steering display bar + calibrate button (gyro-driven)
 * - Mid-left:   THR zone (momentary hold = 127)
 * - Mid-right:  BRK zone (momentary hold = 127)
 * - Bot-left:   DRS toggle (tap)
 * - Bot-right:  ERS toggle (tap)
 */
public class RacingView extends View {

    public interface RacingInputListener {
        void onGasChanged(int value);       // 0 or 127
        void onBrakeChanged(int value);     // 0 or 127
        void onDrsToggled(boolean active);
        void onErsToggled(boolean active);
        void onCalibrateRequested();
    }

    private RacingInputListener listener;

    // State
    private int steeringValue = 0;  // set externally from gyro
    private boolean gasActive = false;
    private boolean brakeActive = false;
    private boolean drsActive = false;
    private boolean ersActive = false;
    private boolean connected = false;

    // Multitouch: maps pointer ID → zone name
    private final SparseArray<String> pointerZones = new SparseArray<>();

    // Paints
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint steerTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint steerTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint steerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint steerLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint togglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint toggleLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint toggleDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statusBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint calPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Layout rects
    private final RectF thrRect = new RectF();
    private final RectF brkRect = new RectF();
    private final RectF drsRect = new RectF();
    private final RectF ersRect = new RectF();
    private final RectF calRect = new RectF();

    // Steering bar geometry
    private float steerTrackLeft;
    private float steerTrackRight;
    private float steerTrackY;

    // Colors
    private static final int COLOR_BG = 0xFF0A0A0A;
    private static final int COLOR_ZONE_BG = 0xFF141414;
    private static final int COLOR_ZONE_BORDER = 0xFF252525;
    private static final int COLOR_THR_ACTIVE = 0xFF00CC66;
    private static final int COLOR_BRK_ACTIVE = 0xFFCC3333;
    private static final int COLOR_DRS_BG = 0xFF141428;
    private static final int COLOR_DRS_ACTIVE = 0xFF3366CC;
    private static final int COLOR_ERS_BG = 0xFF1E1D10;
    private static final int COLOR_ERS_ACTIVE = 0xFFCCA033;
    private static final int COLOR_STEER_DOT = 0xFF00DD66;
    private static final int COLOR_TRACK = 0xFF1A1A1A;
    private static final int COLOR_TICK = 0xFF2A2A2A;
    private static final int COLOR_DIM = 0xFF333333;
    private static final int COLOR_GREEN = 0xFF00DD66;
    private static final int COLOR_CAL = 0xFF1A1A2E;

    public RacingView(Context context) {
        super(context);
        init();
    }

    public RacingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bgPaint.setColor(COLOR_BG);

        steerTrackPaint.setColor(COLOR_TRACK);
        steerTrackPaint.setStrokeWidth(2f);
        steerTrackPaint.setStrokeCap(Paint.Cap.ROUND);

        steerTickPaint.setColor(COLOR_TICK);
        steerTickPaint.setStrokeWidth(1f);
        steerTickPaint.setStrokeCap(Paint.Cap.ROUND);

        steerDotPaint.setColor(COLOR_STEER_DOT);

        steerLabelPaint.setColor(COLOR_DIM);
        steerLabelPaint.setTextAlign(Paint.Align.CENTER);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1f);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(COLOR_DIM);

        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setColor(COLOR_DIM);

        toggleLabelPaint.setTextAlign(Paint.Align.CENTER);
        toggleLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        toggleDotPaint.setStyle(Paint.Style.FILL);

        statusPaint.setColor(COLOR_DIM);
        statusPaint.setTextAlign(Paint.Align.LEFT);
        statusPaint.setTypeface(Typeface.create("monospace", Typeface.NORMAL));

        statusBarPaint.setColor(COLOR_GREEN);

        calPaint.setColor(COLOR_CAL);
    }

    public void setListener(RacingInputListener listener) {
        this.listener = listener;
    }

    /** Called from RacingActivity when SteeringManager reports a new value. */
    public void setSteering(int value) {
        this.steeringValue = Math.max(-127, Math.min(127, value));
        invalidate();
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float pad = w * 0.10f;
        float gap = w * 0.02f;
        float midX = w / 2f;

        // Steering bar
        steerTrackLeft = pad + 16;
        steerTrackRight = w - pad - 16;
        steerTrackY = h * 0.14f;

        // Calibrate button: small pill above steering bar, centered
        float calW = w * 0.22f;
        float calH = h * 0.06f;
        calRect.set(midX - calW / 2, h * 0.02f, midX + calW / 2, h * 0.02f + calH);

        // THR/BRK zones
        float zoneTop = h * 0.28f;
        float zoneSplit = h * 0.68f;

        thrRect.set(pad, zoneTop, midX - gap, zoneSplit);
        brkRect.set(midX + gap, zoneTop, w - pad, zoneSplit);

        // DRS/ERS
        float toggleTop = zoneSplit + gap;
        float toggleBot = h * 0.88f;

        drsRect.set(pad, toggleTop, midX - gap, toggleBot);
        ersRect.set(midX + gap, toggleTop, w - pad, toggleBot);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        canvas.drawColor(COLOR_BG);

        drawCalButton(canvas);
        drawSteeringBar(canvas);
        drawInputZone(canvas, thrRect, "THR", gasActive, COLOR_THR_ACTIVE);
        drawInputZone(canvas, brkRect, "BRK", brakeActive, COLOR_BRK_ACTIVE);
        drawToggle(canvas, drsRect, "DRS", drsActive, COLOR_DRS_BG, COLOR_DRS_ACTIVE);
        drawToggle(canvas, ersRect, "ERS", ersActive, COLOR_ERS_BG, COLOR_ERS_ACTIVE);
        drawStatusBar(canvas, w, h);
    }

    private void drawCalButton(Canvas canvas) {
        float cornerR = 8f;

        // Background
        calPaint.setColor(COLOR_CAL);
        canvas.drawRoundRect(calRect, cornerR, cornerR, calPaint);

        // Border
        borderPaint.setColor(0xFF333355);
        borderPaint.setAlpha(120);
        canvas.drawRoundRect(calRect, cornerR, cornerR, borderPaint);
        borderPaint.setAlpha(255);

        // Label
        steerLabelPaint.setTextSize(9f);
        steerLabelPaint.setColor(0xFF6666AA);
        canvas.drawText("CALIBRATE", calRect.centerX(), calRect.centerY() + 3, steerLabelPaint);
    }

    private void drawSteeringBar(Canvas canvas) {
        float trackW = steerTrackRight - steerTrackLeft;
        float cx = (steerTrackLeft + steerTrackRight) / 2f;

        // Track line
        canvas.drawLine(steerTrackLeft, steerTrackY, steerTrackRight, steerTrackY, steerTrackPaint);

        // Tick marks
        int numTicks = 21;
        for (int i = 0; i <= numTicks; i++) {
            float tx = steerTrackLeft + (trackW * i / numTicks);
            float tickH = (i == numTicks / 2) ? 6f : 3f;
            canvas.drawLine(tx, steerTrackY - tickH, tx, steerTrackY + tickH, steerTickPaint);
        }

        // Dot
        float dotX = cx + (steeringValue / 127f) * (trackW / 2f) * 0.92f;
        int dotColor = Math.abs(steeringValue) > 90 ? 0xFFFF4444 :
                       Math.abs(steeringValue) > 50 ? 0xFFFFAA00 : COLOR_STEER_DOT;
        steerDotPaint.setColor(dotColor);
        canvas.drawCircle(dotX, steerTrackY, 6f, steerDotPaint);

        // Value + label
        steerLabelPaint.setTextSize(12f);
        steerLabelPaint.setColor(COLOR_DIM);
        String valStr = (steeringValue >= 0 ? "+" : "") + steeringValue;
        canvas.drawText(valStr + " STR", cx, steerTrackY + 20, steerLabelPaint);
    }

    private void drawInputZone(Canvas canvas, RectF rect, String label,
                               boolean active, int activeColor) {
        float cornerR = 12f;
        float cx = rect.centerX();
        float cy = rect.centerY();

        // Background
        if (active) {
            fillPaint.setColor(activeColor);
            fillPaint.setAlpha(30);
            canvas.drawRoundRect(rect, cornerR, cornerR, fillPaint);
            fillPaint.setAlpha(255);
        } else {
            zonePaint.setColor(COLOR_ZONE_BG);
            canvas.drawRoundRect(rect, cornerR, cornerR, zonePaint);
        }

        // Border
        borderPaint.setColor(active ? activeColor : COLOR_ZONE_BORDER);
        borderPaint.setAlpha(active ? 150 : 80);
        canvas.drawRoundRect(rect, cornerR, cornerR, borderPaint);
        borderPaint.setAlpha(255);

        // Value
        int val = active ? 127 : 0;
        valuePaint.setTextSize(28f);
        valuePaint.setColor(active ? 0xCCFFFFFF : COLOR_DIM);
        canvas.drawText(String.valueOf(val), cx, cy + 4, valuePaint);

        // Label
        labelPaint.setTextSize(10f);
        labelPaint.setColor(active ? (activeColor & 0x99FFFFFF) : 0xFF282828);
        canvas.drawText(label, cx, cy + 24, labelPaint);
    }

    private void drawToggle(Canvas canvas, RectF rect, String label,
                            boolean active, int bgColor, int activeColor) {
        float cornerR = 10f;
        float cx = rect.centerX();
        float cy = rect.centerY();

        togglePaint.setColor(active ? activeColor : bgColor);
        togglePaint.setAlpha(active ? 180 : 255);
        canvas.drawRoundRect(rect, cornerR, cornerR, togglePaint);
        togglePaint.setAlpha(255);

        borderPaint.setColor(active ? activeColor : (bgColor | 0xFF333333));
        borderPaint.setAlpha(active ? 220 : 60);
        canvas.drawRoundRect(rect, cornerR, cornerR, borderPaint);
        borderPaint.setAlpha(255);

        toggleLabelPaint.setTextSize(13f);
        toggleLabelPaint.setColor(active ? 0xFFFFFFFF : (bgColor | 0xFF555555));

        float labelW = toggleLabelPaint.measureText(label);
        float totalW = 10 + 6 + labelW;
        float startX = cx - totalW / 2f;

        int dotColor = active ? activeColor : 0xFF444444;
        toggleDotPaint.setColor(dotColor);
        canvas.drawRect(startX, cy - 4, startX + 8, cy + 4, toggleDotPaint);

        canvas.drawText(label, startX + 10 + labelW / 2f, cy + 5, toggleLabelPaint);
    }

    private void drawStatusBar(Canvas canvas, int w, int h) {
        float y = h * 0.95f;

        statusBarPaint.setColor(connected ? COLOR_GREEN : 0xFFFF4444);
        canvas.drawRect(w * 0.08f, y - 10, w * 0.08f + 3, y + 2, statusBarPaint);

        statusPaint.setTextSize(7f);
        int thr = gasActive ? 127 : 0;
        int brk = brakeActive ? 127 : 0;
        int btn = (drsActive ? 1 : 0) + (ersActive ? 2 : 0);
        String status = String.format("STR %+04d  THR %03d  BRK %03d  BTN %02d",
                steeringValue, thr, brk, btn);
        canvas.drawText(status, w * 0.12f, y, statusPaint);
    }

    // ---- Multitouch handling ----

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                float x = event.getX(actionIndex);
                float y = event.getY(actionIndex);

                // Calibrate button
                if (calRect.contains(x, y)) {
                    if (listener != null) listener.onCalibrateRequested();
                    return true;
                }

                // DRS/ERS toggles
                if (drsRect.contains(x, y)) {
                    drsActive = !drsActive;
                    if (listener != null) listener.onDrsToggled(drsActive);
                    invalidate();
                    return true;
                }
                if (ersRect.contains(x, y)) {
                    ersActive = !ersActive;
                    if (listener != null) listener.onErsToggled(ersActive);
                    invalidate();
                    return true;
                }

                // THR (momentary)
                if (thrRect.contains(x, y)) {
                    pointerZones.put(pointerId, "thr");
                    gasActive = true;
                    if (listener != null) listener.onGasChanged(127);
                    invalidate();
                    return true;
                }

                // BRK (momentary)
                if (brkRect.contains(x, y)) {
                    pointerZones.put(pointerId, "brk");
                    brakeActive = true;
                    if (listener != null) listener.onBrakeChanged(127);
                    invalidate();
                    return true;
                }

                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                String zone = pointerZones.get(pointerId);
                if (zone != null) {
                    if ("thr".equals(zone)) {
                        gasActive = false;
                        if (listener != null) listener.onGasChanged(0);
                    } else if ("brk".equals(zone)) {
                        brakeActive = false;
                        if (listener != null) listener.onBrakeChanged(0);
                    }
                    pointerZones.remove(pointerId);
                    invalidate();
                }
                return true;
            }

            case MotionEvent.ACTION_CANCEL: {
                gasActive = false;
                brakeActive = false;
                if (listener != null) {
                    listener.onGasChanged(0);
                    listener.onBrakeChanged(0);
                }
                pointerZones.clear();
                invalidate();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
}
