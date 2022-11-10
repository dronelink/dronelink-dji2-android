//  DJI2DroneAdapter.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 10/4/22.
//  Copyright © 2022 Dronelink. All rights reserved.
//
package com.dronelink.dji2.adapters;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dronelink.core.Convert;
import com.dronelink.core.DatedValue;
import com.dronelink.core.adapters.BatteryAdapter;
import com.dronelink.core.adapters.BatteryStateAdapter;
import com.dronelink.core.adapters.CameraAdapter;
import com.dronelink.core.adapters.CameraStateAdapter;
import com.dronelink.core.adapters.DroneAdapter;
import com.dronelink.core.adapters.DroneStateAdapter;
import com.dronelink.core.adapters.EnumElement;
import com.dronelink.core.adapters.GimbalAdapter;
import com.dronelink.core.adapters.GimbalStateAdapter;
import com.dronelink.core.adapters.RemoteControllerAdapter;
import com.dronelink.core.adapters.RemoteControllerStateAdapter;
import com.dronelink.core.command.Command;
import com.dronelink.core.command.CommandError;
import com.dronelink.core.kernel.command.drone.DroneCommand;
import com.dronelink.core.kernel.command.drone.LowBatteryWarningThresholdDroneCommand;
import com.dronelink.core.kernel.command.drone.MaxAltitudeDroneCommand;
import com.dronelink.core.kernel.command.drone.RemoteControllerSticksDroneCommand;
import com.dronelink.core.kernel.command.drone.ReturnHomeAltitudeDroneCommand;
import com.dronelink.core.kernel.command.drone.VelocityDroneCommand;
import com.dronelink.core.kernel.core.Orientation3;
import com.dronelink.core.kernel.core.Vector2;
import com.dronelink.core.kernel.core.enums.GimbalMode;
import com.dronelink.dji2.DJI2CameraFile;
import com.dronelink.dji2.DJI2ListenerGroup;
import com.dronelink.dji2.DronelinkDJI2;
import com.dronelink.dji2.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import dji.sdk.keyvalue.key.BatteryKey;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem;
import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode;
import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode;
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam;
import dji.sdk.keyvalue.value.flightcontroller.YawControlMode;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager;
import dji.v5.manager.aircraft.virtualstick.VirtualStickRange;
import dji.v5.manager.datacenter.media.MediaFile;
import dji.v5.manager.datacenter.media.MediaFileFilter;
import dji.v5.manager.datacenter.media.MediaFileListData;
import dji.v5.manager.datacenter.media.MediaFileListState;
import dji.v5.manager.datacenter.media.MediaFileListStateListener;
import dji.v5.manager.datacenter.media.MediaManager;
import dji.v5.manager.datacenter.media.PullMediaFileListParam;

public class DJI2DroneAdapter implements DroneAdapter, MediaFileListStateListener {
    public interface CameraFileGeneratedCallback {
        void onCameraFileGenerated(final DJI2CameraFile file);
    }

    private static final String TAG = DJI2DroneAdapter.class.getCanonicalName();

    public String serialNumber;
    public String name;
    public ProductType productType;
    public String firmwarePackageVersion;
    public DJI2DroneStateAdapter state;

    private final Timer pullMediaFileTimer;
    private boolean pullingMediaFileListState = false;
    private MediaFileListState mediaFileListState;
    private final Map<Integer, DJI2CameraFile> pendingCameraFiles = new HashMap<>();

    private final DJI2ListenerGroup listeners = new DJI2ListenerGroup();
    private final DJI2RemoteControllerAdapter remoteController;
    private final Map<ComponentIndexType, CameraAdapter> cameras = new HashMap<>();
    private final Map<ComponentIndexType, GimbalAdapter> gimbals = new HashMap<>();
    private final Map<Integer, DJI2BatteryAdapter> batteries = new HashMap<>();

    public DJI2DroneAdapter(final Context context, final CommonCallbacks.CompletionCallbackWithParam<String> onSerialNumber, final CameraFileGeneratedCallback cameraFileReceiver) {
        state = new DJI2DroneStateAdapter(context);

        MediaManager.getInstance().addMediaFileListStateListener(this);

        pullMediaFileTimer = new Timer();
        pullMediaFileTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (pullingMediaFileListState || pendingCameraFiles.size() == 0) {
                    return;
                }

                pullingMediaFileListState = true;
                //disregarding mediaFileListState, seems unreliable and will get stuck in UPDATING, even though it isn't?
                final PullMediaFileListParam.Builder param = new PullMediaFileListParam.Builder();
                param.filter(MediaFileFilter.ALL);
                MediaManager.getInstance().pullMediaFileListFromCamera(param.build(), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Pulled media file list!");

                        new Handler().post(() -> {
                            synchronized (pendingCameraFiles) {
                                if (pendingCameraFiles.size() > 0) {
                                    final MediaFileListData mediaFileListData = MediaManager.getInstance().getMediaFileListData();
                                    if (mediaFileListData != null) {
                                        final List<MediaFile> mediaFiles = mediaFileListData.getData();
                                        if (mediaFiles != null && mediaFiles.size() > 0) {
                                            for (final Object pendingCameraFile : pendingCameraFiles.values().toArray()) {
                                                final DJI2CameraFile cameraFile = (DJI2CameraFile)pendingCameraFile;
                                                for (final MediaFile mediaFile : mediaFiles) {
                                                    if (mediaFile.getFileIndex() == cameraFile.generatedMediaFileInfo.getIndex()) {
                                                        cameraFile.mediaFile = mediaFile;
                                                        cameraFileReceiver.onCameraFileGenerated(cameraFile);
                                                        Log.d(TAG, String.format("Camera[%d] file generated: %s, %s",
                                                                cameraFile.getChannel(),
                                                                cameraFile.getName(),
                                                                Convert.HumanReadableByteCount(cameraFile.getSize(), true)));
                                                        pendingCameraFiles.remove(mediaFile.getFileIndex());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        });

                        pullingMediaFileListState = false;
                    }

                    @Override
                    public void onFailure(final @NonNull IDJIError error) {
                        Log.e(TAG, "Unable to pull media file list: " + (error.errorCode()));
                        pullingMediaFileListState = false;
                    }
                });
            }
        }, 0, 1000);

        listeners.init(KeyTools.createKey(FlightControllerKey.KeySerialNumber), (oldValue, newValue) -> {
            if (newValue != null) {
                serialNumber = newValue;
                Log.i(TAG, "Serial number: " + serialNumber);
                onSerialNumber.onSuccess(newValue);
            }
        });

        listeners.init(KeyTools.createKey(FlightControllerKey.KeyFirmwareVersion), (oldValue, newValue) -> {
            if (newValue != null) {
                firmwarePackageVersion = newValue;
                Log.i(TAG, "Firmware package version: " + firmwarePackageVersion);
            }
        });

        listeners.init(KeyTools.createKey(ProductKey.KeyProductType), (oldValue, newValue) -> {
            if (newValue != null) {
                productType = newValue;
                Log.i(TAG, "Product type: " + productType.name());
            }
        });

        listeners.init(KeyTools.createKey(FlightControllerKey.KeyAircraftName), (oldValue, newValue) -> {
            if (newValue != null) {
                name = newValue;
                Log.i(TAG, "Firmware package version: " + name);
            }
        });

        remoteController = new DJI2RemoteControllerAdapter();

        for (final ComponentIndexType index : ComponentIndexType.values()) {
            if (index != ComponentIndexType.UNKNOWN && index != ComponentIndexType.AGGREGATION) {
                listeners.init(KeyTools.createKey(GimbalKey.KeyConnection, index), (oldValue, newValue) -> {
                    synchronized (gimbals) {
                        if (newValue == null || !newValue) {
                            final DJI2GimbalAdapter gimbal = (DJI2GimbalAdapter) gimbals.remove(index);
                            if (gimbal != null) {
                                Log.i(TAG, "Gimbal disconnected: " + index.name());
                                gimbal.close();
                            }
                        } else {
                            Log.i(TAG, "Gimbal connected: " + index.name());
                            gimbals.put(index, new DJI2GimbalAdapter(index));
                        }
                    }
                });

                listeners.init(KeyTools.createKey(CameraKey.KeyConnection, index), (oldValue, newValue) -> {
                    synchronized (cameras) {
                        if (newValue == null || !newValue) {
                            final DJI2CameraAdapter camera = (DJI2CameraAdapter) cameras.remove(index);
                            if (camera != null) {
                                Log.i(TAG, "Camera disconnected: " + index.name());
                                camera.close();
                            }
                        } else {
                            Log.i(TAG, "Camera connected: " + index.name());
                            cameras.put(index, new DJI2CameraAdapter(context, this, index, info -> {
                                final Orientation3 orientation = state.getOrientation();
                                final DatedValue<GimbalStateAdapter> gimbalState = getGimbalState(index.value());
                                if (gimbalState != null) {
                                    orientation.x = gimbalState.value.getOrientation().x;
                                    orientation.y = gimbalState.value.getOrientation().y;
                                    if (gimbalState.value.getMode() == GimbalMode.FREE) {
                                        orientation.z = gimbalState.value.getOrientation().z;
                                    }
                                } else {
                                    orientation.x = 0.0;
                                    orientation.y = 0.0;
                                }

                                new Handler().post(() -> {
                                    synchronized (pendingCameraFiles) {
                                        pendingCameraFiles.put(info.getIndex(), new DJI2CameraFile(index.value(), info, state.getLocation(), state.getAltitude(), orientation));
                                    }
                                });
                            }));
                        }
                    }
                });
            }
        }

        //TODO doesn't work? listeners.init(KeyTools.createKey(BatteryKey.KeyNumberOfConnectedBatteries), (oldValue, newValue) -> {
//            synchronized (batteries) {
//                for (final DJI2BatteryAdapter battery : batteries) {
//                    battery.close();
//                }
//                batteries.clear();
//
//                if (newValue != null) {
//                    for (int i = 0; i < newValue; i++) {
//                        batteries.add(new DJI2BatteryAdapter(i));
//                    }
//                }
//            }
        //});

        for (int batteryIndex = 0; batteryIndex < 2; batteryIndex++) {
            final int index = batteryIndex;
            listeners.init(KeyTools.createKey(BatteryKey.KeyConnection, index), (oldValue, newValue) -> {
                synchronized (batteries) {
                    if (newValue == null || !newValue) {
                        final DJI2BatteryAdapter battery = (DJI2BatteryAdapter) batteries.remove(index);
                        if (battery != null) {
                            Log.i(TAG, "Battery disconnected: " + index);
                            battery.close();
                        }
                    } else {
                        Log.i(TAG, "Battery connected: " + index);
                        batteries.put(index, new DJI2BatteryAdapter(index));
                    }
                }
            });
        }
    }

    public void close() {
        pullMediaFileTimer.cancel();
        MediaManager.getInstance().removeMediaFileListStateListener(this);
        listeners.cancelAll();
        state.close();
        remoteController.close();

        for (final CameraAdapter camera : getCameras()) {
            ((DJI2CameraAdapter)camera).close();
        }

        for (final GimbalAdapter camera : getGimbals()) {
            ((DJI2GimbalAdapter)camera).close();
        }

        for (final BatteryAdapter battery : getBatteries()) {
            ((DJI2BatteryAdapter)battery).close();
        }
    }

    public DatedValue<DroneStateAdapter> getState() {
        return state.asDatedValue();
    }

    @Override
    public void onUpdate(final MediaFileListState mediaFileListState) {
        this.mediaFileListState = mediaFileListState;
        //Log.d(TAG, "Media file list state changed: " + mediaFileListState.name());
    }

    @Override
    public Collection<RemoteControllerAdapter> getRemoteControllers() {
        final ArrayList<RemoteControllerAdapter> remoteControllers = new ArrayList<>();
        remoteControllers.add(remoteController);
        return remoteControllers;
    }

    @Override
    public Collection<CameraAdapter> getCameras() {
        synchronized (cameras) {
            return new ArrayList<>(cameras.values());
        }
    }

    @Override
    public Collection<GimbalAdapter> getGimbals() {
        synchronized (gimbals) {
            return new ArrayList<>(gimbals.values());
        }
    }

    @Override
    public Collection<BatteryAdapter> getBatteries() {
        synchronized (batteries) {
            return new ArrayList<>(batteries.values());
        }
    }

    @Override
    public RemoteControllerAdapter getRemoteController(final int channel) {
        return remoteController;
    }

    @Override
    public Integer getCameraChannel(final Integer videoFeedChannel) {
        //TODO use VideoFeedPhysicalSource
        return 0;
    }

    @Override
    public CameraAdapter getCamera(final int channel) {
        synchronized (cameras) {
            for (final CameraAdapter camera : cameras.values()) {
                if (camera.getIndex() == channel) {
                    return camera;
                }
            }
            return null;
        }
    }

    @Override
    public GimbalAdapter getGimbal(final int channel) {
        synchronized (gimbals) {
            for (final GimbalAdapter gimbal : gimbals.values()) {
                if (gimbal.getIndex() == channel) {
                    return gimbal;
                }
            }
            return null;
        }
    }

    @Override
    public BatteryAdapter getBattery(final int index) {
        synchronized (batteries) {
            for (final BatteryAdapter battery : batteries.values()) {
                if (battery.getIndex() == index) {
                    return battery;
                }
            }
            return null;
        }
    }

    @Override
    public void sendVelocityCommand(final @Nullable VelocityDroneCommand velocityCommand) {
        if (velocityCommand == null) {
            sendResetVelocityCommand();
            return;
        }

        final VirtualStickFlightControlParam param = new VirtualStickFlightControlParam();
        param.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        param.setRollPitchCoordinateSystem(FlightCoordinateSystem.GROUND);
        param.setVerticalControlMode(VerticalControlMode.VELOCITY);
        param.setYawControlMode(velocityCommand.heading == null ? YawControlMode.ANGULAR_VELOCITY : YawControlMode.ANGLE);
        final Vector2 horizontal = velocityCommand.velocity.getHorizontal();
        //TODO allow the web to plan missions up to 23 m/s?
        horizontal.magnitude = Math.min(VirtualStickRange.ROLL_PITCH_CONTROL_MAX_VELOCITY, horizontal.magnitude);
        param.setPitch(horizontal.getY());
        param.setRoll(horizontal.getX());
        param.setYaw(Math.toDegrees(velocityCommand.heading == null ? velocityCommand.velocity.getRotational() : Convert.AngleDifferenceSigned(velocityCommand.heading, 0)));
        param.setVerticalThrottle(velocityCommand.velocity.getVertical());
        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
        VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param);
    }

    @Override
    public void sendRemoteControllerSticksCommand(final @Nullable RemoteControllerSticksDroneCommand remoteControllerSticks) {
        if (remoteControllerSticks == null) {
            sendResetVelocityCommand();
            return;
        }

        final VirtualStickFlightControlParam param = new VirtualStickFlightControlParam();
        param.setRollPitchControlMode(RollPitchControlMode.ANGLE);
        param.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        param.setVerticalControlMode(VerticalControlMode.VELOCITY);
        param.setYawControlMode(remoteControllerSticks.heading == null ? YawControlMode.ANGULAR_VELOCITY : YawControlMode.ANGLE);
        param.setPitch((double) (-remoteControllerSticks.rightStick.y * 30));
        param.setRoll((double) (remoteControllerSticks.rightStick.x * 30));
        param.setYaw(remoteControllerSticks.heading == null ? (double) (remoteControllerSticks.leftStick.x * 100) : (double) Math.toDegrees(Convert.AngleDifferenceSigned(remoteControllerSticks.heading, 0)));
        param.setVerticalThrottle((double) (remoteControllerSticks.leftStick.y * 4.0));
        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
        VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param);
    }

    @Override
    public void startTakeoff(final Command.Finisher finisher) {
        KeyManager.getInstance().performAction(
                KeyTools.createKey(FlightControllerKey.KeyStartTakeoff),
                DronelinkDJI2.createCompletionCallbackWithParam(finisher));
    }

    @Override
    public void startLand(final Command.Finisher finisher) {
        KeyManager.getInstance().performAction(
                KeyTools.createKey(FlightControllerKey.KeyStartAutoLanding),
                DronelinkDJI2.createCompletionCallbackWithParam(finisher));
    }

    @Override
    public void stopLand(final Command.Finisher finisher) {
        KeyManager.getInstance().performAction(
                KeyTools.createKey(FlightControllerKey.KeyStopAutoLanding),
                DronelinkDJI2.createCompletionCallbackWithParam(finisher));
    }

    @Override
    public void startReturnHome(final Command.Finisher finisher) {
        KeyManager.getInstance().performAction(
                KeyTools.createKey(FlightControllerKey.KeyStartGoHome),
                DronelinkDJI2.createCompletionCallbackWithParam(finisher));
    }

    @Override
    public void stopReturnHome(final Command.Finisher finisher) {
        KeyManager.getInstance().performAction(
                KeyTools.createKey(FlightControllerKey.KeyStopGoHome),
                DronelinkDJI2.createCompletionCallbackWithParam(finisher));
    }

    @Override
    public void startCompassCalibration(final Command.Finisher finisher) {
        KeyManager.getInstance().performAction(
                KeyTools.createKey(FlightControllerKey.KeyStartCompassCalibration),
                DronelinkDJI2.createCompletionCallbackWithParam(finisher));
    }

    @Override
    public void stopCompassCalibration(final Command.Finisher finisher) {
        KeyManager.getInstance().performAction(
                KeyTools.createKey(FlightControllerKey.KeyStopCompassCalibration),
                DronelinkDJI2.createCompletionCallbackWithParam(finisher));
    }

    @Override
    public List<EnumElement> getEnumElements(final String parameter) {
        return null;
    }

    public DatedValue<RemoteControllerStateAdapter> getRemoteControllerState(final int channel) {
        return ((DJI2RemoteControllerAdapter)getRemoteController(0)).getState();
    }

    public DatedValue<CameraStateAdapter> getCameraState(final int channel) {
        final DJI2CameraAdapter camera = (DJI2CameraAdapter)getCamera(channel);
        if (camera != null) {
            return camera.getState();
        }
        return null;
    }

    public DatedValue<GimbalStateAdapter> getGimbalState(final int channel) {
        final DJI2GimbalAdapter gimbal = (DJI2GimbalAdapter)getGimbal(channel);
        if (gimbal != null) {
            return gimbal.getState();
        }
        return null;
    }

    public DatedValue<BatteryStateAdapter> getBatteryState(final int index) {
        final DJI2BatteryAdapter battery = (DJI2BatteryAdapter) getBattery(index);
        if (battery != null) {
            return battery.getState();
        }
        return null;
    }

    public void sendResetVelocityCommand() {
        final VirtualStickFlightControlParam param = new VirtualStickFlightControlParam();
        param.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        param.setRollPitchCoordinateSystem(FlightCoordinateSystem.GROUND);
        param.setVerticalControlMode(VerticalControlMode.VELOCITY);
        param.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
        param.setPitch(0.0);
        param.setRoll(0.0);
        param.setYaw(0.0);
        param.setVerticalThrottle(0.0);
        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
        VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param);
    }

    public void sendResetGimbalCommands() {
        synchronized (gimbals) {
            for (final GimbalAdapter gimbal : gimbals.values()) {
                ((DJI2GimbalAdapter) gimbal).sendResetCommands();
            }
        }
    }

    public void sendResetCameraCommands() {
        synchronized (cameras) {
            for (final CameraAdapter camera : cameras.values()) {
                ((DJI2CameraAdapter) camera).sendResetCommands();
            }
        }
    }

    public CommandError executeCommand(final Context context, final DroneCommand command, final Command.Finisher finished) {
        if (command instanceof LowBatteryWarningThresholdDroneCommand) {
            final int target = (int)(((LowBatteryWarningThresholdDroneCommand) command).lowBatteryWarningThreshold * 100);
            Command.conditionallyExecute(target != state.lowBatteryThreshold, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightControllerKey.KeyLowBatteryWarningThreshold), target, DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof MaxAltitudeDroneCommand) {
            final int target = (int)((MaxAltitudeDroneCommand) command).maxAltitude;
            Command.conditionallyExecute(target != state.maxAltitude, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightControllerKey.KeyHeightLimit), target, DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof ReturnHomeAltitudeDroneCommand) {
            final int target = (int)((ReturnHomeAltitudeDroneCommand) command).returnHomeAltitude;
            Command.conditionallyExecute(target != state.returnHomeAltitude, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightControllerKey.KeyGoHomeHeight), target, DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        return new CommandError(context.getString(R.string.MissionDisengageReason_command_type_unhandled) + ": " + command.type);
    }
}
