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

package com.ginkage.wearmouse.bluetooth;

/**
 * Builds the 4-byte HID input report for the gamepad descriptor.
 *
 * Report layout (matches HIDD_REPORT_DESC for ID_GAMEPAD):
 *   Byte 0: Steering  — signed int8,  -127 (full left) to 127 (full right)
 *   Byte 1: Gas       — unsigned int8, 0 (off) to 127 (full throttle)
 *   Byte 2: Brake     — unsigned int8, 0 (off) to 127 (full brake)
 *   Byte 3: Buttons   — 8-bit bitmask (bit0=DRS, bit1=ERS, bits2-7=spare)
 *
 * Thread safety: This class is NOT thread-safe. Call from main thread only,
 * or synchronize externally.
 */
public class GamepadReport {

    // Button indices (1-based, matching HID Usage Minimum)
    public static final int BTN_DRS = 1;
    public static final int BTN_ERS = 2;

    private final byte[] report = new byte[4];

    private int steering = 0;
    private int gas = 0;
    private int brake = 0;
    private int buttons = 0;

    /**
     * Set steering value.
     * @param value -127 (full left) to 127 (full right). Clamped.
     */
    public void setSteering(int value) {
        this.steering = clamp(value, -127, 127);
    }

    /**
     * Set gas/throttle level (linear).
     * @param value 0 (no throttle) to 127 (full throttle). Clamped.
     */
    public void setGas(int value) {
        this.gas = clamp(value, 0, 127);
    }

    /**
     * Set brake level (linear).
     * @param value 0 (no brake) to 127 (full brake). Clamped.
     */
    public void setBrake(int value) {
        this.brake = clamp(value, 0, 127);
    }

    /**
     * Set or clear a button by number (1-indexed).
     * @param buttonNumber 1-8 (use BTN_DRS, BTN_ERS constants)
     * @param pressed true to set, false to clear
     */
    public void setButton(int buttonNumber, boolean pressed) {
        if (buttonNumber < 1 || buttonNumber > 8) return;
        int bit = 1 << (buttonNumber - 1);
        if (pressed) {
            buttons |= bit;
        } else {
            buttons &= ~bit;
        }
    }

    /**
     * Toggle a button's state.
     * @return the new state (true = pressed)
     */
    public boolean toggleButton(int buttonNumber) {
        if (buttonNumber < 1 || buttonNumber > 8) return false;
        int bit = 1 << (buttonNumber - 1);
        buttons ^= bit;
        return (buttons & bit) != 0;
    }

    /** Get the current state of a button. */
    public boolean getButton(int buttonNumber) {
        if (buttonNumber < 1 || buttonNumber > 8) return false;
        return (buttons & (1 << (buttonNumber - 1))) != 0;
    }

    public int getSteering() { return steering; }
    public int getGas() { return gas; }
    public int getBrake() { return brake; }

    /**
     * Build and return the 4-byte report array.
     * This returns the same array instance each time (no allocation).
     */
    public byte[] setValue() {
        report[0] = (byte) steering;
        report[1] = (byte) gas;
        report[2] = (byte) brake;
        report[3] = (byte) buttons;
        return report;
    }

    /** Return the current report bytes (for GET_REPORT handling). */
    byte[] getReport() {
        return report;
    }

    /** Reset everything to neutral. */
    public void reset() {
        steering = 0;
        gas = 0;
        brake = 0;
        buttons = 0;
        setValue();
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
