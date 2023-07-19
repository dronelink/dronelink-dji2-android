//  DJI2CameraStateAdapter.java
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
import com.dronelink.core.Dronelink;
import com.dronelink.core.Kernel;
import com.dronelink.core.adapters.CameraStateAdapter;
import com.dronelink.core.adapters.EnumElement;
import com.dronelink.core.adapters.EnumElementsCollection;
import com.dronelink.core.command.Command;
import com.dronelink.core.command.CommandError;
import com.dronelink.core.kernel.command.camera.AEBCountCameraCommand;
import com.dronelink.core.kernel.command.camera.ApertureCameraCommand;
import com.dronelink.core.kernel.command.camera.AutoExposureLockCameraCommand;
import com.dronelink.core.kernel.command.camera.AutoLockGimbalCameraCommand;
import com.dronelink.core.kernel.command.camera.CameraCommand;
import com.dronelink.core.kernel.command.camera.ColorCameraCommand;
import com.dronelink.core.kernel.command.camera.ContrastCameraCommand;
import com.dronelink.core.kernel.command.camera.DewarpingCameraCommand;
import com.dronelink.core.kernel.command.camera.DisplayModeCameraCommand;
import com.dronelink.core.kernel.command.camera.ExposureCompensationCameraCommand;
import com.dronelink.core.kernel.command.camera.ExposureCompensationStepCameraCommand;
import com.dronelink.core.kernel.command.camera.ExposureModeCameraCommand;
import com.dronelink.core.kernel.command.camera.FileIndexModeCameraCommand;
import com.dronelink.core.kernel.command.camera.FocusCameraCommand;
import com.dronelink.core.kernel.command.camera.FocusDistanceCameraCommand;
import com.dronelink.core.kernel.command.camera.FocusModeCameraCommand;
import com.dronelink.core.kernel.command.camera.FocusRingCameraCommand;
import com.dronelink.core.kernel.command.camera.ISOCameraCommand;
import com.dronelink.core.kernel.command.camera.MechanicalShutterCameraCommand;
import com.dronelink.core.kernel.command.camera.MeteringModeCameraCommand;
import com.dronelink.core.kernel.command.camera.PhotoAspectRatioCameraCommand;
import com.dronelink.core.kernel.command.camera.PhotoFileFormatCameraCommand;
import com.dronelink.core.kernel.command.camera.PhotoIntervalCameraCommand;
import com.dronelink.core.kernel.command.camera.SaturationCameraCommand;
import com.dronelink.core.kernel.command.camera.SharpnessCameraCommand;
import com.dronelink.core.kernel.command.camera.ShutterSpeedCameraCommand;
import com.dronelink.core.kernel.command.camera.SpotMeteringTargetCameraCommand;
import com.dronelink.core.kernel.command.camera.VideoCaptionCameraCommand;
import com.dronelink.core.kernel.command.camera.VideoFileCompressionStandardCameraCommand;
import com.dronelink.core.kernel.command.camera.VideoFileFormatCameraCommand;
import com.dronelink.core.kernel.command.camera.VideoModeCameraCommand;
import com.dronelink.core.kernel.command.camera.VideoResolutionFrameRateCameraCommand;
import com.dronelink.core.kernel.command.camera.VideoStandardCameraCommand;
import com.dronelink.core.kernel.command.camera.VideoStreamSourceCameraCommand;
import com.dronelink.core.kernel.command.camera.WhiteBalanceCustomCameraCommand;
import com.dronelink.core.kernel.command.camera.WhiteBalancePresetCameraCommand;
import com.dronelink.core.kernel.command.camera.ZoomPercentCameraCommand;
import com.dronelink.core.kernel.command.camera.ZoomRatioCameraCommand;
import com.dronelink.core.kernel.core.CameraFocusCalibration;
import com.dronelink.core.kernel.core.CameraZoomSpecification;
import com.dronelink.core.kernel.core.Message;
import com.dronelink.core.kernel.core.PercentZoomSpecification;
import com.dronelink.core.kernel.core.Point2;
import com.dronelink.core.kernel.core.RatioZoomSpecification;
import com.dronelink.core.kernel.core.enums.CameraAEBCount;
import com.dronelink.core.kernel.core.enums.CameraAperture;
import com.dronelink.core.kernel.core.enums.CameraBurstCount;
import com.dronelink.core.kernel.core.enums.CameraColor;
import com.dronelink.core.kernel.core.enums.CameraDisplayMode;
import com.dronelink.core.kernel.core.enums.CameraExposureCompensation;
import com.dronelink.core.kernel.core.enums.CameraExposureMode;
import com.dronelink.core.kernel.core.enums.CameraFileIndexMode;
import com.dronelink.core.kernel.core.enums.CameraFocusMode;
import com.dronelink.core.kernel.core.enums.CameraISO;
import com.dronelink.core.kernel.core.enums.CameraMeteringMode;
import com.dronelink.core.kernel.core.enums.CameraMode;
import com.dronelink.core.kernel.core.enums.CameraPhotoAspectRatio;
import com.dronelink.core.kernel.core.enums.CameraPhotoFileFormat;
import com.dronelink.core.kernel.core.enums.CameraPhotoMode;
import com.dronelink.core.kernel.core.enums.CameraShutterSpeed;
import com.dronelink.core.kernel.core.enums.CameraStorageLocation;
import com.dronelink.core.kernel.core.enums.CameraVideoFieldOfView;
import com.dronelink.core.kernel.core.enums.CameraVideoFileCompressionStandard;
import com.dronelink.core.kernel.core.enums.CameraVideoFileFormat;
import com.dronelink.core.kernel.core.enums.CameraVideoFrameRate;
import com.dronelink.core.kernel.core.enums.CameraVideoMode;
import com.dronelink.core.kernel.core.enums.CameraVideoResolution;
import com.dronelink.core.kernel.core.enums.CameraVideoStandard;
import com.dronelink.core.kernel.core.enums.CameraVideoStreamSource;
import com.dronelink.core.kernel.core.enums.CameraWhiteBalancePreset;
import com.dronelink.dji2.DJI2ListenerGroup;
import com.dronelink.dji2.DronelinkDJI2;
import com.dronelink.dji2.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.DJIKeyInfo;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraExposureSettings;
import dji.sdk.keyvalue.value.camera.CameraHybridZoomSpec;
import dji.sdk.keyvalue.value.camera.CameraOpticalZoomSpec;
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType;
import dji.sdk.keyvalue.value.camera.CameraWhiteBalanceInfo;
import dji.sdk.keyvalue.value.camera.CameraWhiteBalanceMode;
import dji.sdk.keyvalue.value.camera.PhotoAEBExposureOffset;
import dji.sdk.keyvalue.value.camera.PhotoAEBSettings;
import dji.sdk.keyvalue.value.camera.PhotoBurstCount;
import dji.sdk.keyvalue.value.camera.PhotoFileFormat;
import dji.sdk.keyvalue.value.camera.PhotoIntervalShootSettings;
import dji.sdk.keyvalue.value.camera.PhotoRatio;
import dji.sdk.keyvalue.value.camera.SSDTotalSpace;
import dji.sdk.keyvalue.value.camera.ThermalDisplayMode;
import dji.sdk.keyvalue.value.camera.VideoFileCompressionStandard;
import dji.sdk.keyvalue.value.camera.VideoFileFormat;
import dji.sdk.keyvalue.value.camera.VideoRecordMode;
import dji.sdk.keyvalue.value.camera.VideoResolutionFrameRate;
import dji.sdk.keyvalue.value.camera.VideoResolutionFrameRateAndFov;
import dji.sdk.keyvalue.value.camera.VideoStandard;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.DoublePoint2D;
import dji.sdk.keyvalue.value.common.DoubleRect;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;

public
class DJI2CameraStateAdapter implements CameraStateAdapter {
    private static final String TAG = DJI2CameraStateAdapter.class.getCanonicalName();

    private final Context context;
    private final DJI2DroneAdapter drone;
    public final ComponentIndexType index;
    public final CameraLensType lensType;
    private final DJI2ListenerGroup listeners = new DJI2ListenerGroup();
    private final EnumElementsCollection enumElements = new EnumElementsCollection();
    private boolean isShootingPhoto = false;
    private Date isShootingPhotoUpdated = new Date();
    private Integer photoIntervalCountdown;
    private boolean isShootingBurstPhoto = false;
    private boolean isShootingHyperlapse = false;
    private boolean isShootingPhotoPanorama = false;
    private boolean isShootingRAWBurstPhoto = false;
    private boolean isShootingSinglePhoto = false;
    private boolean isShootingSinglePhotoInRAWFormat = false;
    private boolean isShootingSuperResolutionPhoto = false;
    private boolean isShootingVisionBokehPhoto = false;
    private boolean isRecording = false;
    private boolean isSDCardInserted = false;
    private dji.sdk.keyvalue.value.camera.CameraStorageLocation storageLocation;
    private Integer remainingSpaceSDCard;
    private Integer totalSpaceSDCard;
    private Integer remainingSpaceSSD;
    private SSDTotalSpace totalSpaceSSD;
    private Integer remainingSpaceInternalStorage;
    private Integer totalSpaceInternalStorage;
    private Integer availablePhotoCountSDCard;
    private Integer availablePhotoCountSSD;
    private Integer availablePhotoCountInternalStorage;
    private dji.sdk.keyvalue.value.camera.CameraFlatMode flatMode;
    private dji.sdk.keyvalue.value.camera.CameraColor color;
    private Integer contrast;
    private Integer saturation;
    private Integer sharpness;
    private ThermalDisplayMode displayMode;
    private PhotoFileFormat photoFileFormat;
    private PhotoIntervalShootSettings photoIntervalShootSettings;
    private PhotoBurstCount photoBurstCount;
    private PhotoAEBSettings photoAEBSettings;
    private Boolean videoCaptionEnabled;
    private VideoFileCompressionStandard videoFileCompressionStandard;
    private VideoFileFormat videoFileFormat;
    private VideoStandard videoStandard;
    private CameraVideoStreamSourceType videoStreamSource;
    private VideoRecordMode videoRecordMode;
    private VideoResolutionFrameRateAndFov videoResolutionFrameRateFov;
    private Integer recordingTime;
    private dji.sdk.keyvalue.value.camera.CameraExposureMode exposureMode;
    private CameraExposureSettings exposureSettings;
    private dji.sdk.keyvalue.value.camera.CameraExposureCompensation exposureCompensation;
    private dji.sdk.keyvalue.value.camera.CameraFileIndexMode fileIndexMode;
    private dji.sdk.keyvalue.value.camera.CameraISO iso;
    private dji.sdk.keyvalue.value.camera.CameraShutterSpeed shutterSpeed;
    private dji.sdk.keyvalue.value.camera.CameraAperture aperture;
    private CameraWhiteBalanceInfo whiteBalance;
    private short[] histogram;
    private dji.sdk.keyvalue.value.camera.CameraFocusMode focusMode;
    private Integer focusRingValue;
    private Integer focusRingMax;
    private dji.sdk.keyvalue.value.camera.CameraMeteringMode meteringMode;
    private boolean aeLockEnabled = false;
    private PhotoRatio photoRatio;
    private boolean lockGimbalDuringShootPhotoEnabled = false;
    private boolean mechanicalShutterEnabled = false;
    private boolean dewarpingEnabled = false;
    private boolean isHybridZoomSupported = false;
    private Integer hybridZoomFocalLength;
    private CameraHybridZoomSpec hybridZoomSpecification;
    private int[] zoomRatios;
    private int[] thermalZoomRatios;
    private Double currentThermalZoomRatio;
    private Double currentZoomRatio;

    public DJI2CameraStateAdapter(final Context context, final DJI2DroneAdapter drone, final ComponentIndexType index, final CameraLensType lensType) {
        this.context = context;
        this.drone = drone;
        this.index = index;
        this.lensType = lensType;

        listeners.init(createLensKey(CameraKey.KeyIntervalModeParamRange), (oldValue, newValue) -> {
            final List<EnumElement> elements = new ArrayList<>();
            if (newValue != null) {
                for (final Double interval : newValue) {
                    if (interval.intValue() == interval) {
                        elements.add(new EnumElement(interval.intValue() + " s", interval));
                    }
                    else {
                        elements.add(new EnumElement(interval + " s", interval));
                    }
                }
            }

            enumElements.put("CameraPhotoInterval", elements);
        });

        listeners.init(createLensKey(CameraKey.KeyCameraApertureRange), (oldValue, newValue) -> {
            final List<String> range = new ArrayList<>();
            if (newValue != null) {
                for (final dji.sdk.keyvalue.value.camera.CameraAperture value : newValue) {
                    range.add(Kernel.enumRawValue(DronelinkDJI2.getCameraAperture(value)));
                }
            }
            enumElements.update("CameraAperture", range);
        });

        listeners.init(createLensKey(CameraKey.KeyExposureCompensationRange), (oldValue, newValue) -> {
            final List<String> range = new ArrayList<>();
            if (newValue != null) {
                for (final dji.sdk.keyvalue.value.camera.CameraExposureCompensation value : newValue) {
                    range.add(Kernel.enumRawValue(DronelinkDJI2.getCameraExposureCompensation(value)));
                }
            }
            enumElements.update("CameraExposureCompensation", range);
        });

        listeners.init(createLensKey(CameraKey.KeyExposureModeRange), (oldValue, newValue) -> {
            final List<String> range = new ArrayList<>();
            if (newValue != null) {
                for (final dji.sdk.keyvalue.value.camera.CameraExposureMode value : newValue) {
                    range.add(Kernel.enumRawValue(DronelinkDJI2.getCameraExposureMode(value)));
                }
            }
            enumElements.update("CameraExposureMode", range);
        });

        listeners.init(createLensKey(CameraKey.KeyISORange), (oldValue, newValue) -> {
            final List<String> range = new ArrayList<>();
            if (newValue != null) {
                for (final dji.sdk.keyvalue.value.camera.CameraISO value : newValue) {
                    range.add(Kernel.enumRawValue(DronelinkDJI2.getCameraISO(value)));
                }
            }
            enumElements.update("CameraISO", range);
        });

        listeners.init(createKey(CameraKey.KeyCameraFlatModeRange), (oldValue, newValue) -> {
            final List<String> range = new ArrayList<>();
            if (newValue != null) {
                for (final dji.sdk.keyvalue.value.camera.CameraFlatMode value : newValue) {
                    range.add(Kernel.enumRawValue(DronelinkDJI2.getCameraPhotoMode(value)));
                }
            }
            enumElements.update("CameraPhotoMode", range);
        });

        listeners.init(createKey(CameraKey.KeyPhotoFileFormatRange), (oldValue, newValue) -> {
            final List<String> range = new ArrayList<>();
            if (newValue != null) {
                for (final dji.sdk.keyvalue.value.camera.PhotoFileFormat value : newValue) {
                    range.add(Kernel.enumRawValue(DronelinkDJI2.getCameraPhotoFileFormat(value)));
                }
            }
            enumElements.update("CameraPhotoFileFormat", range);
        });

        listeners.init(createKey(CameraKey.KeyCameraModeRange), (oldValue, newValue) -> {
            final List<String> range = new ArrayList<>();
            if (newValue != null) {
                for (final dji.sdk.keyvalue.value.camera.CameraMode value : newValue) {
                    range.add(Kernel.enumRawValue(DronelinkDJI2.getCameraMode(value)));
                }
            }
            enumElements.update("CameraMode", range);
        });

        listeners.init(createLensKey(CameraKey.KeyShutterSpeedRange), (oldValue, newValue) -> {
            final List<String> range = new ArrayList<>();
            if (newValue != null) {
                for (final dji.sdk.keyvalue.value.camera.CameraShutterSpeed value : newValue) {
                    range.add(Kernel.enumRawValue(DronelinkDJI2.getCameraShutterSpeed(value)));
                }
            }
            enumElements.update("CameraShutterSpeed", range);
        });

        listeners.init(createKey(CameraKey.KeyIsInternalStorageSupported), (oldValue, newValue) -> {
            final List<String> range = new ArrayList<>();
            range.add(Kernel.enumRawValue(CameraStorageLocation.SD_CARD));
            if (newValue != null && newValue) {
                range.add(Kernel.enumRawValue(CameraStorageLocation.INTERNAL));
            }
            enumElements.update("CameraStorageLocation", range);
        });

        listeners.init(createKey(CameraKey.KeyVideoFileFormatRange), (oldValue, newValue) -> {
            final List<String> range = new ArrayList<>();
            if (newValue != null) {
                for (final dji.sdk.keyvalue.value.camera.VideoFileFormat value : newValue) {
                    range.add(Kernel.enumRawValue(DronelinkDJI2.getCameraVideoFileFormat(value)));
                }
            }
            enumElements.update("CameraVideoFileFormat", range);
        });

        listeners.init(createLensKey(CameraKey.KeyCameraWhiteBalanceRange), (oldValue, newValue) -> {
            final List<String> range = new ArrayList<>();
            if (newValue != null) {
                for (final dji.sdk.keyvalue.value.camera.CameraWhiteBalanceMode value : newValue) {
                    range.add(Kernel.enumRawValue(DronelinkDJI2.getCameraWhiteBalancePreset(value)));
                }
            }
            enumElements.update("CameraWhiteBalancePreset", range);
        });

        listeners.init(createKey(CameraKey.KeyIsShootingPhoto), (oldValue, newValue) -> {
            isShootingPhoto = newValue != null && newValue;
            isShootingPhotoUpdated = new Date();
        });
        listeners.init(createKey(CameraKey.KeyPhotoIntervalCountdown), (oldValue, newValue) -> photoIntervalCountdown = newValue);
        listeners.init(createKey(CameraKey.KeyIsShootingBurstPhoto), (oldValue, newValue) -> isShootingBurstPhoto = newValue != null && newValue);
        listeners.init(createKey(CameraKey.KeyIsShootingHyperlapse), (oldValue, newValue) -> isShootingHyperlapse = newValue != null && newValue);
        listeners.init(createKey(CameraKey.KeyIsShootingPhotoPanorama), (oldValue, newValue) -> isShootingPhotoPanorama = newValue != null && newValue);
        listeners.init(createKey(CameraKey.KeyIsShootingRAWBurstPhoto), (oldValue, newValue) -> isShootingRAWBurstPhoto = newValue != null && newValue);
        listeners.init(createKey(CameraKey.KeyIsShootingSinglePhoto), (oldValue, newValue) -> isShootingSinglePhoto = newValue != null && newValue);
        listeners.init(createKey(CameraKey.KeyIsShootingSinglePhotoInRAWFormat), (oldValue, newValue) -> isShootingSinglePhotoInRAWFormat = newValue != null && newValue);
        listeners.init(createKey(CameraKey.KeyIsShootingSuperResolutionPhoto), (oldValue, newValue) -> isShootingSuperResolutionPhoto = newValue != null && newValue);
        listeners.init(createKey(CameraKey.KeyIsShootingVisionBokehPhoto), (oldValue, newValue) -> isShootingVisionBokehPhoto = newValue != null && newValue);
        listeners.init(createKey(CameraKey.KeyIsRecording), (oldValue, newValue) -> isRecording = newValue != null && newValue);
        listeners.init(createKey(CameraKey.KeyCameraFlatMode), (oldValue, newValue) -> flatMode = newValue);
        listeners.init(createKey(CameraKey.KeyCameraSDCardInserted), (oldValue, newValue) -> isSDCardInserted = newValue != null && newValue);
        listeners.init(createKey(CameraKey.KeyCameraStorageLocation), (oldValue, newValue) -> storageLocation = newValue);
        listeners.init(createKey(CameraKey.KeySDCardRemainSpace), (oldValue, newValue) -> remainingSpaceSDCard = newValue);
        listeners.init(createKey(CameraKey.KeySDCardTotalSpace), (oldValue, newValue) -> totalSpaceSDCard = newValue);
        listeners.init(createKey(CameraKey.KeySSDRemainingSpaceInMB), (oldValue, newValue) -> remainingSpaceSSD = newValue);
        listeners.init(createKey(CameraKey.KeySSDTotalSpace), (oldValue, newValue) -> totalSpaceSSD = newValue);
        listeners.init(createKey(CameraKey.KeyInternalStorageRemainSpace), (oldValue, newValue) -> remainingSpaceInternalStorage = newValue);
        listeners.init(createKey(CameraKey.KeyInternalStorageTotalSpace), (oldValue, newValue) -> totalSpaceInternalStorage = newValue);
        listeners.init(createKey(CameraKey.KeySDCardAvailablePhotoCount), (oldValue, newValue) -> availablePhotoCountSDCard = newValue);
        listeners.init(createKey(CameraKey.KeyInternalSSDAvailablePhotoCount), (oldValue, newValue) -> availablePhotoCountSSD = newValue);
        listeners.init(createKey(CameraKey.KeyInternalStorageAvailablePhotoCount), (oldValue, newValue) -> availablePhotoCountInternalStorage = newValue);
        listeners.init(createKey(CameraKey.KeyCameraVideoStreamSource), (oldValue, newValue) -> videoStreamSource = newValue);
        listeners.init(createLensKey(CameraKey.KeyCameraColor), (oldValue, newValue) -> color = newValue);
        listeners.init(createLensKey(CameraKey.KeyContrast), (oldValue, newValue) -> contrast = newValue);
        listeners.init(createLensKey(CameraKey.KeySaturation), (oldValue, newValue) -> saturation = newValue);
        listeners.init(createLensKey(CameraKey.KeySharpness), (oldValue, newValue) -> sharpness = newValue);
        listeners.init(createLensKey(CameraKey.KeyThermalDisplayMode), (oldValue, newValue) -> displayMode = newValue);
        listeners.init(createLensKey(CameraKey.KeyPhotoFileFormat), (oldValue, newValue) -> photoFileFormat = newValue);
        listeners.init(createLensKey(CameraKey.KeyPhotoIntervalShootSettings), (oldValue, newValue) -> photoIntervalShootSettings = newValue);
        listeners.init(createLensKey(CameraKey.KeyPhotoBurstCount), (oldValue, newValue) -> photoBurstCount = newValue);
        listeners.init(createLensKey(CameraKey.KeyAEBSettings), (oldValue, newValue) -> photoAEBSettings = newValue);
        listeners.init(createLensKey(CameraKey.KeyCameraVideoCaptionEnabled), (oldValue, newValue) -> videoCaptionEnabled = newValue);
        listeners.init(createLensKey(CameraKey.KeyVideoFileCompressionStandard), (oldValue, newValue) -> videoFileCompressionStandard = newValue);
        listeners.init(createLensKey(CameraKey.KeyVideoFileFormat), (oldValue, newValue) -> videoFileFormat = newValue);
        listeners.init(createLensKey(CameraKey.KeyVideoRecordMode), (oldValue, newValue) -> videoRecordMode = newValue);
        listeners.init(createLensKey(CameraKey.KeyVideoResolutionFrameRateAndFov), (oldValue, newValue) -> videoResolutionFrameRateFov = newValue);
        listeners.init(createLensKey(CameraKey.KeyRecordingTime), (oldValue, newValue) -> recordingTime = newValue);
        listeners.init(createLensKey(CameraKey.KeyVideoStandard), (oldValue, newValue) -> videoStandard = newValue);
        listeners.init(createLensKey(CameraKey.KeyExposureMode), (oldValue, newValue) -> exposureMode = newValue);
        listeners.init(createLensKey(CameraKey.KeyExposureSettings), (oldValue, newValue) -> exposureSettings = newValue);
        listeners.init(createLensKey(CameraKey.KeyExposureCompensation), (oldValue, newValue) -> exposureCompensation = newValue);
        listeners.init(createLensKey(CameraKey.KeyCameraFileIndexMode), (oldValue, newValue) -> fileIndexMode = newValue);
        listeners.init(createLensKey(CameraKey.KeyISO), (oldValue, newValue) -> iso = newValue);
        listeners.init(createLensKey(CameraKey.KeyShutterSpeed), (oldValue, newValue) -> shutterSpeed = newValue);
        listeners.init(createLensKey(CameraKey.KeyAperture), (oldValue, newValue) -> aperture = newValue);
        listeners.init(createLensKey(CameraKey.KeyWhiteBalance), (oldValue, newValue) -> whiteBalance = newValue);
        listeners.init(createLensKey(CameraKey.KeyHistogramData), (oldValue, newValue) -> {
            if (newValue == null) {
                histogram = null;
                return;
            }

            histogram = new short[newValue.size()];
            for (int i = 0; i < histogram.length; i++) {
                histogram[i] = newValue.get(i).shortValue();
            }
        });
        listeners.init(createLensKey(CameraKey.KeyCameraFocusMode), (oldValue, newValue) -> focusMode = newValue);
        listeners.init(createLensKey(CameraKey.KeyCameraFocusRingValue), (oldValue, newValue) -> focusRingValue = newValue);
        listeners.init(createLensKey(CameraKey.KeyCameraFocusRingMaxValue), (oldValue, newValue) -> focusRingMax = newValue);
        listeners.init(createLensKey(CameraKey.KeyCameraMeteringMode), (oldValue, newValue) -> meteringMode = newValue);
        listeners.init(createLensKey(CameraKey.KeyAELockEnabled), (oldValue, newValue) -> aeLockEnabled = newValue != null && newValue);
        listeners.init(createLensKey(CameraKey.KeyPhotoRatio), (oldValue, newValue) -> photoRatio = newValue);
        listeners.init(createLensKey(CameraKey.KeyLockGimbalDuringShootPhotoEnabled), (oldValue, newValue) -> lockGimbalDuringShootPhotoEnabled = newValue != null && newValue);
        listeners.init(createLensKey(CameraKey.KeyMechanicalShutterEnabled), (oldValue, newValue) -> mechanicalShutterEnabled = newValue != null && newValue);
        listeners.init(createLensKey(CameraKey.KeyDewarpingEnabled), (oldValue, newValue) -> dewarpingEnabled = newValue != null && newValue);
        listeners.init(createLensKey(CameraKey.KeyCameraHybridZoomSupported), (oldValue, newValue) -> isHybridZoomSupported = newValue != null && newValue);
        listeners.init(createLensKey(CameraKey.KeyCameraHybridZoomFocalLength), (oldValue, newValue) -> hybridZoomFocalLength = newValue);
        listeners.init(createLensKey(CameraKey.KeyCameraHybridZoomSpec), (oldValue, newValue) -> hybridZoomSpecification = newValue);
        listeners.init(createLensKey(CameraKey.KeyThermalZoomRatios), (oldValue, newValue) -> currentThermalZoomRatio = newValue);
        listeners.init(createLensKey(CameraKey.KeyCameraZoomRatios), (oldValue, newValue) -> currentZoomRatio = newValue);
        listeners.init(createLensKey(CameraKey.KeyCameraZoomRatiosRange), (oldValue, newValue) -> {
            if (newValue != null) {
                zoomRatios = newValue.getGears();
            } else {
                zoomRatios = null;
            }
        });
        listeners.init(createLensKey(CameraKey.KeyThermalZoomRatiosRange), (oldValue, newValue) -> {
            if (newValue != null) {
                thermalZoomRatios = newValue.getGears();
            } else {
                thermalZoomRatios = null;
            }
        });
    }

    private <T> DJIKey<T> createKey(final DJIKeyInfo<T> keyInfo) {
        return KeyTools.createKey(keyInfo, index);
    }

    private <T> DJIKey<T> createLensKey(final DJIKeyInfo<T> keyInfo) {
        return KeyTools.createCameraKey(keyInfo, index, lensType);
    }

    public void close() {
        listeners.cancelAll();
    }

    public DatedValue<CameraStateAdapter> asDatedValue() {
        return new DatedValue<>(this, new Date());
    }

    public List<Message> getStatusMessages() {
        final List<Message> messages = new ArrayList<>();

        final Long storageRemainingSpace = getStorageRemainingSpace();
        if (storageRemainingSpace != null) {
            final String storageName = Dronelink.getInstance().formatEnum("CameraStorageLocation", Kernel.enumRawValue(getStorageLocation()), "");
            int percentFull = 0;
            final Long storageTotalSpace = getStorageTotalSpace();
            if (storageTotalSpace != null && storageTotalSpace > 0) {
                percentFull = 100 - (int)(Math.min(1.0, (storageRemainingSpace.doubleValue() / storageTotalSpace.doubleValue())) * 100.0);
            }

            if (storageRemainingSpace == 0 || percentFull == 100) {
                messages.add(new Message(context.getString(R.string.DJI2CameraStateAdapter_statusMessages_storage_remaining_space_none_title, storageName), Message.Level.WARNING));
            } else if (percentFull >= 90) {
                messages.add(new Message(context.getString(R.string.DJI2CameraStateAdapter_statusMessages_storage_remaining_space_low_title, storageName, percentFull), Message.Level.WARNING));
            }
        }

        return messages;
    }

    @Override
    public boolean isBusy() {
        return isShootingPhoto
                || isShootingBurstPhoto
                || isShootingHyperlapse
                || isShootingPhotoPanorama
                || isShootingRAWBurstPhoto
                || isShootingSinglePhoto
                || isShootingSinglePhotoInRAWFormat
                || isShootingSuperResolutionPhoto
                || isShootingVisionBokehPhoto;
    }

    @Override
    public boolean isCapturing() {
        return isRecording
                || isShootingPhoto
                || isShootingBurstPhoto
                || isShootingHyperlapse
                || isShootingPhotoPanorama
                || isShootingRAWBurstPhoto
                || isShootingSinglePhoto
                || isShootingSinglePhotoInRAWFormat
                || isShootingSuperResolutionPhoto
                || isShootingVisionBokehPhoto;
    }

    @Override
    public boolean isCapturingPhotoInterval() {
        final Integer photoIntervalCountdown = this.photoIntervalCountdown;
        if (photoIntervalCountdown != null && photoIntervalCountdown > 0) {
            return true;
        }

        if (getMode() == CameraMode.PHOTO && getPhotoMode() == CameraPhotoMode.INTERVAL) {
            final Double photoInterval = getPhotoInterval();
            //0.7s doesn't work with photoIntervalCountdown
            if (photoInterval != null && photoInterval < 1 && (System.currentTimeMillis() - isShootingPhotoUpdated.getTime()) < photoInterval * 1000) {
                return true;
            }
            return isShootingPhoto;
        }

        return false;
    }

    @Override
    public boolean isCapturingVideo() {
        return isRecording;
    }

    @Override
    public boolean isCapturingContinuous() {
        return isCapturingVideo() || isCapturingPhotoInterval();
    }

    @Override
    public boolean isSDCardInserted() {
        return isSDCardInserted;
    }

    @Override
    public CameraVideoStreamSource getVideoStreamSource() {
        return DronelinkDJI2.getCameraVideoStreamSource(videoStreamSource);
    }

    @Override
    public CameraStorageLocation getStorageLocation() {
        return DronelinkDJI2.getCameraStorageLocation(storageLocation);
    }

    @Override
    public Long getStorageRemainingSpace() {
        Integer value = null;
        switch (getStorageLocation()) {
            case SD_CARD:
                value = remainingSpaceSDCard;
                break;
            case INTERNAL:
                value = remainingSpaceInternalStorage;
                break;
            case INTERNAL_SSD:
                value = remainingSpaceSSD;
                break;
            case UNKNOWN:
                break;
        }

        if (value == null) {
            return null;
        }

        return value.longValue() * 1024 * 1014;
    }

    public Long getStorageTotalSpace() {
        Integer value = null;
        switch (getStorageLocation()) {
            case SD_CARD:
                value = totalSpaceSDCard;
                break;
            case INTERNAL:
                value = totalSpaceInternalStorage;
                break;
            case INTERNAL_SSD:
                final SSDTotalSpace totalSpace = totalSpaceSSD;
                if (totalSpace != null) {
                    switch (totalSpaceSSD) {
                        case SPACE_256GB:
                            value = 256 * 1000;
                            break;

                        case SPACE_512GB:
                            value = 512 * 1000;
                            break;

                        case SPACE_1TB:
                            value = 1024 * 1000;
                            break;

                        case UNKNOWN:
                            break;
                    }
                }
                break;
            case UNKNOWN:
                break;
        }

        if (value == null) {
            return null;
        }

        return value.longValue() * 1024 * 1014;
    }

    @Override
    public Long getStorageRemainingPhotos() {
        Integer value = null;
        switch (getStorageLocation()) {
            case SD_CARD:
                value = availablePhotoCountSDCard;
                break;
            case INTERNAL:
                value = availablePhotoCountInternalStorage;
                break;
            case INTERNAL_SSD:
                value = availablePhotoCountSSD;
                break;
            case UNKNOWN:
                break;
        }

        if (value == null) {
            return null;
        }

        return value.longValue();
    }

    @Override
    public CameraMode getMode() {
        return DronelinkDJI2.getCameraMode(flatMode);
    }

    @Override
    public CameraPhotoMode getPhotoMode() {
        return DronelinkDJI2.getCameraPhotoMode(flatMode);
    }

    @Override
    public CameraPhotoFileFormat getPhotoFileFormat() {
        return DronelinkDJI2.getCameraPhotoFileFormat(photoFileFormat);
    }

    @Override
    public Double getPhotoInterval() {
        final PhotoIntervalShootSettings settings = photoIntervalShootSettings;
        if (settings != null) {
            return photoIntervalShootSettings.interval;
        }
        return null;
    }

    @Override
    public CameraBurstCount getBurstCount() {
        return DronelinkDJI2.getCameraPhotoBurstCount(photoBurstCount);
    }

    @Override
    public CameraAEBCount getAEBCount() {
        return DronelinkDJI2.getCameraPhotoAEBCount(photoAEBSettings == null ? null : photoAEBSettings.count);
    }

    @Override
    public CameraVideoFileFormat getVideoFileFormat() {
        return DronelinkDJI2.getCameraVideoFileFormat(videoFileFormat);
    }

    public CameraVideoFieldOfView getVideoFieldOfView() {
        return DronelinkDJI2.getCameraVideoFieldOfView(videoResolutionFrameRateFov);
    }

    @Override
    public CameraVideoFrameRate getVideoFrameRate() {
        return DronelinkDJI2.getCameraVideoFrameRate(videoResolutionFrameRateFov);
    }

    @Override
    public CameraVideoResolution getVideoResolution() {
        return DronelinkDJI2.getCameraVideoResolution(videoResolutionFrameRateFov);
    }

    @Override
    public Double getCurrentVideoTime() {
        final Integer recordingTime = this.recordingTime;
        if (recordingTime != null) {
            return recordingTime.doubleValue();
        }
        return null;
    }

    @Override
    public CameraExposureMode getExposureMode() {
        return DronelinkDJI2.getCameraExposureMode(exposureMode);
    }

    @Override
    public CameraExposureCompensation getExposureCompensation() {
        return DronelinkDJI2.getCameraExposureCompensation(exposureCompensation);
    }

    @Override
    public CameraISO getISO() {
        return DronelinkDJI2.getCameraISO(iso);
    }

    @Override
    public Integer getISOActual() {
        final CameraExposureSettings settings = exposureSettings;
        if (settings != null) {
            return settings.iso;
        }
        return null;
    }

    @Override
    public CameraShutterSpeed getShutterSpeed() {
        return DronelinkDJI2.getCameraShutterSpeed(shutterSpeed);
    }

    @Override
    public CameraShutterSpeed getShutterSpeedActual() {
        final CameraExposureSettings settings = exposureSettings;
        if (settings != null) {
            return DronelinkDJI2.getCameraShutterSpeed(settings.shutterSpeed);
        }
        return null;
    }

    @Override
    public CameraAperture getAperture() {
        return DronelinkDJI2.getCameraAperture(aperture);
    }

    @Override
    public CameraAperture getApertureActual() {
        final CameraExposureSettings settings = exposureSettings;
        if (settings != null) {
            return DronelinkDJI2.getCameraAperture(settings.aperture);
        }
        return null;
    }

    @Override
    public CameraWhiteBalancePreset getWhiteBalancePreset() {
        final CameraWhiteBalanceInfo whiteBalance = this.whiteBalance;
        if (whiteBalance != null) {
            return DronelinkDJI2.getCameraWhiteBalancePreset(whiteBalance.whiteBalanceMode);
        }
        return null;
    }

    @Override
    public Integer getWhiteBalanceColorTemperature() {
        final CameraWhiteBalanceInfo whiteBalance = this.whiteBalance;
        if (whiteBalance != null) {
            return whiteBalance.colorTemperature;
        }
        return null;
    }

    @Override
    public short[] getHistogram() {
        return histogram;
    }

    @Override
    public String getLensDetails() {
        return DronelinkDJI2.getString(context, lensType);
    }

    @Override
    public CameraFocusMode getFocusMode() {
        return DronelinkDJI2.getCameraFocusMode(focusMode);
    }

    @Override
    public Double getFocusRingValue() {
        final Integer value = focusRingValue;
        if (value != null) {
            return value.doubleValue();
        }
        return null;
    }

    @Override
    public Double getFocusRingMax() {
        final Integer value = focusRingMax;
        if (value != null) {
            return value.doubleValue();
        }
        return null;
    }

    @Override
    public boolean isPercentZoomSupported() {
        return lensType == CameraLensType.CAMERA_LENS_ZOOM && isHybridZoomSupported;
    }

    @Override
    public boolean isRatioZoomSupported() {
        return lensType == CameraLensType.CAMERA_LENS_THERMAL || (lensType == CameraLensType.CAMERA_LENS_ZOOM && !isHybridZoomSupported);
    }

    @Override
    public CameraZoomSpecification getDefaultZoomSpecification() {
        return isPercentZoomSupported() ? getPercentZoomSpecification() : isRatioZoomSupported() ? getRatioZoomSpecification() : null;
    }

    public RatioZoomSpecification getRatioZoomSpecification() {
        final Double currentZoomRatio = lensType == CameraLensType.CAMERA_LENS_THERMAL ? this.currentThermalZoomRatio : this.currentZoomRatio;
        final int[] zoomRatios = lensType == CameraLensType.CAMERA_LENS_THERMAL ? this.thermalZoomRatios : this.zoomRatios;
        if (!isRatioZoomSupported() || currentZoomRatio == null || zoomRatios == null) {
            return null;
        }

        try {
           return new RatioZoomSpecification(currentZoomRatio, DJI2CameraStateAdapter.zoomRatiosResolved(zoomRatios));
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, "Error initializing ratio zoom spec: " + e.getMessage());
        }
        return null;
    }

    public PercentZoomSpecification getPercentZoomSpecification() {
        final Integer zoomValue = this.hybridZoomFocalLength;
        final CameraHybridZoomSpec specification = this.hybridZoomSpecification;
        final int[] zoomRatios = this.zoomRatios;
        if (!isPercentZoomSupported() || zoomValue == null || specification == null || zoomRatios == null) {
            return null;
        }

        try {
            //Sometimes DJI SDK return a focal length step of 0 incorrectly, in those cases we hard code it to 10 so that a valid zoom specification is created.
            final int step = specification.focalLengthStep == 0 ? 10 : specification.focalLengthStep;
            return new PercentZoomSpecification(zoomValue.doubleValue(), specification.minFocalLength, specification.maxFocalLength,
                    specification.maxOpticalFocalLength, step, DJI2CameraStateAdapter.zoomRatiosResolved(zoomRatios));
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, "Error initializing percent zoom specification: " + e.getMessage());
        }
        return null;
    }

    /*TODO: As of 7/19/23, We are on an older version of java API that does not allow streams for 1 liner conversion between int[] and double[]
       so we have to do it the old fashioned way. Error is "Call requires API level 24 (current min is 21):"*/
    private static double[] zoomRatiosResolved(final int[] zoomRatios) {
        final double[] zoomRatiosResolved = new double[zoomRatios.length];
        for (int i = 0; i < zoomRatios.length; i++) {
            zoomRatiosResolved[i] = zoomRatios[i];
        }
        return zoomRatiosResolved;
    }

    @Override
    public CameraMeteringMode getMeteringMode() {
        return DronelinkDJI2.getCameraMeteringMode(meteringMode);
    }

    @Override
    public boolean isAutoExposureLockEnabled() {
        return aeLockEnabled;
    }

    @Override
    public CameraPhotoAspectRatio getAspectRatio() {
        return getMode() == CameraMode.PHOTO ? DronelinkDJI2.getCameraPhotoAspectRatio(photoRatio) : CameraPhotoAspectRatio._16_9;
    }

    public List<EnumElement> getEnumElements(final String parameter) {
        return enumElements.get(parameter);
    }

    public CommandError executeCommand(final Context context, final CameraCommand command, final Command.Finisher finished) {
        if (command instanceof AEBCountCameraCommand) {
            final CameraAEBCount target = ((AEBCountCameraCommand) command).aebCount;
            Command.conditionallyExecute(target != getAEBCount(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyAEBSettings),
                    new PhotoAEBSettings(DronelinkDJI2.getCameraPhotoAEBCount(target), PhotoAEBExposureOffset.OFFSET_0EV),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof ApertureCameraCommand) {
            final CameraAperture target = ((ApertureCameraCommand) command).aperture;
            Command.conditionallyExecute(target != getAperture(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyAperture),
                    DronelinkDJI2.getCameraAperture(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof AutoExposureLockCameraCommand) {
            final boolean target = ((AutoExposureLockCameraCommand) command).enabled;
            Command.conditionallyExecute(target != isAutoExposureLockEnabled(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyAELockEnabled),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof AutoLockGimbalCameraCommand) {
            final boolean target = ((AutoLockGimbalCameraCommand) command).enabled;
            Command.conditionallyExecute(target != lockGimbalDuringShootPhotoEnabled, finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyLockGimbalDuringShootPhotoEnabled),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof ColorCameraCommand) {
            final CameraColor target = ((ColorCameraCommand) command).color;
            Command.conditionallyExecute(target != DronelinkDJI2.getCameraColor(color), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyCameraColor),
                    DronelinkDJI2.getCameraColor(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
        }

        if (command instanceof ContrastCameraCommand) {
            final int target = ((ContrastCameraCommand) command).contrast;
            Command.conditionallyExecute(contrast == null || target != contrast, finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyContrast),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof DewarpingCameraCommand) {
            final boolean target = ((DewarpingCameraCommand) command).enabled;
            Command.conditionallyExecute(target != dewarpingEnabled, finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyDewarpingEnabled),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof DisplayModeCameraCommand) {
            final CameraDisplayMode target = ((DisplayModeCameraCommand) command).displayMode;
            Command.conditionallyExecute(target != DronelinkDJI2.getCameraDisplayMode(displayMode), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyThermalDisplayMode),
                    DronelinkDJI2.getCameraDisplayMode(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof ExposureCompensationCameraCommand) {
            final CameraExposureCompensation target = ((ExposureCompensationCameraCommand) command).exposureCompensation;
            Command.conditionallyExecute(target != getExposureCompensation(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyExposureCompensation),
                    DronelinkDJI2.getCameraExposureCompensation(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof ExposureCompensationStepCameraCommand) {
            KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyExposureCompensation),
                    DronelinkDJI2.getCameraExposureCompensation(getExposureCompensation().offset(((ExposureCompensationStepCameraCommand) command).exposureCompensationSteps)),
                    DronelinkDJI2.createCompletionCallback(finished));
            return null;
        }

        if (command instanceof ExposureModeCameraCommand) {
            final CameraExposureMode target = ((ExposureModeCameraCommand) command).exposureMode;
            Command.conditionallyExecute(target != getExposureMode(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyExposureMode),
                    DronelinkDJI2.getCameraExposureMode(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof FileIndexModeCameraCommand) {
            final CameraFileIndexMode target = ((FileIndexModeCameraCommand) command).fileIndexMode;
            Command.conditionallyExecute(target != DronelinkDJI2.getCameraFileIndexMode(fileIndexMode), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyCameraFileIndexMode),
                    DronelinkDJI2.getCameraFileIndexMode(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof FocusCameraCommand) {
            final Point2 target = ((FocusCameraCommand) command).focusTarget;
            KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyCameraFocusTarget),
                    new DoublePoint2D(target.x, target.y),
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            if (finished != null) {
                                commandFinishFocusTargetVerifyRing(context, (FocusCameraCommand) command, finished);
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

        if (command instanceof FocusDistanceCameraCommand) {
            final FocusDistanceCameraCommand focusDistanceCameraCommand = (FocusDistanceCameraCommand)command;
            final CameraFocusCalibration cameraFocusCalibration = Dronelink.getInstance().getCameraFocusCalibration(focusDistanceCameraCommand.focusCalibration.withDroneSerialNumber(drone.serialNumber));
            if (cameraFocusCalibration == null) {
                return new CommandError(context.getString(R.string.DJI2CameraStateAdapter_cameraCommand_focus_distance_error) + ": " + (int)focusDistanceCameraCommand.focusCalibration.distance);
            }
            KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyCameraFocusRingValue),
                    cameraFocusCalibration.ringValue.intValue(),
                    DronelinkDJI2.createCompletionCallback(finished));
            return null;
        }

        if (command instanceof FocusModeCameraCommand) {
            final CameraFocusMode target = ((FocusModeCameraCommand) command).focusMode;
            Command.conditionallyExecute(target != getFocusMode(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyCameraFocusMode),
                    DronelinkDJI2.getCameraFocusMode(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof FocusRingCameraCommand) {
            final Integer focusRingMax = this.focusRingMax;
            KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyCameraFocusRingValue),
                    (int)(((FocusRingCameraCommand)command).focusRingPercent * (focusRingMax == null ? 0 : focusRingMax)),
                    DronelinkDJI2.createCompletionCallback(finished));
            return null;
        }

        if (command instanceof ZoomPercentCameraCommand) {
            final PercentZoomSpecification specification = getPercentZoomSpecification();
            if (specification == null) {
                return new CommandError(context.getString(R.string.MissionDisengageReason_command_type_unsupported_lens));
            }
            final int hybridZoomFocalLength = (int) (Math.round((((ZoomPercentCameraCommand) command).zoomPercent
                    * (specification.max - specification.min) + specification.min) / specification.step) * specification.step);
            Command.conditionallyExecute(Math.abs(hybridZoomFocalLength - specification.currentZoom) >= 0.1, finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyCameraHybridZoomFocalLength),
                    hybridZoomFocalLength,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof ZoomRatioCameraCommand) {
            final RatioZoomSpecification specification = getRatioZoomSpecification();
            final DJIKeyInfo<Double> cameraKey = lensType == CameraLensType.CAMERA_LENS_THERMAL ? CameraKey.KeyThermalZoomRatios
                    : lensType == CameraLensType.CAMERA_LENS_ZOOM ? CameraKey.KeyCameraZoomRatios
                    : null;
            if (specification == null || cameraKey == null) {
                return new CommandError(context.getString(R.string.MissionDisengageReason_command_type_unsupported_lens));
            }
            final double zoomRatio = ((ZoomRatioCameraCommand) command).zoomRatio;
            if (!(RatioZoomSpecification.containsElement(specification.ratios, zoomRatio))) {
                return new CommandError(context.getString(R.string.MissionDisengageReason_zoom_ratio_unsupported));
            }

            Command.conditionallyExecute(Math.abs(specification.currentRatio - zoomRatio) >= 0.1,
                    finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(cameraKey),
                    zoomRatio,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof ISOCameraCommand) {
            final CameraISO target = ((ISOCameraCommand) command).iso;
            Command.conditionallyExecute(target != getISO(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyISO),
                    DronelinkDJI2.getCameraISO(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof MechanicalShutterCameraCommand) {
            final boolean target = ((MechanicalShutterCameraCommand) command).enabled;
            Command.conditionallyExecute(target != mechanicalShutterEnabled, finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyMechanicalShutterEnabled),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof MeteringModeCameraCommand) {
            final CameraMeteringMode target = ((MeteringModeCameraCommand) command).meteringMode;
            Command.conditionallyExecute(target != getMeteringMode(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyCameraMeteringMode),
                    DronelinkDJI2.getCameraMeteringMode(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof PhotoAspectRatioCameraCommand) {
            final CameraPhotoAspectRatio target = ((PhotoAspectRatioCameraCommand) command).photoAspectRatio;
            Command.conditionallyExecute(target != getAspectRatio(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyPhotoRatio),
                    DronelinkDJI2.getCameraPhotoAspectRatio(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof PhotoFileFormatCameraCommand) {
            final CameraPhotoFileFormat target = ((PhotoFileFormatCameraCommand) command).photoFileFormat;
            Command.conditionallyExecute(target != getPhotoFileFormat(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyPhotoFileFormat),
                    DronelinkDJI2.getCameraPhotoFileFormat(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof PhotoIntervalCameraCommand) {
            final double target = ((PhotoIntervalCameraCommand) command).photoInterval;
            Command.conditionallyExecute(target != getPhotoInterval(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyPhotoIntervalShootSettings),
                    new PhotoIntervalShootSettings(255, (double)target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof SaturationCameraCommand) {
            final int target = ((SaturationCameraCommand) command).saturation;
            Command.conditionallyExecute(saturation == null || target != saturation, finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeySaturation),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof SharpnessCameraCommand) {
            final int target = ((SharpnessCameraCommand) command).sharpness;
            Command.conditionallyExecute(sharpness == null || target != sharpness, finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeySharpness),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof ShutterSpeedCameraCommand) {
            final CameraShutterSpeed target = ((ShutterSpeedCameraCommand) command).shutterSpeed;
            Command.conditionallyExecute(target != getShutterSpeed(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyShutterSpeed),
                    DronelinkDJI2.getCameraShutterSpeed(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof SpotMeteringTargetCameraCommand) {
            final Point2 spotMeteringTarget = ((SpotMeteringTargetCameraCommand) command).spotMeteringTarget;
            final DoubleRect target = new DoubleRect();
            target.setX(spotMeteringTarget.x);
            target.setY(spotMeteringTarget.y);
            KeyManager.getInstance().setValue(createLensKey(CameraKey.KeySpotMeteringTargetArea), target, DronelinkDJI2.createCompletionCallback(finished));
            return null;
        }

        if (command instanceof VideoCaptionCameraCommand) {
            final boolean target = ((VideoCaptionCameraCommand) command).enabled;
            Command.conditionallyExecute(target != videoCaptionEnabled, finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyCameraVideoCaptionEnabled),
                    target,
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof VideoFileCompressionStandardCameraCommand) {
            final CameraVideoFileCompressionStandard target = ((VideoFileCompressionStandardCameraCommand) command).videoFileCompressionStandard;
            Command.conditionallyExecute(target != DronelinkDJI2.getCameraVideoFileCompressionStandard(videoFileCompressionStandard), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyVideoFileCompressionStandard),
                    DronelinkDJI2.getCameraVideoFileCompressionStandard(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof VideoFileFormatCameraCommand) {
            final CameraVideoFileFormat target = ((VideoFileFormatCameraCommand) command).videoFileFormat;
            Command.conditionallyExecute(target != getVideoFileFormat(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyVideoFileFormat),
                    DronelinkDJI2.getCameraVideoFileFormat(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof VideoModeCameraCommand) {
            final CameraVideoMode target = ((VideoModeCameraCommand) command).videoMode;
            Command.conditionallyExecute(target != DronelinkDJI2.getCameraVideoMode(videoRecordMode), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyVideoRecordMode),
                    DronelinkDJI2.getCameraVideoMode(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof VideoResolutionFrameRateCameraCommand) {
            final CameraVideoResolution videoResolution = ((VideoResolutionFrameRateCameraCommand) command).videoResolution;
            final CameraVideoFrameRate videoFrameRate = ((VideoResolutionFrameRateCameraCommand) command).videoFrameRate;
            final CameraVideoFieldOfView videoFieldOfView = ((VideoResolutionFrameRateCameraCommand) command).videoFieldOfView;
            Command.conditionallyExecute(videoResolution != getVideoResolution() || videoFrameRate != getVideoFrameRate() || videoFieldOfView != getVideoFieldOfView(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyVideoResolutionFrameRateAndFov),
                    new VideoResolutionFrameRateAndFov(
                            new VideoResolutionFrameRate(DronelinkDJI2.getCameraVideoResolution(videoResolution), DronelinkDJI2.getCameraVideoFrameRate(videoFrameRate)),
                            DronelinkDJI2.getCameraVideoFieldOfView(videoFieldOfView)),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof VideoStandardCameraCommand) {
            final CameraVideoStandard target = ((VideoStandardCameraCommand) command).videoStandard;
            Command.conditionallyExecute(target != DronelinkDJI2.getCameraVideoStandard(videoStandard), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyVideoStandard),
                    DronelinkDJI2.getCameraVideoStandard(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof VideoStreamSourceCameraCommand) {
            final CameraVideoStreamSource target = ((VideoStreamSourceCameraCommand) command).videoStreamSource;
            Command.conditionallyExecute(target != DronelinkDJI2.getCameraVideoStreamSource(videoStreamSource), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyCameraVideoStreamSource),
                    DronelinkDJI2.getCameraVideoStreamSource(target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof WhiteBalanceCustomCameraCommand) {
            final int target = ((WhiteBalanceCustomCameraCommand) command).whiteBalanceCustom;
            final CameraWhiteBalanceInfo whiteBalance = this.whiteBalance;
            final Integer current = whiteBalance == null ? null : whiteBalance.colorTemperature;
            Command.conditionallyExecute(getWhiteBalancePreset() != CameraWhiteBalancePreset.CUSTOM || current == null || current != target, finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyWhiteBalance),
                    new CameraWhiteBalanceInfo(CameraWhiteBalanceMode.MANUAL, target),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        if (command instanceof WhiteBalancePresetCameraCommand) {
            final CameraWhiteBalancePreset target = ((WhiteBalancePresetCameraCommand) command).whiteBalancePreset;
            Command.conditionallyExecute(target != getWhiteBalancePreset(), finished, () -> KeyManager.getInstance().setValue(
                    createLensKey(CameraKey.KeyWhiteBalance),
                    new CameraWhiteBalanceInfo(DronelinkDJI2.getCameraWhiteBalancePreset(target), null),
                    DronelinkDJI2.createCompletionCallback(finished)));
            return null;
        }

        return new CommandError(context.getString(R.string.MissionDisengageReason_command_type_unhandled) + ": " + command.type);
    }


    private void commandFinishFocusTargetVerifyRing(final Context context, final FocusCameraCommand command, final Command.Finisher finished) {
        commandFinishFocusTargetVerifyRing(context, command, 0, 10, finished);
    }

    private void commandFinishFocusTargetVerifyRing(final Context context, final FocusCameraCommand command, final int attempt, final int maxAttempts, final Command.Finisher finished) {
        if (command.focusRingPercentLimits == null) {
            finished.execute(null);
            return;
        }

        if (attempt >= maxAttempts) {
            finished.execute(new CommandError(context.getString(R.string.DJI2CameraStateAdapter_cameraCommand_focus_target_error)));
            return;
        }

        if (!isBusy()) {
            final Double focusRingValue = getFocusRingValue();
            final Double focusRingMax = getFocusRingMax();
            if (focusRingValue != null && focusRingMax != null && focusRingMax > 0) {
                final double focusRingPercent = focusRingValue / focusRingMax;
                if (focusRingPercent < command.focusRingPercentLimits.min || focusRingPercent > command.focusRingPercentLimits.max) {
                    finished.execute(new CommandError(
                            context.getString(R.string.DJI2CameraStateAdapter_cameraCommand_focus_target_ring_invalid) + " " +
                                    Dronelink.getInstance().format("percent", command.focusRingPercentLimits.min, "") + " < " +
                                    Dronelink.getInstance().format("percent", focusRingPercent, "") + " < " +
                                    Dronelink.getInstance().format("percent", command.focusRingPercentLimits.max, "")
                    ));
                    return;
                }
            }

            finished.execute(null);
            return;
        }


        new Handler().postDelayed(() -> commandFinishFocusTargetVerifyRing(context, command, attempt + 1, maxAttempts, finished), 100);
    }
}