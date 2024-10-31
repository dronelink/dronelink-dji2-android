//  DJI2CameraAdapter.java
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

import com.dronelink.core.DatedValue;
import com.dronelink.core.DroneSession;
import com.dronelink.core.DroneSessionManager;
import com.dronelink.core.Dronelink;
import com.dronelink.core.Kernel;
import com.dronelink.core.MissionExecutor;
import com.dronelink.core.adapters.CameraAdapter;
import com.dronelink.core.adapters.CameraStateAdapter;
import com.dronelink.core.adapters.EnumElement;
import com.dronelink.core.adapters.EnumElementTuple;
import com.dronelink.core.adapters.EnumElementsCollection;
import com.dronelink.core.command.Command;
import com.dronelink.core.command.CommandError;
import com.dronelink.core.kernel.command.camera.CameraCommand;
import com.dronelink.core.kernel.command.camera.StorageCustomFolderNameCameraCommand;
import com.dronelink.core.kernel.command.camera.ModeCameraCommand;
import com.dronelink.core.kernel.command.camera.PhotoModeCameraCommand;
import com.dronelink.core.kernel.command.camera.StartCaptureCameraCommand;
import com.dronelink.core.kernel.command.camera.StopCaptureCameraCommand;
import com.dronelink.core.kernel.command.camera.StorageLocationCameraCommand;
import com.dronelink.core.kernel.command.camera.VideoStreamSourceCameraCommand;
import com.dronelink.core.kernel.core.Message;
import com.dronelink.core.kernel.core.enums.CameraMode;
import com.dronelink.core.kernel.core.enums.CameraPhotoMode;
import com.dronelink.core.kernel.core.enums.CameraStorageLocation;
import com.dronelink.core.kernel.core.enums.CameraVideoStreamSource;
import com.dronelink.dji2.DJI2ListenerGroup;
import com.dronelink.dji2.DronelinkDJI2;
import com.dronelink.dji2.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.DJIActionKeyInfo;
import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.DJIKeyInfo;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraType;
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType;
import dji.sdk.keyvalue.value.camera.CustomExpandNameSettings;
import dji.sdk.keyvalue.value.camera.GeneratedMediaFileInfo;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;

public class DJI2CameraAdapter implements CameraAdapter {
    public interface GeneratedMediaFileInfoCallback {
        void onGeneratedMediaFileInfo(final GeneratedMediaFileInfo info);
    }

    private static final String TAG = DJI2CameraAdapter.class.getCanonicalName();

    private final ComponentIndexType index;
    private final DJI2ListenerGroup listeners = new DJI2ListenerGroup();
    private final EnumElementsCollection enumElements = new EnumElementsCollection();
    private CameraType type;
    private final DJI2CameraStateAdapter defaultState;
    private final Map<CameraLensType, DJI2CameraStateAdapter> lensStates = new HashMap<>();
    private CameraVideoStreamSourceType videoStreamSource;
    private CustomExpandNameSettings customExpandNameSettings;
    private DatedValue<GeneratedMediaFileInfo> mostRecentGeneratedMediaFileInfo;

    public DJI2CameraAdapter(final Context context, final DJI2DroneAdapter drone, final ComponentIndexType index, final GeneratedMediaFileInfoCallback generatedMediaFileInfoReceiver) {
        this.index = index;
        this.defaultState = new DJI2CameraStateAdapter(context, drone, index, CameraLensType.CAMERA_LENS_DEFAULT);

        KeyManager.getInstance().getValue(createKey(CameraKey.KeyCameraType), new CommonCallbacks.CompletionCallbackWithParam<CameraType>() {
            @Override
            public void onSuccess(final CameraType t) {
                type = t;
                Log.i(TAG, "Camera Type: " + t.name());
            }

            @Override
            public void onFailure(final @NonNull IDJIError error) {
                Log.e(TAG, "Camera type failed: " + error.description());
            }
        });

        listeners.init(createKey(CameraKey.KeyCameraVideoStreamSourceRange), (oldValue, newValue) -> {
            synchronized (lensStates) {
                for (final DJI2CameraStateAdapter state : lensStates.values()) {
                    state.close();
                }
                lensStates.clear();
                final List<String> range = new ArrayList<>();
                if (newValue != null) {
                    for (final CameraVideoStreamSourceType value : newValue) {
                        final CameraLensType lensType = DronelinkDJI2.getCameraLensType(value);
                        if (lensType != CameraLensType.CAMERA_LENS_DEFAULT) {
                            lensStates.put(lensType, new DJI2CameraStateAdapter(context, drone, index, lensType));
                        }
                        range.add(Kernel.enumRawValue(DronelinkDJI2.getCameraVideoStreamSource(value)));
                    }
                }
                enumElements.update("CameraVideoStreamSource", range);
            }
        });

        listeners.init(createKey(CameraKey.KeyCameraVideoStreamSource), (oldValue, newValue) -> videoStreamSource = newValue);

        listeners.init(KeyTools.createKey(CameraKey.KeyNewlyGeneratedMediaFile, index), (oldValue, newValue) -> {
            if (newValue != null) {
                generatedMediaFileInfoReceiver.onGeneratedMediaFileInfo(newValue);
                mostRecentGeneratedMediaFileInfo = new DatedValue<>(newValue);
            }
        });
        listeners.init(KeyTools.createKey(CameraKey.KeyCustomExpandDirectoryNameSettings), (oldValue, newValue) -> customExpandNameSettings = newValue);
    }

    private <T> DJIKey<T> createKey(final DJIKeyInfo<T> keyInfo) {
        return KeyTools.createKey(keyInfo, index);
    }

    private <T, R> DJIKey.ActionKey<T, R> createKey(final DJIActionKeyInfo<T, R> keyInfo) {
        return KeyTools.createKey(keyInfo, index);
    }

    public void close() {
        listeners.cancelAll();
        defaultState.close();
        for (final DJI2CameraStateAdapter state : lensStates.values()) {
            state.close();
        }
    }

    private DJI2CameraStateAdapter getActiveState() {
        DJI2CameraStateAdapter state = null;

        final CameraLensType lensType = DronelinkDJI2.getCameraLensType(videoStreamSource);
        if (lensType != null && lensType != CameraLensType.CAMERA_LENS_DEFAULT) {
            synchronized (lensStates) {
                state = lensStates.get(lensType);
            }
        }

        if (state == null) {
            return defaultState;
        }

        return state;
    }

    public DatedValue<CameraStateAdapter> getState() {
        return getActiveState().asDatedValue();
    }

    public void sendResetCommands() {
        if (getActiveState().isCapturingVideo()) {
            KeyManager.getInstance().performAction(createKey(CameraKey.KeyStopRecord), null);
        }
        else if (getActiveState().isCapturingPhotoInterval()) {
            KeyManager.getInstance().performAction(createKey(CameraKey.KeyStopShootPhoto), null);
        }
    }

    public List<Message> getStatusMessages() {
        return getActiveState().getStatusMessages();
    }

    @Override
    public String getModel() {
        return type == null ? "Unknown" : type.name();
    }

    @Override
    public int getIndex() {
        return index.value();
    }

    @Override
    public void format(final CameraStorageLocation storageLocation, final Command.Finisher finisher) {
        KeyManager.getInstance().performAction(
                createKey(CameraKey.KeyFormatStorage),
                DronelinkDJI2.getCameraStorageLocation(storageLocation),
                DronelinkDJI2.createCompletionCallbackWithParam(finisher));
    }

    @Override
    public void setHistogramEnabled(final boolean enabled, final Command.Finisher finisher) {
        //FIXME not working - waiting for fix from DJI
        KeyManager.getInstance().setValue(
                createKey(CameraKey.KeyHistogramEnabled),
                enabled,
                DronelinkDJI2.createCompletionCallback(finisher));
    }

    @Override
    public List<EnumElement> getEnumElements(final String parameter) {
        final List<EnumElement> elements = enumElements.get(parameter);
        if (elements != null) {
            return elements;
        }
        return getActiveState().getEnumElements(parameter);
    }

    @Override
    public List<EnumElementTuple> getTupleEnumElements(String parameter) {
        return getActiveState().getEnumElementTuples(parameter);
    }

    public CommandError executeCommand(final Context context, final CameraCommand command, final Command.Finisher finished) {
        final DJI2CameraStateAdapter state = getActiveState();

        if (command instanceof StartCaptureCameraCommand) {
            switch (state.getMode()) {
                case PHOTO:
                    if (state.isCapturingPhotoInterval()) {
                        Log.d(TAG, "Camera start capture skipped, already shooting interval photos");
                        finished.execute(null);
                        return null;
                    }

                    Log.d(TAG, "Camera start capture photo");
                    final Date started = new Date();
                    KeyManager.getInstance().performAction(createKey(CameraKey.KeyStartShootPhoto), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(final EmptyMsg emptyMsg) {
                            if (finished != null) {
                                //waiting since isBusy will still be false for a bit
                                new Handler().postDelayed(() -> {
                                    final StartCaptureCameraCommand startCaptureCameraCommand = (StartCaptureCameraCommand) command;
                                    if (startCaptureCameraCommand.verifyFileCreated) {
                                        commandFinishStartShootPhotoVerifyFile(context, startCaptureCameraCommand, started, finished);
                                    } else {
                                        commandFinishNotBusy(startCaptureCameraCommand, finished);
                                    }
                                }, 250);
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

                case VIDEO:
                    if (state.isCapturingVideo()) {
                        Log.d(TAG, "Camera start capture skipped, already recording video");
                        finished.execute(null);
                        return null;
                    }

                    Log.d(TAG, "Camera start capture video");
                    KeyManager.getInstance().performAction(createKey(CameraKey.KeyStartRecord), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(final EmptyMsg emptyMsg) {
                            if (finished != null) {
                                //waiting since isBusy will still be false for a bit
                                new Handler().postDelayed(() -> commandFinishNotBusy(command, finished), 250);
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

                case PLAYBACK:
                case DOWNLOAD:
                case BROADCAST:
                case UNKNOWN:
                    Log.i(TAG, "Camera start capture invalid mode: " + state.getMode().toString());
                    return new CommandError(context.getString(com.dronelink.core.R.string.MissionDisengageReason_drone_camera_mode_invalid_title));
            }
        }

        if (command instanceof StopCaptureCameraCommand) {
            switch (state.getMode()) {
                case PHOTO:
                    if (!state.isCapturingPhotoInterval()) {
                        Log.d(TAG, "Camera stop capture skipped, not shooting interval photos");
                        finished.execute(null);
                        return null;
                    }

                    Log.d(TAG, "Camera stop capture interval photo");
                    KeyManager.getInstance().performAction(createKey(CameraKey.KeyStopShootPhoto), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(final EmptyMsg emptyMsg) {
                            if (finished != null) {
                                commandFinishStopCapture(context, command, finished);
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

                case VIDEO:
                    if (!state.isCapturingVideo()) {
                        Log.d(TAG, "Camera stop capture skipped, not recording video");
                        finished.execute(null);
                        return null;
                    }

                    Log.d(TAG, "Camera stop capture video");
                    KeyManager.getInstance().performAction(createKey(CameraKey.KeyStopRecord), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(final EmptyMsg emptyMsg) {
                            if (finished == null) {
                                commandFinishStopCapture(context, command, finished);
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

                case PLAYBACK:
                case DOWNLOAD:
                case BROADCAST:
                case UNKNOWN:
                    Log.i(TAG, "Camera start capture invalid mode: " + state.getMode().toString());
                    return new CommandError(context.getString(com.dronelink.core.R.string.MissionDisengageReason_drone_camera_mode_invalid_title));
            }
        }

        if (command instanceof ModeCameraCommand) {
            final CameraMode target = ((ModeCameraCommand) command).mode;
            Command.conditionallyExecute(target != state.getMode(), finished, () -> KeyManager.getInstance().setValue(
                    createKey(CameraKey.KeyCameraMode),
                    DronelinkDJI2.getCameraMode(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof PhotoModeCameraCommand) {
            final CameraPhotoMode target = ((PhotoModeCameraCommand) command).photoMode;
            Command.conditionallyExecute(target != state.getPhotoMode(), finished, () -> KeyManager.getInstance().setValue(
                    createKey(CameraKey.KeyCameraFlatMode),
                    DronelinkDJI2.getCameraFlatMode(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof StorageCustomFolderNameCameraCommand) {
            final String target = ((StorageCustomFolderNameCameraCommand) command).customFolderName;
            final String current = customExpandNameSettings == null ? null : customExpandNameSettings.getCustomContent();
            if (target == null) {
                return new CommandError(context.getString(R.string.MissionDisengageReason_command_value_invalid));
            }
            if (state.isCapturingVideo()) {
                return new CommandError(context.getString(R.string.DJI2CameraAdapter_cameraCommand_storage_custom_folder_name_video_error));
            }

            String droneName = "";
            String missionName = "";
            final DroneSessionManager droneSessionManager = Dronelink.getInstance().getTargetDroneSessionManager();
            if (droneSessionManager != null) {
                final DroneSession session = droneSessionManager.getSession();
                if (session != null) {
                    droneName = session.getName() == null ? "" : session.getName();
                }
            }
            final MissionExecutor missionExecutor = Dronelink.getInstance().getMissionExecutor();
            if (missionExecutor != null) {
                missionName = missionExecutor.descriptors == null ? "" : missionExecutor.descriptors.name;
            }

            //Secret target syntax {drone.name} and {mission.name} to insert drone name and mission name into command. Only supported in english.
            final String targetResolved = target.replace("{drone.name}", droneName).replace("{mission.name}", missionName).replaceAll("[^a-zA-Z0-9]+", "-");

            //validation that the target is a valid folder name per DJI SDK docs. The regex ensures its not just a number and only contains letters (in all languages), numbers, and hyphens.
            if (Pattern.matches("\\d+", targetResolved) || !Pattern.matches("[\\p{L}\\p{N}\\-]+", targetResolved)) {
                return new CommandError(context.getString(R.string.DJI2CameraAdapter_cameraCommand_storage_custom_folder_name_invalid));
            }

            customExpandNameSettings.setCustomContent(targetResolved);
            Command.conditionallyExecute(!targetResolved.equals(current), finished, () -> KeyManager.getInstance().setValue(
                    createKey(CameraKey.KeyCustomExpandDirectoryNameSettings),
                    customExpandNameSettings,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof StorageLocationCameraCommand) {
            final CameraStorageLocation target = ((StorageLocationCameraCommand) command).storageLocation;
            Command.conditionallyExecute(target != state.getStorageLocation(), finished, () -> KeyManager.getInstance().setValue(
                    createKey(CameraKey.KeyCameraStorageLocation),
                    DronelinkDJI2.getCameraStorageLocation(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof VideoStreamSourceCameraCommand) {
            final CameraVideoStreamSource target = ((VideoStreamSourceCameraCommand)command).videoStreamSource;
            Command.conditionallyExecute(target != DronelinkDJI2.getCameraVideoStreamSource(videoStreamSource), finished, () -> KeyManager.getInstance().setValue(
                    createKey(CameraKey.KeyCameraVideoStreamSource),
                    DronelinkDJI2.getCameraVideoStreamSource(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        return state.executeCommand(context, command, finished);
    }

    private void commandFinishStopCapture(final Context context, final CameraCommand command, final Command.Finisher finished) {
        commandFinishStopCapture(context, command, 0, 20, finished);
    }

    private void commandFinishStopCapture(final Context context, final CameraCommand command, final int attempt, final int maxAttempts, final Command.Finisher finished) {
        if (attempt >= maxAttempts) {
            finished.execute(new CommandError(context.getString(R.string.DJI2CameraAdapter_cameraCommand_stop_capture_error)));
            return;
        }

        final DJI2CameraStateAdapter state = getActiveState();
        if (!state.isCapturing()) {
            finished.execute(null);
            return;
        }

        final long wait = 250;
        Log.d(TAG, "Camera command finished and waiting for camera to stop capturing (" + ((attempt + 1) * wait) + "ms)... (" + command.id + ")");
        new Handler().postDelayed(() -> commandFinishNotBusy(command, attempt + 1, maxAttempts, finished), wait);
    }

    private void commandFinishStartShootPhotoVerifyFile(final Context context, final StartCaptureCameraCommand command, final Date started, final Command.Finisher finished) {
        commandFinishStartShootPhoto(context, command, started, 0, 20, finished);
    }

    private void commandFinishStartShootPhoto(final Context context, final StartCaptureCameraCommand command, final Date started, final int attempt, final int maxAttempts, final Command.Finisher finished) {
        if (attempt >= maxAttempts) {
            finished.execute(new CommandError(context.getString(R.string.DJI2CameraAdapter_cameraCommand_start_shoot_photo_no_file)));
            return;
        }

        final DatedValue<GeneratedMediaFileInfo> mostRecentGeneratedMediaFileInfo = this.mostRecentGeneratedMediaFileInfo;
        if (mostRecentGeneratedMediaFileInfo != null) {
            final long timeSinceMostRecentCameraFile = mostRecentGeneratedMediaFileInfo.date.getTime() - started.getTime();
            if (timeSinceMostRecentCameraFile > 0) {
                Log.d(TAG, "Camera start shoot photo found camera file (" + mostRecentGeneratedMediaFileInfo.value.getIndex() + ") after " + timeSinceMostRecentCameraFile + "ms (" + command.id + ")");
                commandFinishNotBusy(command, finished);
                return;
            }
        }

        final long wait = 250;
        Log.d(TAG, "Camera start shoot photo finished and waiting for camera file (" + ((attempt + 1) * wait) + "ms)... (" + command.id + ")");
        new Handler().postDelayed(() -> commandFinishStartShootPhoto(context, command, started, attempt + 1, maxAttempts, finished), wait);
    }

    private void commandFinishNotBusy(final CameraCommand command, final Command.Finisher finished) {
        commandFinishNotBusy(command, 0, 10, finished);
    }

    private void commandFinishNotBusy(final CameraCommand command, final int attempt, final int maxAttempts, final Command.Finisher finished) {
        final DJI2CameraStateAdapter state = getActiveState();
        if (attempt >= maxAttempts || !state.isBusy()) {
            finished.execute(null);
            return;
        }

        Log.d(TAG, "Camera command finished and waiting for camera to not be busy (" + (attempt + 1) + ")... (" + command.id + ")");
        new Handler().postDelayed(() -> commandFinishNotBusy(command, attempt + 1, maxAttempts, finished), 100);
    }
}
