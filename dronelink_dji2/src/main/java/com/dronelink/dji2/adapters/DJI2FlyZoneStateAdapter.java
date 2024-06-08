package com.dronelink.dji2.adapters;

import android.content.Context;

import com.dronelink.core.DatedValue;
import com.dronelink.core.adapters.FlyZoneStateAdapter;
import com.dronelink.core.kernel.core.Message;

import java.util.Date;
import java.util.List;

public class DJI2FlyZoneStateAdapter implements FlyZoneStateAdapter {
    private final Context context;
    public DJI2FlyZoneStateAdapter(final Context context) {
        this.context = context;
    }

    public DatedValue<FlyZoneStateAdapter> asDatedValue() {
        return new DatedValue<>(this, new Date());
    }

    @Override
    public boolean isLoggedIn() {
        return false;
    }

    @Override
    public List<Message> getStatusMessages() {
        return null;
    }
}
