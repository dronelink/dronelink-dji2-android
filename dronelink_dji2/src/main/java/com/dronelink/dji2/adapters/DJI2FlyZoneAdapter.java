package com.dronelink.dji2.adapters;

import android.content.Context;

import com.dronelink.core.DatedValue;
import com.dronelink.core.adapters.FlyZoneAdapter;
import com.dronelink.core.adapters.FlyZoneStateAdapter;

public class DJI2FlyZoneAdapter implements FlyZoneAdapter {
    private final DJI2FlyZoneStateAdapter state;
    public DJI2FlyZoneAdapter(Context context) {
        this.state = new DJI2FlyZoneStateAdapter(context);
    }

    public DatedValue<FlyZoneStateAdapter> getState() {
        return state.asDatedValue();
    }
}
