//  DJI2BatteryAdapter.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 11/8/22.
//  Copyright © 2022 Dronelink. All rights reserved.
//
package com.dronelink.dji2.adapters;

import android.util.Log;

import com.dronelink.core.DatedValue;
import com.dronelink.core.adapters.BatteryAdapter;
import com.dronelink.core.adapters.BatteryStateAdapter;
import com.dronelink.dji2.DJI2ListenerGroup;

import dji.sdk.keyvalue.key.BatteryKey;
import dji.sdk.keyvalue.key.KeyTools;

public class DJI2BatteryAdapter implements BatteryAdapter {
    private static final String TAG = DJI2BatteryAdapter.class.getCanonicalName();

    private final int index;
    private String serialNumber;
    private String firmwareVersion;
    private Integer numberOfCells;
    public DJI2BatteryStateAdapter state;

    private final DJI2ListenerGroup listeners = new DJI2ListenerGroup();

    public DJI2BatteryAdapter(final int index) {
        this.index = index;
        state = new DJI2BatteryStateAdapter(index);

        listeners.init(KeyTools.createKey(BatteryKey.KeySerialNumber), (oldValue, newValue) -> {
            if (newValue != null) {
                serialNumber = newValue;
                Log.i(TAG, "Serial number: " + serialNumber);
            }
        });

        listeners.init(KeyTools.createKey(BatteryKey.KeyFirmwareVersion), (oldValue, newValue) -> {
            if (newValue != null) {
                firmwareVersion = newValue;
                Log.i(TAG, "Firmware version: " + firmwareVersion);
            }
        });

        listeners.init(KeyTools.createKey(BatteryKey.KeyNumberOfCells), (oldValue, newValue) -> numberOfCells = newValue);
    }

    public void close() {
        listeners.cancelAll();
        state.close();
    }

    public DatedValue<BatteryStateAdapter> getState() {
        return state.asDatedValue();
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getSerialNumber() {
        return serialNumber;
    }

    @Override
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    @Override
    public Integer getCellCount() {
        return numberOfCells;
    }
}