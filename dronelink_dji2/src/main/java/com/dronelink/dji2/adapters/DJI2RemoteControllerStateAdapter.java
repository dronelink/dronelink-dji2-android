//  DJI2RemoteControllerStateAdapter.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 10/4/22.
//  Copyright © 2022 Dronelink. All rights reserved.
//
package com.dronelink.dji2.adapters;

import android.location.Location;

import com.dronelink.core.DatedValue;
import com.dronelink.core.adapters.RemoteControllerStateAdapter;
import com.dronelink.core.kernel.core.RemoteControllerButton;
import com.dronelink.core.kernel.core.RemoteControllerStick;
import com.dronelink.core.kernel.core.RemoteControllerWheel;
import com.dronelink.dji2.DJI2ListenerGroup;
import com.dronelink.dji2.DronelinkDJI2;

import java.util.Date;

import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.DJIKeyInfo;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.sdk.keyvalue.value.remotecontroller.FiveDimensionPressedStatus;
import dji.sdk.keyvalue.value.remotecontroller.RcGPSInfo;

public class DJI2RemoteControllerStateAdapter implements RemoteControllerStateAdapter {
    private final DJI2ListenerGroup listeners = new DJI2ListenerGroup();

    private RcGPSInfo gpsInfo;
    private int dialLeft;
    private int stickLeftHorizontal;
    private int stickLeftVertical;
    private int stickRightHorizontal;
    private int stickRightVertical;
    private Boolean shutterButtonDown;
    private Boolean videoButtonDown;
    private Boolean pauseButtonDown;
    private Boolean returnHomeButtonDown;
    private Boolean c1ButtonDown;
    private Boolean c2ButtonDown;
    private Boolean c3ButtonDown;
    private FiveDimensionPressedStatus fiveDimensionPressedStatus;

    public DJI2RemoteControllerStateAdapter() {
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyRcGPSInfo), (oldValue, newValue) -> gpsInfo = newValue);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyLeftDial), (oldValue, newValue) -> dialLeft = newValue != null ? newValue : 0);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyStickLeftHorizontal), (oldValue, newValue) -> stickLeftHorizontal = newValue != null ? newValue : 0);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyStickLeftVertical), (oldValue, newValue) -> stickLeftVertical = newValue != null ? newValue : 0);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyStickRightHorizontal), (oldValue, newValue) -> stickRightHorizontal = newValue != null ? newValue : 0);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyStickRightVertical), (oldValue, newValue) -> stickRightVertical = newValue != null ? newValue : 0);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyShutterButtonDown), (oldValue, newValue) -> shutterButtonDown = newValue);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyRecordButtonDown), (oldValue, newValue) -> videoButtonDown = newValue);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyPauseButtonDown), (oldValue, newValue) -> pauseButtonDown = newValue);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyGoHomeButtonDown), (oldValue, newValue) -> returnHomeButtonDown = newValue);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyCustomButton1Down), (oldValue, newValue) -> c1ButtonDown = newValue);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyCustomButton2Down), (oldValue, newValue) -> c2ButtonDown = newValue);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyCustomButton3Down), (oldValue, newValue) -> c3ButtonDown = newValue);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyFiveDimensionPressedStatus), (oldValue, newValue) -> fiveDimensionPressedStatus = newValue);
    }

    public void close() {
        listeners.cancelAll();
    }

    public DatedValue<RemoteControllerStateAdapter> asDatedValue() {
        return new DatedValue<>(this, new Date());
    }

    public <T> DJIKey<T> createKey(final DJIKeyInfo<T> keyInfo) {
        return KeyTools.createKey(keyInfo);
    }

    @Override
    public Location getLocation() {
        return DronelinkDJI2.getLocation(gpsInfo);
    }

    public RemoteControllerStick getLeftStick() {
        return new RemoteControllerStick((double)stickLeftHorizontal / 660.0, (double)stickLeftVertical / 660.0);
    }

    public RemoteControllerWheel getLeftWheel() {
        return new RemoteControllerWheel(true, false, (double)dialLeft / 660.0);
    }

    public RemoteControllerStick getRightStick() {
        return new RemoteControllerStick((double)stickRightHorizontal / 660.0, (double)stickRightVertical / 660.0);
    }

    @Override
    public RemoteControllerButton getCaptureButton() {
        return new RemoteControllerButton(shutterButtonDown != null, shutterButtonDown != null && shutterButtonDown);
    }

    @Override
    public RemoteControllerButton getVideoButton() {
        return new RemoteControllerButton(videoButtonDown != null, videoButtonDown != null && videoButtonDown);
    }

    @Override
    public RemoteControllerButton getPhotoButton() {
        return null;
    }

    public RemoteControllerButton getPauseButton() {
        return new RemoteControllerButton(pauseButtonDown != null, pauseButtonDown != null && pauseButtonDown);
    }

    public RemoteControllerButton getReturnHomeButton() {
        return new RemoteControllerButton(returnHomeButtonDown != null, returnHomeButtonDown != null && returnHomeButtonDown);
    }

    @Override
    public RemoteControllerButton getFunctionButton() {
        return null;
    }

    public RemoteControllerButton getC1Button() {
        return new RemoteControllerButton(c1ButtonDown != null, c1ButtonDown != null && c1ButtonDown);
    }

    public RemoteControllerButton getC2Button() {
        return new RemoteControllerButton(c2ButtonDown != null, c2ButtonDown != null && c2ButtonDown);
    }

    @Override
    public RemoteControllerButton getC3Button() {
        return new RemoteControllerButton(c3ButtonDown != null, c3ButtonDown != null && c3ButtonDown);
    }

    @Override
    public RemoteControllerButton getUpButton() {
        return new RemoteControllerButton(fiveDimensionPressedStatus != null, fiveDimensionPressedStatus != null && fiveDimensionPressedStatus.getUpwards() != null && fiveDimensionPressedStatus.getUpwards());
    }

    @Override
    public RemoteControllerButton getDownButton() {
        return new RemoteControllerButton(fiveDimensionPressedStatus != null, fiveDimensionPressedStatus != null && fiveDimensionPressedStatus.getDownwards() != null && fiveDimensionPressedStatus.getDownwards());
    }

    @Override
    public RemoteControllerButton getLeftButton() {
        return new RemoteControllerButton(fiveDimensionPressedStatus != null, fiveDimensionPressedStatus != null && fiveDimensionPressedStatus.getLeftwards() != null && fiveDimensionPressedStatus.getLeftwards());
    }

    @Override
    public RemoteControllerButton getRightButton() {
        return new RemoteControllerButton(fiveDimensionPressedStatus != null, fiveDimensionPressedStatus != null && fiveDimensionPressedStatus.getRightwards() != null && fiveDimensionPressedStatus.getRightwards());
    }

    @Override
    public RemoteControllerButton getL1Button() {
        return null;
    }

    @Override
    public RemoteControllerButton getL2Button() {
        return null;
    }

    @Override
    public RemoteControllerButton getL3Button() {
        return null;
    }

    @Override
    public RemoteControllerButton getR1Button() {
        return null;
    }

    @Override
    public RemoteControllerButton getR2Button() {
        return null;
    }

    @Override
    public RemoteControllerButton getR3Button() {
        return null;
    }
}