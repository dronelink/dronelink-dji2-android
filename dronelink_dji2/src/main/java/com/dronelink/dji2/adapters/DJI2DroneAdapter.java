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
import com.dronelink.core.adapters.LiveStreamingAdapter;
import com.dronelink.core.adapters.LiveStreamingStateAdapter;
import com.dronelink.core.adapters.RTKAdapter;
import com.dronelink.core.adapters.RTKStateAdapter;
import com.dronelink.core.adapters.RemoteControllerAdapter;
import com.dronelink.core.adapters.RemoteControllerStateAdapter;
import com.dronelink.core.command.Command;
import com.dronelink.core.command.CommandError;
import com.dronelink.core.kernel.command.drone.AccessoryDroneCommand;
import com.dronelink.core.kernel.command.drone.AuxiliaryLightModeDroneCommand;
import com.dronelink.core.kernel.command.drone.BeaconDroneCommand;
import com.dronelink.core.kernel.command.drone.CollisionAvoidanceDroneCommand;
import com.dronelink.core.kernel.command.drone.ConnectionFailSafeBehaviorDroneCommand;
import com.dronelink.core.kernel.command.drone.DownwardAvoidanceDroneCommand;
import com.dronelink.core.kernel.command.drone.DroneCommand;
import com.dronelink.core.kernel.command.drone.FlightAssistantDroneCommand;
import com.dronelink.core.kernel.command.drone.HomeLocationDroneCommand;
import com.dronelink.core.kernel.command.drone.LandingProtectionDroneCommand;
import com.dronelink.core.kernel.command.drone.LowBatteryWarningThresholdDroneCommand;
import com.dronelink.core.kernel.command.drone.MaxAltitudeDroneCommand;
import com.dronelink.core.kernel.command.drone.MaxDistanceDroneCommand;
import com.dronelink.core.kernel.command.drone.MaxDistanceLimitationDroneCommand;
import com.dronelink.core.kernel.command.drone.ObstacleAvoidanceBrakingDistanceDroneCommand;
import com.dronelink.core.kernel.command.drone.ObstacleAvoidanceModeDroneCommand;
import com.dronelink.core.kernel.command.drone.ObstacleAvoidanceWarningDistanceDroneCommand;
import com.dronelink.core.kernel.command.drone.OcuSyncChannelDroneCommand;
import com.dronelink.core.kernel.command.drone.OcuSyncChannelSelectionModeDroneCommand;
import com.dronelink.core.kernel.command.drone.OcuSyncDroneCommand;
import com.dronelink.core.kernel.command.drone.OcuSyncFrequencyBandDroneCommand;
import com.dronelink.core.kernel.command.drone.OcuSyncVideoFeedSourcesDroneCommand;
import com.dronelink.core.kernel.command.drone.PrecisionLandingDroneCommand;
import com.dronelink.core.kernel.command.drone.RemoteControllerSticksDroneCommand;
import com.dronelink.core.kernel.command.drone.ReturnHomeAltitudeDroneCommand;
import com.dronelink.core.kernel.command.drone.ReturnHomeObstacleAvoidanceDroneCommand;
import com.dronelink.core.kernel.command.drone.ReturnHomeRemoteObstacleAvoidanceDroneCommand;
import com.dronelink.core.kernel.command.drone.SeriousLowBatteryWarningThresholdDroneCommand;
import com.dronelink.core.kernel.command.drone.SmartReturnHomeDroneCommand;
import com.dronelink.core.kernel.command.drone.SpotlightBrightnessDroneCommand;
import com.dronelink.core.kernel.command.drone.SpotlightDroneCommand;
import com.dronelink.core.kernel.command.drone.UpwardsAvoidanceDroneCommand;
import com.dronelink.core.kernel.command.drone.VelocityDroneCommand;
import com.dronelink.core.kernel.command.drone.VisionAssistedPositioningDroneCommand;
import com.dronelink.core.kernel.core.DroneObstacleAvoidanceSpecification;
import com.dronelink.core.kernel.core.GeoCoordinate;
import com.dronelink.core.kernel.core.Message;
import com.dronelink.core.kernel.core.Orientation3;
import com.dronelink.core.kernel.core.Vector2;
import com.dronelink.core.kernel.core.enums.DroneObstacleAvoidanceDirection;
import com.dronelink.core.kernel.core.enums.DroneObstacleAvoidanceMode;
import com.dronelink.core.kernel.core.enums.DroneOcuSyncFrequencyBand;
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

import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.BatteryKey;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.FlightAssistantKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.value.airlink.ChannelSelectionMode;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.flightassistant.AuxiliaryLightMode;
import dji.sdk.keyvalue.value.flightcontroller.FailsafeAction;
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem;
import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode;
import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode;
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam;
import dji.sdk.keyvalue.value.flightcontroller.YawControlMode;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.perception.data.PerceptionDirection;
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager;
import dji.v5.manager.aircraft.virtualstick.VirtualStickRange;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.datacenter.livestream.LiveStreamSettings;
import dji.v5.manager.datacenter.livestream.LiveStreamType;
import dji.v5.manager.datacenter.livestream.settings.RtmpSettings;

public class DJI2DroneAdapter implements DroneAdapter {
    public interface CameraFileGeneratedCallback {
        void onCameraFileGenerated(final DJI2CameraFile file);
    }

    private static final String TAG = DJI2DroneAdapter.class.getCanonicalName();

    public String serialNumber;
    public String name;
    public ProductType productType;
    public String firmwarePackageVersion;
    public DJI2DroneStateAdapter state;

    private final DJI2ListenerGroup listeners = new DJI2ListenerGroup();
    private final DJI2RemoteControllerAdapter remoteController;
    private final Map<ComponentIndexType, CameraAdapter> cameras = new HashMap<>();
    private final Map<ComponentIndexType, GimbalAdapter> gimbals = new HashMap<>();
    private final Map<Integer, DJI2BatteryAdapter> batteries = new HashMap<>();
    private final DJI2RTKAdapter rtk;
    private final DJI2LiveStreamingAdapter liveStreaming;

    public DJI2DroneAdapter(final Context context, final CommonCallbacks.CompletionCallbackWithParam<String> onSerialNumber, final CameraFileGeneratedCallback cameraFileReceiver) {
        state = new DJI2DroneStateAdapter(context, this);

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

                                final DJI2CameraFile cameraFile = new DJI2CameraFile(index.value(), info, state.getLocation(), state.getAltitude(), orientation);
                                new Handler().post(() -> {
                                    cameraFileReceiver.onCameraFileGenerated(cameraFile);
                                    Log.d(TAG, String.format("Camera[%d] file generated: %s, %s",
                                            cameraFile.getChannel(),
                                            cameraFile.getName(),
                                            Convert.HumanReadableByteCount(cameraFile.getSize(), true)));
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
                        final DJI2BatteryAdapter battery = batteries.remove(index);
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

        rtk = new DJI2RTKAdapter(context, this);
        liveStreaming = new DJI2LiveStreamingAdapter(context);
    }

    public void close() {
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

    public List<Message> getStatusMessages() {
        final List<Message> messages = new ArrayList<>();

        for (final CameraAdapter camera : getCameras()) {
            messages.addAll(((DJI2CameraAdapter)camera).getStatusMessages());
        }

        final DatedValue<RTKStateAdapter> rtkState = getRTKState();
        if (rtkState != null && rtkState.value != null && rtkState.value.isEnabled()) {
            for (final Message message : rtkState.value.getStatusMessages()) {
                if (message.level != Message.Level.INFO) {
                    messages.add(message);
                }
            }
        }

        final DatedValue<LiveStreamingStateAdapter> liveStreamingState = getLiveStreamingState();
        if (liveStreamingState != null && liveStreamingState.value != null && liveStreamingState.value.isEnabled()) {
            for (final Message message : liveStreamingState.value.getStatusMessages()) {
                if (message.level != Message.Level.INFO) {
                    messages.add(message);
                }
            }
        }

        return messages;
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
    public RTKAdapter getRTK() {
        return rtk;
    }

    @Override
    public LiveStreamingAdapter getLiveStreaming() {
        return liveStreaming;
    }

    private boolean isYawControlModeAngleAvailable() {
        return false;
    }

    private void setVirtualStickFlightControlParamYaw(final VirtualStickFlightControlParam param, final double heading) {
        if (isYawControlModeAngleAvailable()) {
            param.setYawControlMode(YawControlMode.ANGLE);
            param.setYaw(Math.toDegrees(Convert.AngleDifferenceSigned(heading, 0)));
        }
        else {
            param.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            param.setYaw(Math.toDegrees(Convert.AngleDifferenceSigned(heading, state.getOrientation().getYaw()) * 1.5));
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
        final Vector2 horizontal = velocityCommand.velocity.getHorizontal();
        //TODO allow the web to plan missions up to 23 m/s?
        horizontal.magnitude = Math.min(VirtualStickRange.ROLL_PITCH_CONTROL_MAX_VELOCITY, horizontal.magnitude);
        param.setPitch(horizontal.getY());
        param.setRoll(horizontal.getX());
        param.setVerticalThrottle(Math.min(
                VirtualStickRange.VERTICAL_CONTROL_MAX_VELOCITY,
                Math.max(VirtualStickRange.VERTICAL_CONTROL_MIN_VELOCITY,
                        velocityCommand.velocity.getVertical())));
        final Double heading = velocityCommand.heading;
        if (heading == null) {
            param.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            param.setYaw(Math.toDegrees(velocityCommand.velocity.getRotational()));
        }
        else {
            setVirtualStickFlightControlParamYaw(param, heading);
        }
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
        param.setPitch(-remoteControllerSticks.rightStick.y * 30);
        param.setRoll(remoteControllerSticks.rightStick.x * 30);
        param.setVerticalThrottle(remoteControllerSticks.leftStick.y * 4.0);
        final Double heading = remoteControllerSticks.heading;
        if (heading == null) {
            param.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            param.setYaw(remoteControllerSticks.leftStick.x * 100);
        }
        else {
            setVirtualStickFlightControlParamYaw(param, heading);
        }
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

    public DatedValue<RTKStateAdapter> getRTKState() {
        final DJI2RTKAdapter rtk = (DJI2RTKAdapter)getRTK();
        if (rtk != null) {
            return rtk.getState();
        }
        return null;
    }

    public DatedValue<LiveStreamingStateAdapter> getLiveStreamingState() {
        final DJI2LiveStreamingAdapter liveStreaming = (DJI2LiveStreamingAdapter)getLiveStreaming();
        if (liveStreaming != null) {
            return liveStreaming.getState();
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
        if (command instanceof FlightAssistantDroneCommand) {
            return executeFlightAssistantDroneCommand(context, (FlightAssistantDroneCommand) command, finished);
        }

        if (command instanceof OcuSyncDroneCommand) {
            return executeOcuSyncDroneCommand(context, (OcuSyncDroneCommand) command, finished);
        }

        if (command instanceof AccessoryDroneCommand) {
            return executeAccessoryDroneCommand(context, (AccessoryDroneCommand) command, finished);
        }

        if (command instanceof ConnectionFailSafeBehaviorDroneCommand) {
            final FailsafeAction target = DronelinkDJI2.getDroneConnectionFailSafeBehavior(((ConnectionFailSafeBehaviorDroneCommand) command).connectionFailSafeBehavior);
            Command.conditionallyExecute(target != state.failSafeAction, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightControllerKey.KeyFailsafeAction),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof HomeLocationDroneCommand) {
            final GeoCoordinate coordinate = ((HomeLocationDroneCommand) command).coordinate;
            KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyHomeLocation), DronelinkDJI2.getCoordinate(coordinate), DronelinkDJI2.createCompletionCallback(finished));
            return null;
        }

        if (command instanceof LowBatteryWarningThresholdDroneCommand) {
            final int target = (int)(((LowBatteryWarningThresholdDroneCommand) command).lowBatteryWarningThreshold * 100);
            Command.conditionallyExecute(target != state.lowBatteryThreshold, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightControllerKey.KeyLowBatteryWarningThreshold), target, DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof MaxAltitudeDroneCommand) {
            final int target = (int)((MaxAltitudeDroneCommand) command).maxAltitude;
            Command.conditionallyExecute(state.maxAltitude == null || target != state.maxAltitude, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightControllerKey.KeyHeightLimit), target, DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof MaxDistanceDroneCommand) {
            final int target = (int)((MaxDistanceDroneCommand) command).maxDistance;
            Command.conditionallyExecute(state.maxDistance == null || target != state.maxDistance, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightControllerKey.KeyDistanceLimit), target, DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof MaxDistanceLimitationDroneCommand) {
            final boolean target = ((MaxDistanceLimitationDroneCommand) command).enabled;
            Command.conditionallyExecute(target != state.distanceLimitEnabled, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightControllerKey.KeyDistanceLimitEnabled),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof ReturnHomeAltitudeDroneCommand) {
            final int target = (int)((ReturnHomeAltitudeDroneCommand) command).returnHomeAltitude;
            Command.conditionallyExecute(target != state.returnHomeAltitude, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightControllerKey.KeyGoHomeHeight), target, DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof SeriousLowBatteryWarningThresholdDroneCommand) {
            final int target = (int)(((SeriousLowBatteryWarningThresholdDroneCommand) command).seriousLowBatteryWarningThreshold * 100);
            Command.conditionallyExecute(target != state.seriousLowBatteryThreshold, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightControllerKey.KeySeriousLowBatteryWarningThreshold), target, DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof SmartReturnHomeDroneCommand) {
            //TODO
        }

        final LiveStreamSettings.Builder liveStreamSettings = new LiveStreamSettings.Builder();
        liveStreamSettings.setLiveStreamType(LiveStreamType.RTMP);
        final RtmpSettings.Builder rtmpSettings = new RtmpSettings.Builder();
        rtmpSettings.setUrl("");
        liveStreamSettings.setRtmpSettings(rtmpSettings.build());
        MediaDataCenter.getInstance().getLiveStreamManager().setLiveStreamSettings(liveStreamSettings.build());
//        MediaDataCenter.getInstance().getLiveStreamManager().setLiveStreamQuality(StreamQuality.FULL_HD);
//        MediaDataCenter.getInstance().getLiveStreamManager().setLiveVideoBitrateMode(LiveVideoBitrateMode.AUTO);
//        MediaDataCenter.getInstance().getLiveStreamManager().setLiveVideoBitrate(0);

        return new CommandError(context.getString(R.string.MissionDisengageReason_command_type_unhandled) + ": " + command.type);
    }

    private CommandError executeFlightAssistantDroneCommand(final Context context, final FlightAssistantDroneCommand command, final Command.Finisher finished) {
        if (command instanceof AuxiliaryLightModeDroneCommand) {
            final AuxiliaryLightMode target = DronelinkDJI2.getDroneAuxiliaryLightMode(((AuxiliaryLightModeDroneCommand) command).auxiliaryLightMode);
            switch (((AuxiliaryLightModeDroneCommand) command).auxiliaryLightPosition) {
                case BOTTOM:
                    KeyManager.getInstance().setValue(KeyTools.createKey(FlightAssistantKey.KeyBottomAuxiliaryLightMode), target, DronelinkDJI2.createCompletionCallback(finished));
                    return null;
                case TOP:
                    KeyManager.getInstance().setValue(KeyTools.createKey(FlightAssistantKey.KeyTopAuxiliaryLightMode), target, DronelinkDJI2.createCompletionCallback(finished));
                    return null;
                case UNKNOWN:
                    break;
            }
        }

        if (command instanceof CollisionAvoidanceDroneCommand) {
          final Boolean target = ((CollisionAvoidanceDroneCommand) command).enabled;
          final DroneObstacleAvoidanceSpecification spec = state.getObstacleAvoidanceSpecification();
          if (spec != null) {
              Command.conditionallyExecute(!target.equals(spec.avoidanceEnabled.get(DroneObstacleAvoidanceDirection.HORIZONTAL)), finished,
                      () -> state.perceptionManager.setObstacleAvoidanceEnabled(target, PerceptionDirection.HORIZONTAL, DronelinkDJI2.createCompletionCallback(finished)));
          }
          return null;
        }

        if (command instanceof UpwardsAvoidanceDroneCommand) {
            final Boolean target = ((UpwardsAvoidanceDroneCommand) command).enabled;
            final DroneObstacleAvoidanceSpecification spec = state.getObstacleAvoidanceSpecification();
            if (spec != null) {
                Command.conditionallyExecute(!target.equals(spec.avoidanceEnabled.get(DroneObstacleAvoidanceDirection.UPWARD)), finished,
                        () -> state.perceptionManager.setObstacleAvoidanceEnabled(target, PerceptionDirection.UPWARD, DronelinkDJI2.createCompletionCallback(finished)));
            }
            return null;
        }

        if (command instanceof DownwardAvoidanceDroneCommand) {
            final Boolean target = ((DownwardAvoidanceDroneCommand) command).enabled;
            final DroneObstacleAvoidanceSpecification spec = state.getObstacleAvoidanceSpecification();
            if (spec != null) {
                Command.conditionallyExecute(!target.equals(spec.avoidanceEnabled.get(DroneObstacleAvoidanceDirection.DOWNWARD)), finished,
                        () -> state.perceptionManager.setObstacleAvoidanceEnabled(target, PerceptionDirection.DOWNWARD, DronelinkDJI2.createCompletionCallback(finished)));
            }
            return null;
        }

        if (command instanceof ObstacleAvoidanceModeDroneCommand) {
            final DroneObstacleAvoidanceMode target = ((ObstacleAvoidanceModeDroneCommand) command).obstacleAvoidanceMode;
            final DroneObstacleAvoidanceSpecification spec = state.getObstacleAvoidanceSpecification();
            if (spec != null) {
                Command.conditionallyExecute(target != spec.mode, finished,
                        () -> state.perceptionManager.setObstacleAvoidanceType(DronelinkDJI2.getDroneObstacleAvoidanceMode(target), DronelinkDJI2.createCompletionCallback(finished)));
            }
            return null;
        }

        if (command instanceof ObstacleAvoidanceBrakingDistanceDroneCommand) {
            final ObstacleAvoidanceBrakingDistanceDroneCommand commandLocal = (ObstacleAvoidanceBrakingDistanceDroneCommand) command;
            final double target = commandLocal.brakingDistance;
            final DroneObstacleAvoidanceDirection targetDirection = commandLocal.direction;
            final DroneObstacleAvoidanceSpecification spec = state.getObstacleAvoidanceSpecification();
            if (spec != null) {
                final Map<DroneObstacleAvoidanceDirection, Double> brakingDistances = spec.brakingDistances;
                if (brakingDistances != null) {
                    final Double brakingDistance = brakingDistances.get(targetDirection);
                    if (brakingDistance != null) {
                        Command.conditionallyExecute(Math.abs(target - brakingDistance) >= 0.1, finished,
                                () -> state.perceptionManager.setObstacleAvoidanceBrakingDistance(target, DronelinkDJI2.getDroneObstacleAvoidanceDirection(targetDirection),
                                DronelinkDJI2.createCompletionCallback(finished)));
                    }
                }
            }
            return null;
        }

        if (command instanceof ObstacleAvoidanceWarningDistanceDroneCommand) {
            final ObstacleAvoidanceWarningDistanceDroneCommand commandLocal = (ObstacleAvoidanceWarningDistanceDroneCommand) command;
            final double target = commandLocal.warningDistance;
            final DroneObstacleAvoidanceDirection targetDirection = commandLocal.direction;
            final DroneObstacleAvoidanceSpecification spec = state.getObstacleAvoidanceSpecification();
            if (spec != null) {
                final Map<DroneObstacleAvoidanceDirection, Double> warningDistances = spec.warningDistances;
                if (warningDistances != null) {
                    final Double warningDistance = warningDistances.get(targetDirection);
                    if (warningDistance != null) {
                        Command.conditionallyExecute(Math.abs(target - warningDistance) >= 0.1, finished,
                                () -> state.perceptionManager.setObstacleAvoidanceWarningDistance(target, DronelinkDJI2.getDroneObstacleAvoidanceDirection(targetDirection),
                                DronelinkDJI2.createCompletionCallback(finished)));
                    }
                }
            }
            return null;
        }

        if (command instanceof LandingProtectionDroneCommand) {
            final boolean target = ((LandingProtectionDroneCommand) command).enabled;
            Command.conditionallyExecute(target != state.landingProtectionEnabled, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightAssistantKey.KeyLandingProtectionEnabled),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof PrecisionLandingDroneCommand) {
            final boolean target = ((PrecisionLandingDroneCommand) command).enabled;
            Command.conditionallyExecute(target != state.precisionLandingEnabled, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightAssistantKey.KeyPrecisionLandingEnabled),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof ReturnHomeObstacleAvoidanceDroneCommand) {
            final boolean target = ((ReturnHomeObstacleAvoidanceDroneCommand) command).enabled;
            final DroneObstacleAvoidanceSpecification spec = state.getObstacleAvoidanceSpecification();
            if (spec != null) {
                Command.conditionallyExecute(target != spec.returnHomeObstacleAvoidanceEnabled, finished, () -> KeyManager.getInstance().setValue(
                        KeyTools.createKey(FlightAssistantKey.KeyRTHObstacleAvoidanceEnabled),
                        target,
                        DronelinkDJI2.createCompletionCallback(finished)));
            }
            return null;
        }

        if (command instanceof ReturnHomeRemoteObstacleAvoidanceDroneCommand) {
            //TODO
        }

        if (command instanceof VisionAssistedPositioningDroneCommand) {
            final boolean target = ((VisionAssistedPositioningDroneCommand) command).enabled;
            Command.conditionallyExecute(target != state.visionPositioningEnabled, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(FlightAssistantKey.KeyVisionPositioningEnabled),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        return new CommandError(context.getString(R.string.MissionDisengageReason_command_type_unhandled) + ": " + command.type);
    }

    private CommandError executeOcuSyncDroneCommand(final Context context, final OcuSyncDroneCommand command, final Command.Finisher finished) {
        if (command instanceof OcuSyncChannelDroneCommand) {
            //TODO
//            final int target = ((OcuSyncChannelDroneCommand) command).ocuSyncChannel;
//            Command.conditionallyExecute(state.ocuSyncChannel == null || target != state.ocuSyncChannel, finished, () -> KeyManager.getInstance().setValue(
//                    KeyTools.createKey(AirLinkKey.KeyChannelNumber),
//                    target,
//                    DronelinkDJI2.createCompletionCallback(finished)));
//            return null;
        }

        if (command instanceof OcuSyncChannelSelectionModeDroneCommand) {
            final ChannelSelectionMode target = DronelinkDJI2.getOcuSyncChannelSelectionMode(((OcuSyncChannelSelectionModeDroneCommand) command).ocuSyncChannelSelectionMode);
            Command.conditionallyExecute(target != state.ocuSyncChannelSelectionMode, finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(AirLinkKey.KeyChannelSelectionMode),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof OcuSyncFrequencyBandDroneCommand) {
            final DroneOcuSyncFrequencyBand target = ((OcuSyncFrequencyBandDroneCommand) command).ocuSyncFrequencyBand;
            Command.conditionallyExecute(target != state.getOcuSyncFrequencyBand(), finished, () -> KeyManager.getInstance().setValue(
                    KeyTools.createKey(AirLinkKey.KeyFrequencyBand),
                    DronelinkDJI2.getOcuSyncFrequencyBand(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof OcuSyncVideoFeedSourcesDroneCommand) {
            //TODO
        }

        return new CommandError(context.getString(R.string.MissionDisengageReason_command_type_unhandled) + ": " + command.type);
    }

    private CommandError executeAccessoryDroneCommand(final Context context, final AccessoryDroneCommand command, final Command.Finisher finished) {
        if (command instanceof BeaconDroneCommand) {
            //TODO
        }

        if (command instanceof SpotlightDroneCommand || command instanceof SpotlightBrightnessDroneCommand) {
            //TODO
        }

        return new CommandError(context.getString(R.string.MissionDisengageReason_command_type_unhandled) + ": " + command.type);
    }
}
