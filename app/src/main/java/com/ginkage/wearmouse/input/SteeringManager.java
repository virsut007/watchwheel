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

package com.ginkage.wearmouse.input;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Converts watch gyroscope yaw into a steering value (-127 to +127).
 *
 * Uses raw TYPE_GYROSCOPE and integrates ONLY the Y-axis angular velocity.
 * Pitch (X) and roll (Z) are completely ignored — no cross-axis bleed.
 * Pure linear mapping, no dead zone, no smoothing.
 */
public class SteeringManager implements SensorEventListener {

    public interface SteeringListener {
        void onSteeringChanged(int value); // -127 to +127
    }

    private static final float MAX_ANGLE_DEGREES = 20f;
    private static final int SENSOR_RATE = SensorManager.SENSOR_DELAY_GAME;

    private final SensorManager sensorManager;
    private final Sensor gyroSensor;
    private final SteeringListener listener;

    private float integratedAngle = 0f; // accumulated yaw in degrees
    private long lastTimestamp = 0;
    private int lastReportedValue = 0;

    public SteeringManager(Context context, SteeringListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public void start() {
        if (gyroSensor != null) {
            lastTimestamp = 0;
            sensorManager.registerListener(this, gyroSensor, SENSOR_RATE);
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    /** Reset steering to center. */
    public void calibrate() {
        integratedAngle = 0f;
        lastTimestamp = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (lastTimestamp == 0) {
            lastTimestamp = event.timestamp;
            return;
        }

        // dt in seconds (timestamp is in nanoseconds)
        float dt = (event.timestamp - lastTimestamp) / 1_000_000_000f;
        lastTimestamp = event.timestamp;

        // Ignore unreasonable dt (e.g. first sample, resume from pause)
        if (dt <= 0 || dt > 0.1f) return;

        // event.values[2] = angular velocity around Z axis (rad/s)
        // Negated so clockwise rotation (steer right) gives positive value
        float yawRateDeg = -(float) Math.toDegrees(event.values[2]);

        // Integrate to get angle
        integratedAngle += yawRateDeg * dt;

        // Clamp and map linearly to -127..+127
        float clamped = Math.max(-MAX_ANGLE_DEGREES, Math.min(MAX_ANGLE_DEGREES, integratedAngle));
        int intValue = Math.round((clamped / MAX_ANGLE_DEGREES) * 127f);

        if (intValue != lastReportedValue) {
            lastReportedValue = intValue;
            if (listener != null) {
                listener.onSteeringChanged(intValue);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
