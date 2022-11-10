//  DJI2RemoteControllerStateAdapter.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 10/4/22.
//  Copyright © 2022 Dronelink. All rights reserved.
//
package com.dronelink.dji2.adapters;

import com.dronelink.core.DatedValue;
import com.dronelink.core.adapters.RemoteControllerStateAdapter;
import com.dronelink.core.kernel.core.RemoteControllerButton;
import com.dronelink.core.kernel.core.RemoteControllerStick;
import com.dronelink.core.kernel.core.RemoteControllerWheel;
import com.dronelink.dji2.DJI2ListenerGroup;

import java.util.Date;

import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.DJIKeyInfo;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RemoteControllerKey;

public class DJI2RemoteControllerStateAdapter implements RemoteControllerStateAdapter {
    private final DJI2ListenerGroup listeners = new DJI2ListenerGroup();

    private int dialLeft;
    private int stickLeftHorizontal;
    private int stickLeftVertical;
    private int stickRightHorizontal;
    private int stickRightVertical;
    private boolean pauseButtonDown;
    private boolean goHomeButtonDown;
    private boolean custom1ButtonDown;
    private boolean custom2ButtonDown;

    public DJI2RemoteControllerStateAdapter() {
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyLeftDial), (oldValue, newValue) -> dialLeft = newValue != null ? newValue : 0);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyStickLeftHorizontal), (oldValue, newValue) -> stickLeftHorizontal = newValue != null ? newValue : 0);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyStickLeftVertical), (oldValue, newValue) -> stickLeftVertical = newValue != null ? newValue : 0);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyStickRightHorizontal), (oldValue, newValue) -> stickRightHorizontal = newValue != null ? newValue : 0);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyStickRightVertical), (oldValue, newValue) -> stickRightVertical = newValue != null ? newValue : 0);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyPauseButtonDown), (oldValue, newValue) -> pauseButtonDown = newValue != null && newValue);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyGoHomeButtonDown), (oldValue, newValue) -> goHomeButtonDown = newValue != null && newValue);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyCustomButton1Down), (oldValue, newValue) -> custom1ButtonDown = newValue != null && newValue);
        listeners.init(KeyTools.createKey(RemoteControllerKey.KeyCustomButton2Down), (oldValue, newValue) -> custom2ButtonDown = newValue != null && newValue);
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

    public RemoteControllerStick getLeftStick() {
        return new RemoteControllerStick((double)stickLeftHorizontal / 660.0, (double)stickLeftVertical / 660.0);
    }

    public RemoteControllerWheel getLeftWheel() {
        return new RemoteControllerWheel(true, false, (double)dialLeft / 660.0);
    }

    public RemoteControllerStick getRightStick() {
        return new RemoteControllerStick((double)stickRightHorizontal / 660.0, (double)stickRightVertical / 660.0);
    }

    public RemoteControllerButton getPauseButton() {
        return new RemoteControllerButton(true, pauseButtonDown);
    }

    public RemoteControllerButton getReturnHomeButton() {
        return new RemoteControllerButton(true, goHomeButtonDown);
    }

    public RemoteControllerButton getC1Button() {
        return new RemoteControllerButton(true, custom1ButtonDown);
    }

    public RemoteControllerButton getC2Button() {
        return new RemoteControllerButton(true, custom2ButtonDown);
    }
}