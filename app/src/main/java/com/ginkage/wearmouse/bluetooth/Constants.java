/*
 * Copyright 2018 Google LLC All Rights Reserved.
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

import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;

/** Handy constants for the HID Report Descriptor and SDP configuration. */
class Constants {

    static final byte ID_KEYBOARD = 1;
    static final byte ID_MOUSE = 2;
    static final byte ID_GAMEPAD = 3;
    static final byte ID_BATTERY = 32;

    private static final byte[] HIDD_REPORT_DESC = {
        // ======== GAMEPAD (Racing Wheel) ========
        (byte) 0x05, (byte) 0x01,        // Usage Page (Generic Desktop)
        (byte) 0x09, (byte) 0x05,        // Usage (Gamepad)
        (byte) 0xA1, (byte) 0x01,        // Collection (Application)
        (byte) 0x85, ID_GAMEPAD,         //   Report ID (3)

        // --- Physical Collection ---
        (byte) 0xA1, (byte) 0x00,        //   Collection (Physical)

        // --- Steering (X axis), signed ---
        (byte) 0x05, (byte) 0x01,        //     Usage Page (Generic Desktop)
        (byte) 0x09, (byte) 0x30,        //     Usage (X) — steering
        (byte) 0x15, (byte) 0x81,        //     Logical Minimum (-127)
        (byte) 0x25, (byte) 0x7F,        //     Logical Maximum (127)
        (byte) 0x75, (byte) 0x08,        //     Report Size (8 bits)
        (byte) 0x95, (byte) 0x01,        //     Report Count (1)
        (byte) 0x81, (byte) 0x02,        //     Input (Data, Variable, Absolute)

        // --- Gas (Z axis), unsigned ---
        (byte) 0x09, (byte) 0x32,        //     Usage (Z) — throttle
        (byte) 0x15, (byte) 0x00,        //     Logical Minimum (0)
        (byte) 0x25, (byte) 0x7F,        //     Logical Maximum (127)
        (byte) 0x75, (byte) 0x08,        //     Report Size (8 bits)
        (byte) 0x95, (byte) 0x01,        //     Report Count (1)
        (byte) 0x81, (byte) 0x02,        //     Input (Data, Variable, Absolute)

        // --- Brake (Rz axis), unsigned ---
        (byte) 0x09, (byte) 0x35,        //     Usage (Rz) — brake
        (byte) 0x15, (byte) 0x00,        //     Logical Minimum (0)
        (byte) 0x25, (byte) 0x7F,        //     Logical Maximum (127)
        (byte) 0x75, (byte) 0x08,        //     Report Size (8 bits)
        (byte) 0x95, (byte) 0x01,        //     Report Count (1)
        (byte) 0x81, (byte) 0x02,        //     Input (Data, Variable, Absolute)

        // --- 8 Buttons (DRS=btn1, ERS=btn2, 3-8 spare) ---
        (byte) 0x05, (byte) 0x09,        //     Usage Page (Button)
        (byte) 0x19, (byte) 0x01,        //     Usage Minimum (Button 1)
        (byte) 0x29, (byte) 0x08,        //     Usage Maximum (Button 8)
        (byte) 0x15, (byte) 0x00,        //     Logical Minimum (0)
        (byte) 0x25, (byte) 0x01,        //     Logical Maximum (1)
        (byte) 0x75, (byte) 0x01,        //     Report Size (1 bit)
        (byte) 0x95, (byte) 0x08,        //     Report Count (8)
        (byte) 0x81, (byte) 0x02,        //     Input (Data, Variable, Absolute)

        (byte) 0xC0,                     //   End Collection (Physical)
        (byte) 0xC0,                     // End Collection (Application)

        // Battery
        (byte) 0x05, (byte) 0x0C, // Usage page (Consumer)
        (byte) 0x09, (byte) 0x01, // Usage (Consumer Control)
        (byte) 0xA1, (byte) 0x01, // Collection (Application)
        (byte) 0x85, ID_BATTERY,  //    Report ID
        (byte) 0x05, (byte) 0x01, //    Usage page (Generic Desktop)
        (byte) 0x09, (byte) 0x06, //    Usage (Keyboard)
        (byte) 0xA1, (byte) 0x02, //    Collection (Logical)
        (byte) 0x05, (byte) 0x06, //       Usage page (Generic Device Controls)
        (byte) 0x09, (byte) 0x20, //       Usage (Battery Strength)
        (byte) 0x15, (byte) 0x00, //       Logical minimum (0)
        (byte) 0x26, (byte) 0xff, (byte) 0x00, // Logical maximum (255)
        (byte) 0x75, (byte) 0x08, //       Report size (8)
        (byte) 0x95, (byte) 0x01, //       Report count (3)
        (byte) 0x81, (byte) 0x02, //       Input (Data, Variable, Absolute)
        (byte) 0xC0,              //    End Collection
        (byte) 0xC0,              // End Collection
    };

    private static final String SDP_NAME = "WatchWheel";
    private static final String SDP_DESCRIPTION = "Racing Wheel Controller";
    private static final String SDP_PROVIDER = "WearOS";
    private static final int QOS_TOKEN_RATE = 800; // 9 bytes * 1000000 us / 11250 us
    private static final int QOS_TOKEN_BUCKET_SIZE = 9;
    private static final int QOS_PEAK_BANDWIDTH = 0;
    private static final int QOS_LATENCY = 11250;

    static final BluetoothHidDeviceAppSdpSettings SDP_RECORD =
            new BluetoothHidDeviceAppSdpSettings(
                    Constants.SDP_NAME,
                    Constants.SDP_DESCRIPTION,
                    Constants.SDP_PROVIDER,
                    BluetoothHidDevice.SUBCLASS1_NONE,
                    Constants.HIDD_REPORT_DESC);

    static final BluetoothHidDeviceAppQosSettings QOS_OUT =
            new BluetoothHidDeviceAppQosSettings(
                    BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                    Constants.QOS_TOKEN_RATE,
                    Constants.QOS_TOKEN_BUCKET_SIZE,
                    Constants.QOS_PEAK_BANDWIDTH,
                    Constants.QOS_LATENCY,
                    BluetoothHidDeviceAppQosSettings.MAX);
}
