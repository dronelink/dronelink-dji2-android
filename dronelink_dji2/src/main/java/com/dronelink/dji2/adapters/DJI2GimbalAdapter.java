//  DJI2GimbalAdapter.java
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

import com.dronelink.core.Convert;
import com.dronelink.core.DatedValue;
import com.dronelink.core.Kernel;
import com.dronelink.core.adapters.EnumElement;
import com.dronelink.core.adapters.EnumElementsCollection;
import com.dronelink.core.adapters.GimbalAdapter;
import com.dronelink.core.adapters.GimbalStateAdapter;
import com.dronelink.core.command.Command;
import com.dronelink.core.command.CommandError;
import com.dronelink.core.kernel.command.gimbal.GimbalCommand;
import com.dronelink.core.kernel.command.gimbal.ModeGimbalCommand;
import com.dronelink.core.kernel.command.gimbal.OrientationGimbalCommand;
import com.dronelink.core.kernel.command.gimbal.VelocityGimbalCommand;
import com.dronelink.core.kernel.core.Orientation3Optional;
import com.dronelink.core.kernel.core.enums.GimbalMode;
import com.dronelink.dji2.DJI2ListenerGroup;
import com.dronelink.dji2.DronelinkDJI2;
import com.dronelink.dji2.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dji.sdk.keyvalue.key.DJIActionKeyInfo;
import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.DJIKeyInfo;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation;
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotationMode;
import dji.sdk.keyvalue.value.gimbal.GimbalAttitudeRange;
import dji.sdk.keyvalue.value.gimbal.GimbalResetType;
import dji.sdk.keyvalue.value.gimbal.GimbalSpeedRotation;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;

public class DJI2GimbalAdapter implements GimbalAdapter {
    private static final String TAG = DJI2GimbalAdapter.class.getCanonicalName();

    private final ExecutorService serialQueue = Executors.newSingleThreadExecutor();

    private final DJI2ListenerGroup listeners = new DJI2ListenerGroup();

    private final ComponentIndexType index;

    private final EnumElementsCollection enumElements = new EnumElementsCollection();
    private final DJI2GimbalStateAdapter state;
    private GimbalAttitudeRange attitudeRange;
    private boolean isYawAdjustSupported = false;
    private GimbalSpeedRotation pendingSpeedRotation;

    public DJI2GimbalAdapter(final ComponentIndexType index) {
        this.index = index;
        this.state = new DJI2GimbalStateAdapter(index);

        listeners.init(createKey(GimbalKey.KeyGimbalAttitudeRange), (oldValue, newValue) -> attitudeRange = newValue);
        listeners.init(createKey(GimbalKey.KeyYawAdjustSupported), (oldValue, newValue) -> {
            isYawAdjustSupported = newValue != null && newValue;

            final List<String> range = new ArrayList<>();
            range.add(Kernel.enumRawValue(GimbalMode.YAW_FOLLOW));
            if (isAdjustYaw360Supported()) {
                range.add(Kernel.enumRawValue(GimbalMode.FREE));
            }
            range.add(Kernel.enumRawValue(GimbalMode.FPV));
            enumElements.update("GimbalMode", range);
        });

        KeyManager.getInstance().setValue(createKey(GimbalKey.KeyPitchRangeExtensionEnabled), true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, String.format("Gimbal[%d] pitch range extension enabled", getIndex()));
            }

            @Override
            public void onFailure(final @NonNull IDJIError error) {}
        });
    }

    public void close() {
        listeners.cancelAll();
        state.close();
    }

    public <T> DJIKey<T> createKey(final DJIKeyInfo<T> keyInfo) {
        return KeyTools.createKey(keyInfo, index);
    }

    public <T, R> DJIKey.ActionKey<T, R> createKey(final DJIActionKeyInfo<T, R> keyInfo) {
        return KeyTools.createKey(keyInfo, index);
    }

    public DatedValue<GimbalStateAdapter> getState() {
        return state.asDatedValue();
    }

    public void sendResetCommands() {
        final GimbalAngleRotation rotation = new GimbalAngleRotation();
        rotation.setDuration(DronelinkDJI2.GimbalRotationMinTime);
        rotation.setMode(GimbalAngleRotationMode.ABSOLUTE_ANGLE);
        rotation.setPitch(-12.0);
        rotation.setRoll(0.0);
        if (isYawAdjustSupported && state.getMode() != GimbalMode.YAW_FOLLOW) {
            KeyManager.getInstance().setValue(createKey(GimbalKey.KeyGimbalMode), dji.sdk.keyvalue.value.gimbal.GimbalMode.YAW_FOLLOW, null);
            KeyManager.getInstance().performAction(createKey(GimbalKey.KeyGimbalReset), null);
        }
        KeyManager.getInstance().performAction(createKey(GimbalKey.KeyRotateByAngle), rotation, null);
    }

    @Override
    public int getIndex() {
        return index.value();
    }

    @Override
    public void sendVelocityCommand(final VelocityGimbalCommand command, final GimbalMode mode) {
        final GimbalSpeedRotation rotation = new GimbalSpeedRotation();
        rotation.setPitch(Math.max(-90, Math.min(90, Math.toDegrees(command.velocity.getPitch()))));
        rotation.setRoll(Math.max(-90, Math.max(-90, Math.min(90, Math.toDegrees(command.velocity.getRoll())))));
        if (isAdjustYaw360Supported() && mode == GimbalMode.FREE) {
            rotation.setYaw(Math.toDegrees(command.velocity.getYaw()));
        }
        setPendingSpeedRotation(rotation);
    }

    @Override
    public void reset() {
        //doesn't work? GimbalResetType.PITCH_UP_OR_DOWN_WITH_YAW_CENTER
        KeyManager.getInstance().performAction(createKey(GimbalKey.KeyGimbalReset), GimbalResetType.ONLY_YAW, null);
        KeyManager.getInstance().performAction(createKey(GimbalKey.KeyGimbalReset), GimbalResetType.TOGGLE_PITCH, null);
    }

    @Override
    public void fineTuneRoll(final double roll) {
        KeyManager.getInstance().performAction(createKey(GimbalKey.KeyFineTuneRollInDegrees), Convert.RadiansToDegrees(roll), null);
    }

    @Override
    public List<EnumElement> getEnumElements(final String parameter) {
        return enumElements.get(parameter);
    }

    public GimbalSpeedRotation getPendingSpeedRotation() {
        try {
            return serialQueue.submit(() -> pendingSpeedRotation).get();
        }
        catch (final ExecutionException | InterruptedException e) {
            return null;
        }
    }

    public void setPendingSpeedRotation(final GimbalSpeedRotation newPendingSpeedRotation) {
        serialQueue.execute(() -> pendingSpeedRotation = newPendingSpeedRotation);
    }

    public CommandError executeCommand(final Context context, final GimbalCommand command, final Command.Finisher finished) {
        if (command instanceof ModeGimbalCommand) {
            final GimbalMode target = ((ModeGimbalCommand) command).mode;
            Command.conditionallyExecute(target != state.getMode(), finished, () -> KeyManager.getInstance().setValue(
                    createKey(GimbalKey.KeyGimbalMode), DronelinkDJI2.getGimbalMode(target), DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof OrientationGimbalCommand) {
            final Orientation3Optional orientation = ((OrientationGimbalCommand) command).orientation;
            if (orientation.getPitch() == null && orientation.getRoll() == null && orientation.getYaw() == null) {
                finished.execute(null);
                return null;
            }

            final GimbalAngleRotation rotation = new GimbalAngleRotation();
            //rotation.setDuration(DronelinkDJI2.GimbalRotationMinTime);

            Double pitch = orientation.getPitch() == null ? null : Convert.RadiansToDegrees(orientation.getPitch());
            Double roll = orientation.getRoll() == null ? null : Convert.RadiansToDegrees(orientation.getRoll());
            Double yaw = orientation.getYaw();
            if (yaw != null && !(state.getMode() == GimbalMode.FREE && isAdjustYaw360Supported())) {
                yaw = null;
            }

            if (pitch == null && roll == null && yaw == null) {
                finished.execute(null);
                return null;
            }

            if (pitch != null) {
                rotation.setPitch(pitch);
            }

            if (roll != null) {
                rotation.setRoll(roll);
            }

            if (yaw != null) {
                rotation.setYaw(yaw);
            }

            rotation.setMode(GimbalAngleRotationMode.ABSOLUTE_ANGLE);
            KeyManager.getInstance().performAction(createKey(GimbalKey.KeyRotateByAngle), rotation, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(final EmptyMsg emptyMsg) {
                    if (finished != null) {
                        commandFinishOrientationVerify(context, (OrientationGimbalCommand) command, finished);
                    }
                }

                @Override
                public void onFailure(final @NonNull IDJIError error) {
                    if (finished != null) {
                        finished.execute(new CommandError(error.errorCode()));
                    }
                }
            });
            return null;
        }

        return new CommandError(context.getString(R.string.MissionDisengageReason_command_type_unhandled) + ": " + command.type);
    }

    private boolean isAdjustYaw360Supported() {
        if (isYawAdjustSupported) {
            final GimbalAttitudeRange attitudeRange = this.attitudeRange;
            if (attitudeRange != null) {
                return attitudeRange.getYaw().getMin().intValue() <= -180 && attitudeRange.getYaw().getMax().intValue() >= 180;
            }
        }
        return false;
    }

    private void commandFinishOrientationVerify(final Context context, final OrientationGimbalCommand command, final Command.Finisher finished) {
        commandFinishOrientationVerify(context, command, 0, 20, Convert.DegreesToRadians(2.0), finished);
    }

    private void commandFinishOrientationVerify(final Context context, final OrientationGimbalCommand command, final int attempt, final int maxAttempts, final double threshold, final Command.Finisher finished) {
        if (attempt >= maxAttempts) {
            finished.execute(new CommandError(context.getString(R.string.DJI2GimbalAdapter_gimbalCommand_orientation_not_achieved)));
            return;
        }

        boolean verified = true;

        if (command.orientation.getPitch() != null) {
            verified = Math.abs(Convert.AngleDifferenceSigned(command.orientation.getPitch(), state.getOrientation().getPitch())) <= threshold;
        }

        if (command.orientation.getRoll() != null) {
            verified = verified && Math.abs(Convert.AngleDifferenceSigned(command.orientation.getRoll(), state.getOrientation().getRoll())) <= threshold;
        }

        if (command.orientation.getYaw() != null && state.getMode() == GimbalMode.FREE && isAdjustYaw360Supported()) {
            verified = verified && Math.abs(Convert.AngleDifferenceSigned(command.orientation.getYaw(), state.getOrientation().getYaw())) <= threshold;
        }

        if (verified) {
            finished.execute(null);
            return;
        }

        final long wait = 100;
        Log.d(TAG, "Gimbal command finished and waiting for orientation (" + ((attempt + 1) * wait) + "ms)... (" + command.id + ")");
        new Handler().postDelayed(() -> commandFinishOrientationVerify(context, command, attempt + 1, maxAttempts, threshold, finished), wait);
    }
}