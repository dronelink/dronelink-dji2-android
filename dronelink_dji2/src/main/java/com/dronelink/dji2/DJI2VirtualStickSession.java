//  DJI2VirtualStickSession.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 10/4/22.
//  Copyright © 2022 Dronelink. All rights reserved.
//
package com.dronelink.dji2;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dronelink.core.DatedValue;
import com.dronelink.core.DroneControlSession;
import com.dronelink.core.adapters.RTKStateAdapter;
import com.dronelink.core.kernel.core.Message;
import com.dronelink.core.kernel.core.enums.ExecutionEngine;
import com.dronelink.dji2.adapters.DJI2DroneAdapter;
import com.dronelink.dji2.adapters.DJI2RTKStateAdapter;

import java.util.Date;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthorityChangeReason;
import dji.sdk.keyvalue.value.flightcontroller.FlightMode;
import dji.sdk.keyvalue.value.flightcontroller.GPSSignalLevel;
import dji.sdk.keyvalue.value.rtkmobilestation.RTKLocation;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.rtk.RTKLocationInfo;
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager;
import dji.v5.manager.aircraft.virtualstick.VirtualStickState;
import dji.v5.manager.aircraft.virtualstick.VirtualStickStateListener;

public class DJI2VirtualStickSession implements DroneControlSession, VirtualStickStateListener {
    private static final String TAG = DJI2VirtualStickSession.class.getCanonicalName();

    private enum State {
        TAKEOFF_START,
        TAKEOFF_ATTEMPTING,
        TAKEOFF_COMPLETE,
        VIRTUAL_STICK_START,
        VIRTUAL_STICK_ATTEMPTING,
        FLIGHT_MODE_JOYSTICK_ATTEMPTING,
        FLIGHT_MODE_JOYSTICK_COMPLETE,
        DEACTIVATED
    }

    private final Context context;
    private final DJI2DroneAdapter droneAdapter;

    private State state = State.TAKEOFF_START;
    private int virtualStickAttempts = 0;
    private Date virtualStickAttemptPrevious = null;
    private Date flightModeJoystickAttemptingStarted = null;
    private Message attemptDisengageReason = null;
    private FlightControlAuthorityChangeReason reason;

    public DJI2VirtualStickSession(final Context context, final DJI2DroneAdapter droneAdapter) {
        this.context = context;
        this.droneAdapter = droneAdapter;
    }

    @Override
    public ExecutionEngine getExecutionEngine() {
        return ExecutionEngine.DRONELINK_KERNEL;
    }

    @Override
    public Message getDisengageReason() {
        if (attemptDisengageReason != null) {
            return attemptDisengageReason;
        }

        final DatedValue<RTKStateAdapter> rtkState = droneAdapter.getRTKState();
        if (rtkState != null && rtkState.value != null && rtkState.value.isEnabled() && rtkState.value.isConnected()) {
            if (rtkState.value instanceof DJI2RTKStateAdapter) {
                final DJI2RTKStateAdapter djiRTKState = (DJI2RTKStateAdapter)rtkState.value;
                final RTKLocationInfo locationInfo = djiRTKState.locationInfo;
                if (locationInfo != null) {
                    final RTKLocation rtkLocation = locationInfo.getRtkLocation();
                    switch (rtkLocation.getPositioningSolution()) {
                        case FLOAT:
                        case FIXED_POINT:
                            break;
                        case SINGLE_POINT:
                            return new Message(context.getString(R.string.MissionDisengageReason_rtk_positioning_solution_unacceptable_title), context.getString(R.string.DronelinkDJI2_RTKPositioningSolution_value_SINGLE_POINT));
                        case NONE:
                        case UNKNOWN:
                            return new Message(context.getString(R.string.MissionDisengageReason_rtk_positioning_solution_unavailable_title));
                    }
                }
            }
        }

        if (droneAdapter.state.getGPSSignalStrength() < DronelinkDJI2.getGPSSignalStrength(GPSSignalLevel.LEVEL_3)) {
            return new Message(context.getString(R.string.MissionDisengageReason_gps_signal_level_weak_title));
        }

        if (state == State.FLIGHT_MODE_JOYSTICK_COMPLETE) {
            final FlightControlAuthorityChangeReason reason = this.reason;
            if (reason != null) {
                return new Message(context.getString(R.string.MissionDisengageReason_drone_control_override_title), DronelinkDJI2.getString(context, reason));
            }

            final FlightMode flightMode = droneAdapter.state.flightMode;
            if (flightMode != null) {
                switch (flightMode) {
                    case VIRTUAL_STICK:
                    case GPS_NORMAL:
                    case APAS:
                        break;

                    default:
                        return new Message(context.getString(R.string.MissionDisengageReason_drone_control_override_title), droneAdapter.state.getMode());
                }
            }
        }

        return null;
    }

    @Override
    public boolean isReengaging() {
        return false;
    }

    @Override
    public Boolean activate() {
        switch (state) {
            case TAKEOFF_START:
                if (droneAdapter.state.isFlying()) {
                    state = State.TAKEOFF_COMPLETE;
                    return activate();
                }

                state = State.TAKEOFF_ATTEMPTING;
                Log.i(TAG, "Attempting precision takeoff");

                KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyPrecisionStartTakeoff), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                    @Override
                    public void onSuccess(final EmptyMsg emptyMsg) {
                        Log.i(TAG, "Precision takeoff succeeded");
                        state = State.TAKEOFF_COMPLETE;
                    }

                    @Override
                    public void onFailure(final @NonNull IDJIError error) {
                        Log.e(TAG, "Precision takeoff failed: " + error.description());
                        Log.i(TAG, "Attempting takeoff");

                        KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStartTakeoff), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                            @Override
                            public void onSuccess(final EmptyMsg emptyMsg) {
                                Log.i(TAG, "Takeoff succeeded");
                                state = State.TAKEOFF_COMPLETE;
                            }

                            @Override
                            public void onFailure(final @NonNull IDJIError error) {
                                Log.e(TAG, "Takeoff failed: " + error.description());
                                attemptDisengageReason = new Message(context.getString(R.string.MissionDisengageReason_take_off_failed_title), error.description());
                                deactivate();
                            }
                        });
                    }
                });
                return null;

            case TAKEOFF_ATTEMPTING:
                return null;

            case TAKEOFF_COMPLETE:
                if (droneAdapter.state.isFlying() && droneAdapter.state.flightMode != FlightMode.AUTO_TAKE_OFF) {
                    state = State.VIRTUAL_STICK_START;
                    return activate();
                }
                return null;

            case VIRTUAL_STICK_START:
                if (virtualStickAttemptPrevious == null || (System.currentTimeMillis() - virtualStickAttemptPrevious.getTime()) > 2000) {
                    state = State.VIRTUAL_STICK_ATTEMPTING;
                    virtualStickAttemptPrevious = new Date();
                    virtualStickAttempts += 1;

                    Log.i(TAG, String.format("Attempting virtual stick mode control: %d", virtualStickAttempts));
                    VirtualStickManager.getInstance().enableVirtualStick(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);

                            Log.i(TAG, "Virtual stick control enabled");
                            flightModeJoystickAttemptingStarted = new Date();
                            state = State.FLIGHT_MODE_JOYSTICK_ATTEMPTING;
                        }

                        @Override
                        public void onFailure(final @NonNull IDJIError error) {
                            if (virtualStickAttempts >= 5) {
                                final String messageDetails = error.description() == null || error.description().isEmpty()
                                        ? context.getString(R.string.MissionDisengageReason_take_control_failed_rc_mode_verify_details)
                                        : error.description();
                                attemptDisengageReason = new Message(context.getString(R.string.MissionDisengageReason_take_control_failed_title), messageDetails);
                                deactivate();
                            }
                            else {
                                state = State.VIRTUAL_STICK_START;
                            }
                        }
                    });
                }
                return null;

            case VIRTUAL_STICK_ATTEMPTING:
                return null;

            case FLIGHT_MODE_JOYSTICK_ATTEMPTING:
                if (droneAdapter.state.flightMode == FlightMode.VIRTUAL_STICK) {
                    Log.i(TAG, "Flight mode joystick achieved");
                    state = State.FLIGHT_MODE_JOYSTICK_COMPLETE;
                    VirtualStickManager.getInstance().setVirtualStickStateListener(this);
                    return activate();
                }

                if (flightModeJoystickAttemptingStarted != null) {
                    if ((System.currentTimeMillis() - flightModeJoystickAttemptingStarted.getTime()) > 2000) {
                        attemptDisengageReason = new Message(context.getString(R.string.MissionDisengageReason_take_control_failed_title),
                                context.getString(R.string.MissionDisengageReason_take_control_failed_rc_mode_verify_details));
                        deactivate();
                        return false;
                    }
                }

                droneAdapter.sendResetVelocityCommand();
                return null;

            case FLIGHT_MODE_JOYSTICK_COMPLETE:
                return true;

            case DEACTIVATED:
                return false;
        }
        return false;
    }

    @Override
    public void deactivate() {
        VirtualStickManager.getInstance().removeVirtualStickStateListener(this);
        droneAdapter.sendResetVelocityCommand();
        VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Virtual stick control disabled");
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                Log.i(TAG, "Unable to disable virtual stick control");
            }
        });
        state = State.DEACTIVATED;
    }
    @Override
    public void onVirtualStickStateUpdate(final @NonNull VirtualStickState stickState) {
    }

    @Override
    public void onChangeReasonUpdate(final @NonNull FlightControlAuthorityChangeReason reason) {
        this.reason = reason;
    }
}