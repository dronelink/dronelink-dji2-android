//  DJI2DroneSession.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 10/4/22.
//  Copyright © 2022 Dronelink. All rights reserved.
//
package com.dronelink.dji2;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dronelink.core.Convert;
import com.dronelink.core.DatedValue;
import com.dronelink.core.DroneControlSession;
import com.dronelink.core.DroneSession;
import com.dronelink.core.DroneSessionManager;
import com.dronelink.core.Dronelink;
import com.dronelink.core.Executor;
import com.dronelink.core.MissionExecutor;
import com.dronelink.core.ModeExecutor;
import com.dronelink.core.adapters.BatteryStateAdapter;
import com.dronelink.core.adapters.CameraAdapter;
import com.dronelink.core.adapters.CameraStateAdapter;
import com.dronelink.core.adapters.DroneAdapter;
import com.dronelink.core.adapters.DroneStateAdapter;
import com.dronelink.core.adapters.GimbalAdapter;
import com.dronelink.core.adapters.GimbalStateAdapter;
import com.dronelink.core.adapters.RTKAdapter;
import com.dronelink.core.adapters.RTKStateAdapter;
import com.dronelink.core.adapters.RemoteControllerAdapter;
import com.dronelink.core.adapters.RemoteControllerStateAdapter;
import com.dronelink.core.command.Command;
import com.dronelink.core.command.CommandError;
import com.dronelink.core.command.CommandQueue;
import com.dronelink.core.command.MultiChannelCommandQueue;
import com.dronelink.core.kernel.command.camera.CameraCommand;
import com.dronelink.core.kernel.command.camera.ModeCameraCommand;
import com.dronelink.core.kernel.command.drone.DroneCommand;
import com.dronelink.core.kernel.command.gimbal.GimbalCommand;
import com.dronelink.core.kernel.command.gimbal.ModeGimbalCommand;
import com.dronelink.core.kernel.command.gimbal.OrientationGimbalCommand;
import com.dronelink.core.kernel.command.remotecontroller.RemoteControllerCommand;
import com.dronelink.core.kernel.command.rtk.RTKCommand;
import com.dronelink.core.kernel.core.Message;
import com.dronelink.core.kernel.core.enums.ExecutionEngine;
import com.dronelink.dji2.adapters.DJI2CameraAdapter;
import com.dronelink.dji2.adapters.DJI2DroneAdapter;
import com.dronelink.dji2.adapters.DJI2GimbalAdapter;
import com.dronelink.dji2.adapters.DJI2RTKAdapter;
import com.dronelink.dji2.adapters.DJI2RemoteControllerAdapter;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dji.sdk.errorcode.DJIErrorCode;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthorityChangeReason;
import dji.sdk.keyvalue.value.flightcontroller.FlightMode;
import dji.sdk.keyvalue.value.gimbal.GimbalSpeedRotation;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.rtk.RTKCenter;

//TODO onVideoFeedSourceUpdated
public class DJI2DroneSession implements DroneSession, DJI2DroneAdapter.CameraFileGeneratedCallback {
    private static final String TAG = DJI2DroneSession.class.getCanonicalName();

    private String id = UUID.randomUUID().toString();
    private final Context context;
    private final DroneSessionManager manager;
    private final DJI2DroneAdapter droneAdapter;
    public boolean initialized = false;
    public boolean located = false;

    private final DJI2ListenerGroup djiListeners = new DJI2ListenerGroup();

    private final Date opened = new Date();
    private boolean closed = false;
    public boolean isClosed() {
        return closed;
    }

    private final List<Listener> listeners = new LinkedList<>();
    private final ExecutorService listenerExecutor = Executors.newSingleThreadExecutor();

    private final CommandQueue droneCommands = new CommandQueue();
    private final MultiChannelCommandQueue remoteControllerCommands = new MultiChannelCommandQueue();
    private final MultiChannelCommandQueue cameraCommands = new MultiChannelCommandQueue();
    private final MultiChannelCommandQueue gimbalCommands = new MultiChannelCommandQueue();

    public DJI2DroneSession(final Context context, final DroneSessionManager manager) {
        Log.i(TAG, "Drone session opened (" + id + ")");

        this.context = context;
        this.manager = manager;
        this.droneAdapter = new DJI2DroneAdapter(context, new CommonCallbacks.CompletionCallbackWithParam<String>() {
            @Override
            public void onSuccess(final String s) {
                if (!initialized) {
                    initialized = true;
                    onInitialized();
                }
            }

            @Override
            public void onFailure(final @NonNull IDJIError error) {
            }
        }, this);

        djiListeners.init(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D), (oldValue, newValue) -> {
            if (newValue != null && initialized && !located) {
                located = true;
                onLocated();
            }
        });
        djiListeners.init(KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn), (oldValue, newValue) -> onMotorsChanged(newValue != null && newValue));

        runCommandThread();
    }

    private void runCommandThread() {
        new Thread() {
            @Override
            public void run() {
                try {
                    while (!closed) {
                        droneCommands.process();
                        remoteControllerCommands.process();
                        cameraCommands.process();
                        gimbalCommands.process();

                        final MissionExecutor missionExecutor = Dronelink.getInstance().getMissionExecutor();
                        final ModeExecutor modeExecutor = Dronelink.getInstance().getModeExecutor();
                        final boolean missionExecutorEngaged = (missionExecutor != null && missionExecutor.isEngaged());
                        if (missionExecutorEngaged || (modeExecutor != null && modeExecutor.isEngaged())) {
                            for (final GimbalAdapter gimbalAdapter : droneAdapter.getGimbals()) {
                                //don't issue competing speed rotations, OrientationGimbalCommand always takes precedent
                                boolean activeOrientationCommand = false;
                                final CommandQueue queue = gimbalCommands.get(gimbalAdapter.getIndex());
                                if (queue != null) {
                                    final Command currentCommand = queue.getCurrentCommand();
                                    activeOrientationCommand = currentCommand != null && currentCommand.kernelCommand instanceof OrientationGimbalCommand;
                                }

                                if (!activeOrientationCommand) {
                                    if (gimbalAdapter instanceof DJI2GimbalAdapter) {
                                        final DJI2GimbalAdapter djiGimbalAdapter = (DJI2GimbalAdapter) gimbalAdapter;
                                        GimbalSpeedRotation speedRotation = djiGimbalAdapter.getPendingSpeedRotation();
                                        djiGimbalAdapter.setPendingSpeedRotation(null);

                                        //work-around for this issue: https://support.dronelink.com/hc/en-us/community/posts/360034749773-Seeming-to-have-a-Heading-error-
                                        if (speedRotation == null) {
                                            speedRotation = new GimbalSpeedRotation();
                                        }

                                        //this doesn't work because droneAdapter.getState().value.getOrientation().getYaw() is wrong!
//                                        double yawRelativeToAircraftHeading = Convert.RadiansToDegrees(Convert.AngleDifferenceSigned(
//                                                djiGimbalAdapter.getState().value.getOrientation().getYaw(),
//                                                droneAdapter.getState().value.getOrientation().getYaw()));
//                                        speedRotation.setYaw(Math.min(Math.max(-yawRelativeToAircraftHeading * 1.5, -25.0), 25.0));

                                        if (missionExecutorEngaged) {
                                            //TODO final DatedValue<Integer> remoteControllerGimbalChannel = state.remoteControllerGimbalChannel;
                                            final int channel = 0; //TODO remoteControllerGimbalChannel == null || remoteControllerGimbalChannel.value == null ? 0 : remoteControllerGimbalChannel.value;
                                            if (channel == gimbalAdapter.getIndex()) {
                                                final DatedValue<RemoteControllerStateAdapter> remoteControllerState = getRemoteControllerState(channel);
                                                if (remoteControllerState != null && remoteControllerState.value != null && remoteControllerState.value.getLeftWheel().value != 0) {
                                                    speedRotation.setPitch(remoteControllerState.value.getLeftWheel().value * 10);
                                                }
                                            }
                                        }

                                        KeyManager.getInstance().performAction(djiGimbalAdapter.createKey(GimbalKey.KeyRotateBySpeed), speedRotation, null);
                                    }
                                }
                            }
                        }

                        sleep(100);
                    }
                }
                catch (final InterruptedException ignored) {}
            }
        }.start();
    }

    @Override
    public DroneSessionManager getManager() {
        return manager;
    }

    @Override
    public DroneAdapter getDrone() {
        return droneAdapter;
    }

    @Override
    public DatedValue<DroneStateAdapter> getState() {
        return droneAdapter.getState();
    }

    @Override
    public Date getOpened() {
        return opened;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getManufacturer() {
        return "DJI";
    }

    @Override
    public String getSerialNumber() {
        return droneAdapter.serialNumber;
    }

    @Override
    public String getName() {
        return droneAdapter.name;
    }

    @Override
    public String getModel() {
        return DronelinkDJI2.getString(context, droneAdapter.productType);
    }

    @Override
    public String getFirmwarePackageVersion() {
        return droneAdapter.firmwarePackageVersion;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public boolean isLocated() {
        return located;
    }

    @Override
    public boolean isTelemetryDelayed() {
        return System.currentTimeMillis() - getState().date.getTime() > 2000;
    }

    @Override
    public Message getDisengageReason() {
        if (closed) {
            return new Message(context.getString(R.string.MissionDisengageReason_drone_disconnected_title));
        }

        if (droneAdapter.state.flightMode == null) {
            return new Message(context.getString(R.string.MissionDisengageReason_telemetry_unavailable_title));
        }

        if (isTelemetryDelayed()) {
            return new Message(context.getString(R.string.MissionDisengageReason_telemetry_delayed_title), context.getString(R.string.MissionDisengageReason_telemetry_delayed_details));
        }

        if (droneAdapter.state.isOutOfDistanceLimit) {
            return new Message(context.getString(R.string.MissionDisengageReason_drone_max_distance_title), context.getString(R.string.MissionDisengageReason_drone_max_distance_details));
        }

        switch (droneAdapter.state.flightMode) {
            case VIRTUAL_STICK:
            case GPS_NORMAL:
                break;

            case GPS_SPORT:
            case GPS_TRIPOD:
                return new Message(context.getString(R.string.MissionDisengageReason_take_control_failed_title), context.getString(R.string.MissionDisengageReason_take_control_failed_rc_mode_details));

            default:
                return new Message(context.getString(R.string.MissionDisengageReason_take_control_failed_title), context.getString(R.string.MissionDisengageReason_take_control_failed_flight_mode_details));
        }

        return null;
    }

    @Override
    public void identify(final String id) {
        this.id = id;
    }

    @Override
    public void addListener(final Listener listener) {
        final DroneSession self = this;
        listenerExecutor.execute(() -> {
            listeners.add(listener);

            if (isInitialized()) {
                listener.onInitialized(self);
            }

            if (isLocated()) {
                listener.onLocated(self);
            }
        });
    }

    @Override
    public void removeListener(final Listener listener) {
        listenerExecutor.execute(() -> listeners.remove(listener));
    }

    private void onInitialized() {
        Log.i(TAG, "Drone session initialized: " + getSerialNumber());
        final DJI2DroneSession self = this;
        listenerExecutor.execute(() -> {
            for (final Listener listener : listeners) {
                listener.onInitialized(self);
            }
        });
    }

    private void onLocated() {
        Log.i(TAG, "Drone session located");

        final DJI2DroneSession self = this;
        listenerExecutor.execute(() -> {
            for (final Listener listener : listeners) {
                listener.onLocated(self);
            }
        });
    }

    private void onMotorsChanged(final boolean value) {
        final DJI2DroneSession self = this;
        listenerExecutor.execute(() -> {
            for (final Listener listener : listeners) {
                listener.onMotorsChanged(self, value);
            }
        });
    }

    private void onCommandExecuted(final com.dronelink.core.kernel.command.Command command) {
        final DJI2DroneSession self = this;
        listenerExecutor.execute(() -> {
            for (final Listener listener : listeners) {
                listener.onCommandExecuted(self, command);
            }
        });
    }

    private void onCommandFinished(final com.dronelink.core.kernel.command.Command command, final CommandError error) {
        final DJI2DroneSession self = this;
        listenerExecutor.execute(() -> {
            for (final Listener listener : listeners) {
                listener.onCommandFinished(self, command, error);
            }
        });
    }

    public void onCameraFileGenerated(final DJI2CameraFile file) {
        final DJI2DroneSession self = this;
        listenerExecutor.execute(() -> {
            for (final Listener listener : listeners) {
                listener.onCameraFileGenerated(self, file);
            }
        });
    }

    @Override
    public void addCommand(final com.dronelink.core.kernel.command.Command command) throws CommandTypeUnhandledException, Dronelink.UnregisteredException {
        Command.Executor executor = null;

        if (command instanceof DroneCommand) {
            executor = finished -> {
                onCommandExecuted(command);
                finished.execute(null);
                return droneAdapter.executeCommand(context, (DroneCommand)command, finished);
            };
        }
        else if (command instanceof RemoteControllerCommand) {
            executor = finished -> {
                onCommandExecuted(command);
                finished.execute(null);
                final RemoteControllerAdapter remoteController = droneAdapter.getRemoteController(((RemoteControllerCommand) command).channel);
                if (remoteController == null) {
                    return new CommandError(context.getString(R.string.MissionDisengageReason_drone_remote_controller_unavailable_title));
                }
                return ((DJI2RemoteControllerAdapter)remoteController).executeCommand(context, (RemoteControllerCommand) command, finished);
            };
        }
        else if (command instanceof RTKCommand) {
            executor = finished -> {
                onCommandExecuted(command);
                finished.execute(null);
                final RTKAdapter rtk = droneAdapter.getRTK();
                if (rtk == null) {
                    return new CommandError(context.getString(R.string.MissionDisengageReason_rtk_unavailable_title));
                }
                return ((DJI2RTKAdapter)rtk).executeCommand(context, (RTKCommand) command, finished);
            };
        }
        else if (command instanceof CameraCommand) {
            executor = finished -> {
                onCommandExecuted(command);
                final CameraAdapter camera = droneAdapter.getCamera(((CameraCommand) command).channel);
                if (camera == null) {
                    return new CommandError(context.getString(R.string.MissionDisengageReason_drone_camera_unavailable_title));
                }
                return ((DJI2CameraAdapter)camera).executeCommand(context, (CameraCommand)command, finished);
            };
        }
        else if (command instanceof GimbalCommand) {
            executor = finished -> {
                onCommandExecuted(command);
                final GimbalAdapter gimbal = droneAdapter.getGimbal(((GimbalCommand) command).channel);
                if (gimbal == null) {
                    return new CommandError(context.getString(R.string.MissionDisengageReason_drone_gimbal_unavailable_title));
                }
                return ((DJI2GimbalAdapter)gimbal).executeCommand(context, (GimbalCommand)command, finished);
            };
        }

        if (executor != null) {
            final Command c = new Command(
                    command,
                    executor,
                    error -> onCommandFinished(command, error),
                    command.getConfig());

            if (c.config.retriesEnabled == null) {
                //disable retries when the DJI SDK reports that the product does not support the feature
                c.config.retriesEnabled = error -> error == null || error.code != DJIErrorCode.UNSUPPORTED_COMMAND.value();

                if (c.config.finishDelayMillis == null) {
                    //adding a 1.5 second delay after camera and gimbal mode commands
                    if (command instanceof ModeCameraCommand || command instanceof ModeGimbalCommand) {
                        c.config.finishDelayMillis = 1500.0;
                    }
                }
            }

            if (command instanceof DroneCommand) {
                droneCommands.addCommand(c);
            }
            else if (command instanceof RTKCommand) {
                droneCommands.addCommand(c);
            }
            else if (command instanceof RemoteControllerCommand) {
                remoteControllerCommands.addCommand(((RemoteControllerCommand)command).channel, c);
            }
            else if (command instanceof CameraCommand) {
                cameraCommands.addCommand(((CameraCommand)command).channel, c);
            }
            //GimbalCommand
            else {
                gimbalCommands.addCommand(((GimbalCommand)command).channel, c);
            }
            return;
        }

        throw new CommandTypeUnhandledException();
    }

    @Override
    public void removeCommands() {
        droneCommands.removeAll();
        remoteControllerCommands.removeAll();
        cameraCommands.removeAll();
        gimbalCommands.removeAll();
    }

    @Override
    public DroneControlSession createControlSession(final Context context, final ExecutionEngine executionEngine, final Executor executor) throws UnsupportedExecutionEngineException, UnsupportedDroneDJIExecutionEngineException {
        switch (executionEngine) {
            case DRONELINK_KERNEL:
                return new DJI2VirtualStickSession(context, droneAdapter);

            case DJI:
                break;

            default:
                break;
        }

        throw new UnsupportedExecutionEngineException(executionEngine);
    }

    @Override
    public DatedValue<RemoteControllerStateAdapter> getRemoteControllerState(final int channel) {
        return droneAdapter.getRemoteControllerState(channel);
    }

    @Override
    public DatedValue<CameraStateAdapter> getCameraState(final int channel) {
        return droneAdapter.getCameraState(channel);
    }

    @Override
    public DatedValue<GimbalStateAdapter> getGimbalState(final int channel) {
        return droneAdapter.getGimbalState(channel);
    }

    @Override
    public DatedValue<BatteryStateAdapter> getBatteryState(final int index) {
        return droneAdapter.getBatteryState(index);
    }

    @Override
    public DatedValue<RTKStateAdapter> getRTKState() {
        return droneAdapter.getRTKState();
    }

    @Override
    public void resetPayloads() {
        resetPayloads(true, true);
    }

    @Override
    public void resetPayloads(final boolean gimbal, final boolean camera) {
        if (gimbal) {
            droneAdapter.sendResetGimbalCommands();
        }

        if (camera) {
            droneAdapter.sendResetCameraCommands();
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        djiListeners.cancelAll();
        droneAdapter.close();
        closed = true;
        Log.i(TAG, "Drone session closed: " + getModel());
    }
}