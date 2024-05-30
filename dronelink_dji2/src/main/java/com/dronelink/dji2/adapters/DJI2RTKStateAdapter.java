//  DJI2RTKStateAdapter.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 3/21/23.
//  Copyright © 2023 Dronelink. All rights reserved.
//
package com.dronelink.dji2.adapters;

import android.annotation.SuppressLint;
import android.content.Context;

import com.dronelink.core.DatedValue;
import com.dronelink.core.adapters.RTKStateAdapter;
import com.dronelink.core.kernel.core.Message;
import com.dronelink.core.kernel.core.enums.RTKReferenceStationSource;
import com.dronelink.core.kernel.core.enums.RTKServiceState;
import com.dronelink.dji2.DJI2ListenerGroup;
import com.dronelink.dji2.DronelinkDJI2;
import com.dronelink.dji2.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationConnetState;
import dji.sdk.keyvalue.value.rtkmobilestation.RTKPositioningSolution;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.aircraft.rtk.RTKCenter;
import dji.v5.manager.aircraft.rtk.RTKLocationInfo;
import dji.v5.manager.aircraft.rtk.RTKLocationInfoListener;
import dji.v5.manager.aircraft.rtk.RTKSystemState;
import dji.v5.manager.aircraft.rtk.RTKSystemStateListener;
import dji.v5.manager.aircraft.rtk.network.INetworkServiceInfoListener;
import dji.v5.manager.aircraft.rtk.station.ConnectedRTKStationInfo;
import dji.v5.manager.aircraft.rtk.station.ConnectedRTKStationInfoListener;
import dji.v5.manager.aircraft.rtk.station.RTKStationConnectStatusListener;
import dji.v5.manager.interfaces.INetworkRTKManager;
import dji.v5.manager.interfaces.IRTKCenter;
import dji.v5.manager.interfaces.IRTKStationManager;

public class DJI2RTKStateAdapter implements RTKStateAdapter, RTKSystemStateListener, INetworkServiceInfoListener {
    private final Context context;
    private final DJI2DroneAdapter drone;
    private final DJI2ListenerGroup listeners = new DJI2ListenerGroup();

    private RTKSystemState systemState;
    private final RTKLocationInfoListener locationInfoListener;
    private RTKLocationInfo locationInfo;
    private dji.sdk.keyvalue.value.rtkbasestation.RTKServiceState serviceState;
    private IDJIError customNetworkError;
    private RTKStationConnectStatusListener stationConnectStatusListener;
    private RTKStationConnetState stationConnectionState;
    private ConnectedRTKStationInfoListener stationInfoListener;
    private ConnectedRTKStationInfo stationInfo;

    public DJI2RTKStateAdapter(final Context context, final DJI2DroneAdapter drone) {
        this.context = context;
        this.drone = drone;

        locationInfoListener = newValue -> locationInfo = newValue;

        final IRTKCenter rtk = RTKCenter.getInstance();
        if (rtk != null) {
            rtk.addRTKLocationInfoListener(locationInfoListener);
            rtk.addRTKSystemStateListener(this);
            final INetworkRTKManager rtkNetworkManager = rtk.getCustomRTKManager();
            if (rtkNetworkManager != null) {
                rtk.getCustomRTKManager().addNetworkRTKServiceInfoListener(this);
            }

            final IRTKStationManager rtkStationManager = rtk.getRTKStationManager();
            if (rtkStationManager != null) {
                stationConnectStatusListener = state -> stationConnectionState = state;
                rtkStationManager.addRTKStationConnectStatusListener(stationConnectStatusListener);
                stationInfoListener = info -> stationInfo = info;
                rtkStationManager.addConnectedRTKStationInfoListener(stationInfoListener);
            }
        }
    }

    public void close() {
        listeners.cancelAll();
        final IRTKCenter rtk = RTKCenter.getInstance();
        if (rtk != null) {
            rtk.removeRTKLocationInfoListener(locationInfoListener);
            rtk.removeRTKSystemStateListener(this);
            final INetworkRTKManager rtkNetworkManager = rtk.getCustomRTKManager();
            if (rtkNetworkManager != null) {
                rtk.getCustomRTKManager().removeNetworkRTKServiceInfoListener(this);
            }

            final IRTKStationManager rtkStationManager = rtk.getRTKStationManager();
            if (rtkStationManager != null) {
                rtkStationManager.removeRTKStationConnectStatusListener(stationConnectStatusListener);
                rtkStationManager.removeConnectedRTKStationInfoListener(stationInfoListener);
            }
        }
    }

    public DatedValue<RTKStateAdapter> asDatedValue() {
        return new DatedValue<>(this, new Date());
    }

    @Override
    public boolean isConnected() {
        if (systemState != null && systemState.getRTKConnected()) {
            return true;
        }

        return stationConnectionState != null && stationConnectionState == RTKStationConnetState.CONNECTED;
    }

    @Override
    public boolean isEnabled() {
        if (systemState != null && systemState.getIsRTKEnabled()) {
            return true;
        }

        return stationInfo != null;
    }

    @Override
    public boolean isMaintainAccuracyEnabled() {
        return systemState != null && systemState.getRTKMaintainAccuracyEnabled();
    }

    @Override
    public RTKReferenceStationSource getReferenceStationSource() {
        return systemState == null ? null : DronelinkDJI2.getRTKReferenceStationSource(systemState.getRtkReferenceStationSource());
    }

    @Override
    public RTKServiceState getServiceState() {
        return DronelinkDJI2.getRTKServiceState(serviceState);
    }

    @Override
    public boolean isHealthy() {
        return systemState != null && systemState.getRTKHealthy();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public List<Message> getStatusMessages() {
        final List<Message> messages = new ArrayList<>();

        Message message = customNetworkError == null ? null : DronelinkDJI2.getMessage(context, customNetworkError);
        if (message != null) {
            messages.add(message);
        }

        LocationCoordinate3D location = null;
        RTKPositioningSolution positioningSolution = null;
        if (locationInfo != null) {
            location = locationInfo.getReal3DLocation();
            if (location != null) {
                if (location.getLongitude() == 0 && location.getLatitude() == 0) {
                    location = null;
                }
            }

            if (locationInfo.getRtkLocation() != null) {
                positioningSolution = locationInfo.getRtkLocation().getPositioningSolution();
            }
        }

        if (getServiceState() == RTKServiceState.TRANSMITTING) {
            message = DronelinkDJI2.getMessage(context, positioningSolution);
            if (message != null) {
                messages.add(message);
            }

            if (location == null) {
                messages.add(new Message(context.getString(R.string.DJI2RTKStateAdapter_custom_network), context.getString(R.string.DJI2RTKStateAdapter_custom_network_location_unavailable), Message.Level.ERROR));
            }
        }

        message = serviceState == null ? null : DronelinkDJI2.getMessage(context, serviceState);
        if (message != null) {
            messages.add(message);
        }

        message = systemState == null ? null : DronelinkDJI2.getMessage(context, systemState.getError());
        if (message != null) {
            messages.add(message);
        }

        if (getServiceState() == RTKServiceState.TRANSMITTING) {
            if (location != null) {
                messages.add(new Message(context.getString(R.string.DJI2RTKStateAdapter_custom_network_location), String.format("%.6f", location.getLatitude()) + ", " + String.format("%.6f", location.getLongitude()), Message.Level.INFO));
            }
        }

        final ConnectedRTKStationInfo stationInfo = this.stationInfo;
        if (stationInfo != null) {
            //TODO localize
            messages.add(new Message("Signal", (int)((stationInfo.getSignalLevel().doubleValue() / 5.0) * 100.0) + "%", stationInfo.getSignalLevel() < 2 ? Message.Level.WARNING :  Message.Level.INFO));
            messages.add(new Message("Battery", stationInfo.getBatteryCapacityPercent() + "%", stationInfo.getBatteryCapacityPercent() < 20 ? Message.Level.WARNING :  Message.Level.INFO));
            messages.add(new Message("Station ID", stationInfo.getStationId().toString()));
        }

        return messages;
    }

    @Override
    public void onErrorCodeUpdate(final IDJIError code) {
        this.customNetworkError = code;
    }

    @Override
    public void onUpdate(final RTKSystemState rtkSystemState) {
        this.systemState = rtkSystemState;
    }

    @Override
    public void onServiceStateUpdate(final dji.sdk.keyvalue.value.rtkbasestation.RTKServiceState rtkServiceState) {
        this.serviceState = rtkServiceState;
    }
}