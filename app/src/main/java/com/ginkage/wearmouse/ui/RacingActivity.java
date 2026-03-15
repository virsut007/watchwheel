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

import android.os.Bundle;
import android.view.WindowManager;

import android.support.wearable.activity.WearableActivity;

import com.ginkage.wearmouse.bluetooth.HidDataSender;
import com.ginkage.wearmouse.bluetooth.GamepadReport;
import com.ginkage.wearmouse.input.SteeringManager;

/**
 * Racing wheel controller screen.
 * Z-axis gyro steering + multitouch THR/BRK + DRS/ERS toggles.
 */
public class RacingActivity extends WearableActivity
        implements RacingView.RacingInputListener, SteeringManager.SteeringListener {

    private HidDataSender hidDataSender;
    private SteeringManager steeringManager;
    private RacingView racingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        racingView = new RacingView(this);
        racingView.setListener(this);
        setContentView(racingView);

        hidDataSender = HidDataSender.getInstance();
        steeringManager = new SteeringManager(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        steeringManager.start();
        racingView.setConnected(hidDataSender.isConnected());
    }

    @Override
    protected void onPause() {
        super.onPause();
        steeringManager.stop();
        hidDataSender.getGamepadReport().reset();
        hidDataSender.sendGamepad();
    }

    // ---- SteeringManager.SteeringListener ----

    @Override
    public void onSteeringChanged(int value) {
        hidDataSender.getGamepadReport().setSteering(value);
        racingView.setSteering(value);
        sendReport();
    }

    // ---- RacingView.RacingInputListener ----

    @Override
    public void onGasChanged(int value) {
        hidDataSender.getGamepadReport().setGas(value);
        sendReport();
    }

    @Override
    public void onBrakeChanged(int value) {
        hidDataSender.getGamepadReport().setBrake(value);
        sendReport();
    }

    @Override
    public void onDrsToggled(boolean active) {
        hidDataSender.getGamepadReport().setButton(GamepadReport.BTN_DRS, active);
        sendReport();
    }

    @Override
    public void onErsToggled(boolean active) {
        hidDataSender.getGamepadReport().setButton(GamepadReport.BTN_ERS, active);
        sendReport();
    }

    @Override
    public void onCalibrateRequested() {
        steeringManager.calibrate();
    }

    private void sendReport() {
        hidDataSender.sendGamepad();
    }
}
