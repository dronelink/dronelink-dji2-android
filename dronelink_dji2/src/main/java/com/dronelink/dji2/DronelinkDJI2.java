//  DronelinkDJI2.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 10/4/22.
//  Copyright © 2022 Dronelink. All rights reserved.
//
package com.dronelink.dji2;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dronelink.core.Convert;
import com.dronelink.core.Dronelink;
import com.dronelink.core.command.Command;
import com.dronelink.core.command.CommandError;
import com.dronelink.core.kernel.core.GeoCoordinate;
import com.dronelink.core.kernel.core.Message;
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
import com.dronelink.core.kernel.core.enums.CameraLensType;
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
import com.dronelink.core.kernel.core.enums.DroneConnectionFailSafeBehavior;
import com.dronelink.core.kernel.core.enums.DroneOcuSyncChannelSelectionMode;
import com.dronelink.core.kernel.core.enums.DroneOcuSyncFrequencyBand;
import com.dronelink.core.kernel.core.enums.RTKReferenceStationSource;
import com.dronelink.core.kernel.core.enums.GimbalMode;
import com.dronelink.core.kernel.core.enums.RTKServiceState;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.value.airlink.ChannelSelectionMode;
import dji.sdk.keyvalue.value.airlink.FrequencyBand;
import dji.sdk.keyvalue.value.camera.CameraFlatMode;
import dji.sdk.keyvalue.value.camera.CameraShootPhotoMode;
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType;
import dji.sdk.keyvalue.value.camera.CameraWhiteBalanceMode;
import dji.sdk.keyvalue.value.camera.DCFCameraType;
import dji.sdk.keyvalue.value.camera.PhotoAEBPhotoCount;
import dji.sdk.keyvalue.value.camera.PhotoBurstCount;
import dji.sdk.keyvalue.value.camera.PhotoFileFormat;
import dji.sdk.keyvalue.value.camera.PhotoRatio;
import dji.sdk.keyvalue.value.camera.ThermalDisplayMode;
import dji.sdk.keyvalue.value.camera.VideoFileCompressionStandard;
import dji.sdk.keyvalue.value.camera.VideoFileFormat;
import dji.sdk.keyvalue.value.camera.VideoFovType;
import dji.sdk.keyvalue.value.camera.VideoFrameRate;
import dji.sdk.keyvalue.value.camera.VideoRecordMode;
import dji.sdk.keyvalue.value.camera.VideoResolution;
import dji.sdk.keyvalue.value.camera.VideoResolutionFrameRateAndFov;
import dji.sdk.keyvalue.value.camera.VideoStandard;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.common.LocationCoordinate2D;
import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.sdk.keyvalue.value.flightcontroller.AirSenseAirplaneState;
import dji.sdk.keyvalue.value.flightcontroller.AirSenseSystemInformation;
import dji.sdk.keyvalue.value.flightcontroller.CompassCalibrationState;
import dji.sdk.keyvalue.value.flightcontroller.CompassSensorState;
import dji.sdk.keyvalue.value.flightcontroller.FCGoHomeState;
import dji.sdk.keyvalue.value.flightcontroller.FailsafeAction;
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthorityChangeReason;
import dji.sdk.keyvalue.value.flightcontroller.FlightMode;
import dji.sdk.keyvalue.value.flightcontroller.GPSSignalLevel;
import dji.sdk.keyvalue.value.flightcontroller.WindWarning;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.sdk.keyvalue.value.rtkmobilestation.RTKError;
import dji.sdk.keyvalue.value.rtkmobilestation.RTKPositioningSolution;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState;
import dji.v5.manager.diagnostic.DJIDeviceStatus;

public class DronelinkDJI2 {
    public static final double GimbalRotationMinTime = 0.1;

    public static CommonCallbacks.CompletionCallback createCompletionCallback(final @Nullable Command.Finisher finisher) {
        return new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                if (finisher == null) {
                    return;
                }

                finisher.execute(null);
            }

            @Override
            public void onFailure(final @NonNull IDJIError error) {
                if (finisher == null) {
                    return;
                }

                finisher.execute(new CommandError(error.errorCode()));
            }
        };
    }

    public static CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> createCompletionCallbackWithParam(final Command.Finisher finisher) {
        return new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
            @Override
            public void onSuccess(final EmptyMsg emptyMsg) {
                if (finisher == null) {
                    return;
                }

                finisher.execute(null);
            }

            @Override
            public void onFailure(final @NonNull IDJIError error) {
                if (finisher == null) {
                    return;
                }

                finisher.execute(new CommandError(error.errorCode()));
            }
        };
    }

    public static LocationCoordinate2D getCoordinate(final GeoCoordinate value) {
        return new LocationCoordinate2D(value.latitude, value.longitude);
    }

    public static Location getLocation(final @Nullable LocationCoordinate2D value) {
        if (value == null) {
            return null;
        }

        final Location location = new Location("");
        location.setLatitude(value.getLatitude());
        location.setLongitude(value.getLongitude());
        return location;
    }

    public static Location getLocation(final @Nullable LocationCoordinate3D value) {
        if (value == null) {
            return null;
        }

        final Location location = new Location("");
        location.setLatitude(value.getLatitude());
        location.setLongitude(value.getLongitude());
        return location;
    }

    public static Message getMessage(final Context context, final @Nullable CompassSensorState value) {
        if (value != null) {
            String details = null;
            Message.Level level = null;

            switch (value) {
                case DISCONNECTED:
                case NORMAL_MODULUS:
                case WEAK_MODULUS:
                case SERIOUS_MODULUS:
                case UNKNOWN:
                    return null;

                case CALIBRATING:
                    details = context.getString(R.string.DronelinkDJI2_CompassSensorState_value_CALIBRATING);
                    level = Message.Level.WARNING;
                    break;

                case UNCALIBRATED:
                    details = context.getString(R.string.DronelinkDJI2_CompassSensorState_value_UNCALIBRATED);
                    level = Message.Level.WARNING;
                    break;

                case DATA_EXCEPTION:
                    details = context.getString(R.string.DronelinkDJI2_CompassSensorState_value_DATA_EXCEPTION);
                    level = Message.Level.WARNING;
                    break;

                case CALIBRATION_FAILED:
                    details = context.getString(R.string.DronelinkDJI2_CompassSensorState_value_CALIBRATION_FAILED);
                    level = Message.Level.WARNING;
                    break;

                case DIRECTION_EXCEPTION:
                    details = context.getString(R.string.DronelinkDJI2_CompassSensorState_value_DIRECTION_EXCEPTION);
                    level = Message.Level.WARNING;
                    break;
            }

            return new Message(context.getString(R.string.DronelinkDJI2_CompassSensorState_title), details, level);
        }
        return null;
    }

    public static Message getMessage(final Context context, final @Nullable CompassCalibrationState value) {
        if (value != null) {
            String details = null;
            Message.Level level = null;

            switch (value) {
                case IDLE:
                case UNKNOWN:
                    return null;

                case HORIZONTAL:
                    details = context.getString(R.string.DronelinkDJI2_CompassCalibrationState_value_HORIZONTAL);
                    level = Message.Level.WARNING;
                    break;

                case VERTICAL:
                    details = context.getString(R.string.DronelinkDJI2_CompassCalibrationState_value_VERTICAL);
                    level = Message.Level.WARNING;
                    break;

                case SUCCEEDED:
                    details = context.getString(R.string.DronelinkDJI2_CompassCalibrationState_value_SUCCEEDED);
                    level = Message.Level.INFO;
                    break;

                case FAILED:
                    details = context.getString(R.string.DronelinkDJI2_CompassCalibrationState_value_FAILED);
                    level = Message.Level.ERROR;
                    break;
            }
            return new Message(context.getString(R.string.DronelinkDJI2_CompassCalibrationState_title), details, level);
        }
        return null;
    }

    public static Message getMessage(final @Nullable DJIDeviceStatus value) {
        if (value != null) {
            Message.Level level = null;
            switch (value) {
                case NORMAL_RTK:
                case NORMAL:
                case REMOTE_DISCONNECT:
                case AIRCRAFT_DISCONNECT:
                case NON_GPS_IN_THE_AIR_NONVISION:
                case NON_GPS_IN_THE_AIR:
                case NON_GPS_NONVISION:
                case NON_GPS:
                case ATTI_STATE_IN_THE_AIR:
                case ATTI_STATE:
                case GOHOME:
                case NORMAL_IN_THE_AIR_RTK:
                case NORMAL_IN_THE_AIR:
                case LOW_POWER:
                case SERIOUS_LOW_POWER:
                    break;

                case MOTOR_STOP_LANDING_IN_AIR:
                case MOTOR_STOP_LANDING_MODE:
                case COMPASS_ERROR:
                case COMPASS_DISTURB:
                case IMU_CALI:
                case FRONT_VISION_CALI:
                case DOWN_VISION_CALI:
                case BACK_VISION_CALI:
                case BATTERY_OVER_CURRENT:
                case BATTERY_OVER_TEMP:
                case BATTERY_LOW_TEMP:
                case IMU_INITIALIZING:
                case SENSOR_ERROR:
                case IMU_ERROR:
                case IMU_COMPASS_ERROR:
                case IMU_HEATING:
                case SMART_LOW_POWER_LANDING:
                case SMART_LOW_POWER:
                case LOW_VOLTAGE:
                case NOT_ENOUGH_FORCE:
                case GOHOME_FAILED:
                case FAILSAFE_GOHOME:
                case FAILSAFE:
                case LOW_POWER_GOHOME:
                case LOW_RC_POWER:
                case LOW_RC_SIGNAL:
                case LOW_RADIO_SIGNAL:
                case RC_SIGNAL_DISTURB:
                case RADIO_SIGNAL_DISTURB:
                case GALE_WARNING:
                case GIMBAL_STUCK:
                case GIMBAL_END_POINT_STUCK:
                case GIMBAL_END_POINT_OVERLOAD:
                case GIMBAL_VIBRATION:
                case IN_NFZ_MAX_HEIGHT:
                case CHLSTATUS_POOR:
                    level = Message.Level.WARNING;
                    break;

                case ESC_ERROR_SKY:
                case ESC_ERROR:
                case MC_DATA_ERROR:
                case VISION_ERROR:
                case BATTERY_CONN_EXCEPTION:
                case BATTERY_EXCEPTION:
                case BATTERY_BROKEN:
                case BATTERY_SELF_RELEASE:
                case DEVICE_LOCK:
                case CANT_TAKEOFF_NOVICE:
                case CANT_TAKEOFF:
                case CANT_TAKEOFF_HMS:
                    level = Message.Level.ERROR;
                    break;

                case SERIOUS_LOW_VOLTAGE_LANDING:
                case SERIOUS_LOW_VOLTAGE:
                case SERIOUS_LOW_POWER_LANDING:
                    level = Message.Level.DANGER;
                    break;
            }

            if (level != null) {
                return new Message(value.description(), level);
            }
        }
        return null;
    }

    public static Message getMessage(final Context context, final @Nullable FCGoHomeState value) {
        if (value != null) {
            String details = null;
            Message.Level level = null;
            switch (value) {
                case IDLE:
                case COMPLETED:
                case UNKNOWN:
                    return null;

                case PREASCENDING:
                    details = context.getString(R.string.DronelinkDJI2_FCGoHomeState_value_PREASCENDING);
                    level = Message.Level.WARNING;
                    break;

                case ALIGN:
                    details = context.getString(R.string.DronelinkDJI2_FCGoHomeState_value_ALIGN);
                    level = Message.Level.WARNING;
                    break;

                case ASCENDING:
                    details = context.getString(R.string.DronelinkDJI2_FCGoHomeState_value_ASCENDING);
                    level = Message.Level.WARNING;
                    break;

                case CRUISE:
                    details = context.getString(R.string.DronelinkDJI2_FCGoHomeState_value_CRUISE);
                    level = Message.Level.WARNING;
                    break;

                case BRAKING:
                    details = context.getString(R.string.DronelinkDJI2_FCGoHomeState_value_BRAKING);
                    level = Message.Level.WARNING;
                    break;

                case AVOID_ASCENDING:
                    details = context.getString(R.string.DronelinkDJI2_FCGoHomeState_value_AVOID_ASCENDING);
                    level = Message.Level.WARNING;
                    break;
            }
            return new Message(context.getString(R.string.DronelinkDJI2_FCGoHomeState_title), details, level);
        }
        return null;
    }

    public static Message getMessage(final Context context, final @Nullable FlightMode value, final @Nullable WaypointMissionExecuteState waypointMissionExecuteState) {
        if (value != null) {
            switch (value) {
                case WAYPOINT:
                    DronelinkDJI2.getMessage(context, waypointMissionExecuteState);
                    return null;

                case MANUAL:
                case ATTI:
                case GPS_NORMAL:
                case POI:
                case TAKE_OFF_READY:
                case AUTO_TAKE_OFF:
                case AUTO_LANDING:
                case GO_HOME:
                case VIRTUAL_STICK:
                case SMART_FLIGHT:
                case PANO:
                case GPS_SPORT:
                case GPS_TRIPOD:
                case AUTO_AVOIDANCE:
                case SMART_FLY:
                case FORCE_LANDING:
                case ATTI_LANDING:
                case CLICK_GO:
                case CINEMATIC:
                case DRAW:
                case FOLLOW_ME:
                case GPS_NOVICE:
                case QUICK_MOVIE:
                case TAP_FLY:
                case MASTER_SHOT:
                case APAS:
                case TIME_LAPSE:
                case MOTOR_START:
                case UNKNOWN:
                    break;
            }
        }
        return null;
    }

    public static Message getMessage(final Context context, final @Nullable RTKError value) {
        if (value != null) {
            String details = null;
            Message.Level level = null;

            switch (value) {
                case INTIALIZING:
                    details = context.getString(R.string.DronelinkDJI2_RTKError_value_INTIALIZING);
                    level = Message.Level.WARNING;
                    break;
                case CANNOT_START:
                    details = context.getString(R.string.DronelinkDJI2_RTKError_value_CANNOT_START);
                    level = Message.Level.ERROR;
                    break;
                case CONNECTION_BROKEN:
                    details = context.getString(R.string.DronelinkDJI2_RTKError_value_CONNECTION_BROKEN);
                    level = Message.Level.ERROR;
                    break;
                case BS_ANTENNA_ERROR:
                    details = context.getString(R.string.DronelinkDJI2_RTKError_value_BS_ANTENNA_ERROR);
                    level = Message.Level.ERROR;
                    break;
                case BS_COORDINATE_RESET:
                    details = context.getString(R.string.DronelinkDJI2_RTKError_value_BS_COORDINATE_RESET);
                    level = Message.Level.ERROR;
                    break;
                case BASE_STATION_NOT_ACTIVATED:
                    details = context.getString(R.string.DronelinkDJI2_RTKError_value_BASE_STATION_NOT_ACTIVATED);
                    level = Message.Level.ERROR;
                    break;
                case RTCM_TYPE_CHANGE:
                    details = context.getString(R.string.DronelinkDJI2_RTKError_value_RTCM_TYPE_CHANGE);
                    level = Message.Level.ERROR;
                    break;
                case BASE_STATION_IS_MOVED:
                    details = context.getString(R.string.DronelinkDJI2_RTKError_value_BASE_STATION_IS_MOVED);
                    level = Message.Level.ERROR;
                    break;
                case BASE_STATION_FELL:
                    details = context.getString(R.string.DronelinkDJI2_RTKError_value_BASE_STATION_FELL);
                    level = Message.Level.ERROR;
                    break;
                case NONE:
                case UNKNOWN:
                    break;
            }

            if (level != null) {
                return new Message(context.getString(R.string.DronelinkDJI2_RTKError_title), details, level);
            }
        }
        return null;
    }

    public static Message getMessage(final Context context, final @Nullable IDJIError value) {
        if (value != null) {
            return new Message(context.getString(R.string.DronelinkDJI2_RTKError_title), value.description(), Message.Level.ERROR);
        }
        return null;
    }

    public static Message getMessage(final Context context, final @Nullable dji.sdk.keyvalue.value.rtkbasestation.RTKServiceState value) {
        if (value != null) {
            String details = null;
            Message.Level level = null;

            switch (value) {
                case RTCM_CONNECTED:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTCM_CONNECTED);
                    level = Message.Level.INFO;
                    break;
                case RTCM_NORMAL:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTCM_NORMAL);
                    level = Message.Level.INFO;
                    break;
                case RTCM_USER_HAS_ACTIVATE:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTCM_USER_HAS_ACTIVATE);
                    level = Message.Level.INFO;
                    break;
                case RTCM_USER_ACCOUNT_EXPIRES_SOON:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTCM_USER_ACCOUNT_EXPIRES_SOON);
                    level = Message.Level.WARNING;
                    break;
                case RTCM_USE_DEFAULT_MOUNT_POINT:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTCM_USE_DEFAULT_MOUNT_POINT);
                    level = Message.Level.WARNING;
                    break;
                case RTCM_AUTH_FAILED:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTCM_AUTH_FAILED);
                    level = Message.Level.ERROR;
                    break;
                case RTCM_USER_NOT_BOUNDED:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTCM_USER_NOT_BOUNDED);
                    level = Message.Level.ERROR;
                    break;
                case RTCM_USER_NOT_ACTIVATED:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTCM_USER_NOT_ACTIVATED);
                    level = Message.Level.ERROR;
                    break;
                case ACCOUNT_EXPIRED:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_ACCOUNT_EXPIRED);
                    level = Message.Level.ERROR;
                    break;
                case RTCM_ILLEGAL_UTC_TIME:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTCM_ILLEGAL_UTC_TIME);
                    level = Message.Level.ERROR;
                    break;
                case RTCM_SET_COORDINATE_FAILURE:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTCM_SET_COORDINATE_FAILURE);
                    level = Message.Level.ERROR;
                    break;
                case RTCM_CONNECTING:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTCM_CONNECTING);
                    level = Message.Level.INFO;
                    break;
                case RTCM_ACTIVATED_FAILED:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTCM_ACTIVATED_FAILED);
                    level = Message.Level.ERROR;
                    break;
                case DISABLED:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_DISABLED);
                    level = Message.Level.ERROR;
                    break;
                case AIRCRAFT_DISCONNECTED:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_AIRCRAFT_DISCONNECTED);
                    level = Message.Level.ERROR;
                    break;
                case CONNECTING:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_CONNECTING);
                    level = Message.Level.INFO;
                    break;
                case TRANSMITTING:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_TRANSMITTING);
                    level = Message.Level.INFO;
                    break;
                case LOGIN_FAILURE:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_LOGIN_FAILURE);
                    level = Message.Level.ERROR;
                    break;
                case INVALID_REQUEST:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_INVALID_REQUEST);
                    level = Message.Level.ERROR;
                    break;
                case ACCOUNT_ERROR:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_ACCOUNT_ERROR);
                    level = Message.Level.ERROR;
                    break;
                case NETWORK_NOT_REACHABLE:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_NETWORK_NOT_REACHABLE);
                    level = Message.Level.ERROR;
                    break;
                case SERVER_NOT_REACHABLE:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_SERVER_NOT_REACHABLE);
                    level = Message.Level.ERROR;
                    break;
                case SERVICE_SUSPENSION:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_SERVICE_SUSPENSION);
                    level = Message.Level.ERROR;
                    break;
                case DISCONNECTED:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_DISCONNECTED);
                    level = Message.Level.WARNING;
                    break;
                case READY:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_READY);
                    level = Message.Level.INFO;
                    break;
                case SEND_GGA_NO_VALID_BASE:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_SEND_GGA_NO_VALID_BASE);
                    level = Message.Level.ERROR;
                    break;
                case RTK_START_PROCESSING:
                    details = context.getString(R.string.DronelinkDJI2_RTKServiceState_value_RTK_START_PROCESSING);
                    level = Message.Level.INFO;
                    break;
                case UNKNOWN:
                    break;
            }

            if (level != null) {
                return new Message(context.getString(R.string.DronelinkDJI2_RTKServiceState_title), details, level);
            }
        }
        return null;
    }
    public static Message getMessage(final Context context, final @Nullable RTKPositioningSolution value) {
        if (value != null) {
            String details = null;
            Message.Level level = null;

            switch (value) {
                case NONE:
                    details = context.getString(R.string.DronelinkDJI2_RTKPositioningSolution_value_NONE);
                    level = Message.Level.ERROR;
                    break;
                case FLOAT:
                    details = context.getString(R.string.DronelinkDJI2_RTKPositioningSolution_value_FLOAT);
                    level = Message.Level.WARNING;
                    break;
                case SINGLE_POINT:
                    details = context.getString(R.string.DronelinkDJI2_RTKPositioningSolution_value_SINGLE_POINT);
                    level = Message.Level.WARNING;
                    break;
                case FIXED_POINT:
                    details = context.getString(R.string.DronelinkDJI2_RTKPositioningSolution_value_FIXED_POINT);
                    level = Message.Level.INFO;
                    break;
                case UNKNOWN:
                    details = context.getString(R.string.DronelinkDJI2_RTKPositioningSolution_value_UNKNOWN);
                    level = Message.Level.ERROR;
                    break;
            }

            if (level != null) {
                return new Message(context.getString(R.string.DronelinkDJI2_RTKPositioningSolution_title), details, level);
            }
        }
        return null;
    }

    public static Message getMessage(final Context context, final @Nullable WaypointMissionExecuteState value) {
        if (value != null) {
            String details = null;
            Message.Level level = null;
            switch (value) {
                case READY:
                    details = context.getString(R.string.DronelinkDJI2_DJIWaypointMissionState_value_READY);
                    level = Message.Level.INFO;
                    break;
                case UPLOADING:
                    details = context.getString(R.string.DronelinkDJI2_DJIWaypointMissionState_value_UPLOADING);
                    level = Message.Level.WARNING;
                    break;
                case PREPARING:
                    details = context.getString(R.string.DronelinkDJI2_DJIWaypointMissionState_value_PREPARING);
                    level = Message.Level.WARNING;
                    break;
                case ENTER_WAYLINE:
                    details = context.getString(R.string.DronelinkDJI2_DJIWaypointMissionState_value_ENTER_WAYLINE);
                    level = Message.Level.INFO;
                    break;
                case EXECUTING:
                    details = context.getString(R.string.DronelinkDJI2_DJIWaypointMissionState_value_EXECUTING);
                    level = Message.Level.INFO;
                    break;
                case INTERRUPTED:
                    details = context.getString(R.string.DronelinkDJI2_DJIWaypointMissionState_value_INTERRUPTED);
                    level = Message.Level.WARNING;
                    break;
                case RETURN_TO_START_POINT:
                    details = context.getString(R.string.DronelinkDJI2_DJIWaypointMissionState_value_RETURN_TO_START_POINT);
                    level = Message.Level.WARNING;
                    break;
                case RECOVERING:
                    details = context.getString(R.string.DronelinkDJI2_DJIWaypointMissionState_value_RECOVERING);
                    level = Message.Level.WARNING;
                    break;
                case IDLE:
                case DISCONNECTED:
                case NOT_SUPPORTED:
                case FINISHED:
                case UNKNOWN:
                    break;
            }

            if (level != null) {
                return new Message(context.getString(R.string.DronelinkDJI2_DJIWaypointMissionState_title), details, level);
            }
        }
        return null;
    }

    public static Message getMessage(final Context context, final @Nullable WindWarning value) {
        if (value != null) {
            String details = null;
            Message.Level level = null;
            switch (value) {
                case UNKNOWN:
                case LEVEL_0:
                    return null;

                case LEVEL_1:
                    details = context.getString(R.string.DronelinkDJI2_WindWarning_LEVEL_1);
                    level = Message.Level.WARNING;
                    break;

                case LEVEL_2:
                    details = context.getString(R.string.DronelinkDJI2_WindWarning_LEVEL_2);
                    level = Message.Level.WARNING;
                    break;
            }
            return new Message(details, level);
        }
        return null;
    }

    public static List<Message> getMessages(final Context context, final AirSenseSystemInformation value) {
        if (value != null) {
            final List<Message> messages = new ArrayList<>();
            for (final AirSenseAirplaneState airplaneState : value.getAirplaneStates()) {
                Message.Level level = null;
                switch (airplaneState.getWarningLevel()) {
                    case LEVEL_0:
                    case LEVEL_1:
                    case LEVEL_2:
                        level = Message.Level.INFO;
                        break;

                    case LEVEL_3:
                        level = Message.Level.WARNING;
                        break;

                    case LEVEL_4:
                    case UNKNOWN:
                        level = Message.Level.DANGER;
                        break;
                }

                messages.add(new Message(
                        context.getString(R.string.DronelinkDJI2_AirSenseAirplaneState_title),
                        context.getString(R.string.DronelinkDJI2_AirSenseAirplaneState_message,
                                Dronelink.getInstance().format("distance", (double) airplaneState.getDistance(), ""),
                                Dronelink.getInstance().format("angle", Convert.DegreesToRadians((double) airplaneState.getHeading()), ""),
                                airplaneState.getCode()),
                        level));
            }
            return messages;
        }
        return null;
    }

    public static double getGPSSignalStrength(final @Nullable GPSSignalLevel value) {
        if (value != null) {
            switch (value) {
                case LEVEL_0:
                    return 0;
                case LEVEL_1:
                    return 0.2;
                case LEVEL_2:
                    return 0.4;
                case LEVEL_3:
                    return 0.6;
                case LEVEL_4:
                    return 0.8;
                case LEVEL_5:
                case LEVEL_10:
                    return 1.0;
            }
        }
        return 0;
    }

    public static ChannelSelectionMode getOcuSyncChannelSelectionMode(final DroneOcuSyncChannelSelectionMode value) {
        switch (value) {
            case AUTO:
                return ChannelSelectionMode.AUTO;
            case MANUAL:
                return ChannelSelectionMode.MANUAL;
            case UNKNOWN:
                return ChannelSelectionMode.UNKNOWN;
        }
        return ChannelSelectionMode.UNKNOWN;
    }

    public static DroneOcuSyncFrequencyBand getOcuSyncFrequencyBand(final @Nullable FrequencyBand value) {
        if (value != null) {
            switch (value) {
                case BAND_DUAL:
                    return DroneOcuSyncFrequencyBand.DUAL;
                case BAND_2_DOT_4G:
                    return DroneOcuSyncFrequencyBand._2_DOT_4_GHZ;
                case BAND_5_DOT_8G:
                    return DroneOcuSyncFrequencyBand._5_DOT_8_GHZ;
                case BAND_5_DOT_7G:
                    return DroneOcuSyncFrequencyBand._5_DOT_7_GHZ;
                case BAND_1_DOT_4G:
                    return DroneOcuSyncFrequencyBand._1_DOT_4_GHZ;
                case UNKNOWN:
                    return DroneOcuSyncFrequencyBand.UNKNOWN;
            }
        }
        return DroneOcuSyncFrequencyBand.UNKNOWN;
    }

    public static FrequencyBand getOcuSyncFrequencyBand(final DroneOcuSyncFrequencyBand value) {
        switch (value) {
            case _2_DOT_4_GHZ:
                return FrequencyBand.BAND_2_DOT_4G;
            case _5_DOT_7_GHZ:
                return FrequencyBand.BAND_5_DOT_7G;
            case _5_DOT_8_GHZ:
                return FrequencyBand.BAND_5_DOT_8G;
            case DUAL:
                return FrequencyBand.BAND_DUAL;
            case UNKNOWN:
                return FrequencyBand.UNKNOWN;
        }
        return FrequencyBand.UNKNOWN;
    }

    public static CameraMode getCameraMode(final @Nullable dji.sdk.keyvalue.value.camera.CameraMode value) {
        if (value != null) {
            switch (value) {
                case PHOTO_NORMAL:
                    return CameraMode.PHOTO;
                case VIDEO_NORMAL:
                    return CameraMode.VIDEO;
                case PHOTO_INTERVAL:
                case PHOTO_HYPER_LIGHT:
                case PHOTO_PANORAMA:
                case PHOTO_SUPER_RESOLUTION:
                case UNKNOWN:
                    return CameraMode.UNKNOWN;
            }
        }
        return CameraMode.UNKNOWN;
    }

    public static CameraMode getCameraMode(final @Nullable dji.sdk.keyvalue.value.camera.CameraFlatMode value) {
        if (value != null) {
            switch (value) {
                case VIDEO_SLOW_MOTION:
                case VIDEO_NORMAL:
                case VIDEO_TIMELAPSE:
                case VIDEO_HDR:
                case VIDEO_HYPERLAPSE:
                case VIDEO_ASTEROID:
                case VIDEO_ROCKET:
                case VIDEO_OBLIQUE:
                case VIDEO_SURROUND:
                case VIDEO_SCREW:
                case VIDEO_COMET:
                case VIDEO_DOLLYZOOM:
                case VIDEO_MOTIONSLAPSE:
                case VIDEO_LOOP_RECORD:
                case VIDEO_VR:
                case VIDEO_MASTER_SHOT_NORMAL:
                case VIDEO_MASTER_SHOT_BIG_OBJECT:
                case VIDEO_MASTER_SHOT_PANO:
                case VIDEO_MASTER_SHOT_PERSON:
                case VIDEO_SHORT:
                case UVC_LIVE_STREAMING:
                case EXPLORE_VIDEO_NORMAL:
                    return CameraMode.VIDEO;
                case PHOTO_AEB:
                case PHOTO_NORMAL:
                case PHOTO_BURST:
                case PHOTO_HDR:
                case PHOTO_INTERVAL:
                case PHOTO_COUNTDOWN:
                case PHOTO_HYPERLIGHT:
                case PHOTO_PANO:
                case PHOTO_EHDR:
                case PHOTO_HIGH_RESOLUTION:
                case PHOTO_REGIONAL_SR:
                case PHOTO_SMART:
                case INTERNAL_AI_SPOT_CHECKING:
                case PHOTO_VR:
                case EXPLORE_PHOTO_NORMAL:
                    return CameraMode.PHOTO;
                case UNKNOWN:
                    return CameraMode.UNKNOWN;
            }
        }
        return CameraMode.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.camera.CameraMode getCameraMode(final @Nullable CameraMode value) {
        if (value != null) {
            switch (value) {
                case PHOTO:
                    return dji.sdk.keyvalue.value.camera.CameraMode.PHOTO_NORMAL;
                case VIDEO:
                    return dji.sdk.keyvalue.value.camera.CameraMode.VIDEO_NORMAL;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.camera.CameraMode.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.camera.CameraMode.UNKNOWN;
    }

    public static CameraAperture getCameraAperture(final @Nullable dji.sdk.keyvalue.value.camera.CameraAperture value) {
        if (value != null) {
            switch (value) {
                case F_AUTO:
                    return CameraAperture.AUTO;
                case F0_95:
                    return CameraAperture.F_0_DOT_95;
                case F1_0:
                    return CameraAperture.F_1_DOT_0;
                case F1_2:
                    return CameraAperture.F_1_DOT_2;
                case F1_4:
                    return CameraAperture.F_1_DOT_4;
                case F1_6:
                    return CameraAperture.F_1_DOT_6;
                case F1_7:
                    return CameraAperture.F_1_DOT_7;
                case F1_8:
                    return CameraAperture.F_1_DOT_8;
                case F2:
                    return CameraAperture.F_2;
                case F2_2:
                    return CameraAperture.F_2_DOT_2;
                case F2_4:
                    return CameraAperture.F_2_DOT_4;
                case F2_5:
                    return CameraAperture.F_2_DOT_5;
                case F2_8:
                    return CameraAperture.F_2_DOT_8;
                case F3_2:
                    return CameraAperture.F_3_DOT_2;
                case F3_4:
                    return CameraAperture.F_3_DOT_4;
                case F3_5:
                    return CameraAperture.F_3_DOT_5;
                case F4:
                    return CameraAperture.F_4;
                case F4_4:
                    return CameraAperture.F_4_DOT_4;
                case F4_5:
                    return CameraAperture.F_4_DOT_5;
                case F4_8:
                    return CameraAperture.F_4_DOT_8;
                case F5:
                    return CameraAperture.F_5;
                case F5_6:
                    return CameraAperture.F_5_DOT_6;
                case F6_3:
                    return CameraAperture.F_6_DOT_3;
                case F6_8:
                    return CameraAperture.F_6_DOT_8;
                case F7_1:
                    return CameraAperture.F_7_DOT_1;
                case F8:
                    return CameraAperture.F_8;
                case F9:
                    return CameraAperture.F_9;
                case F9_5:
                    return CameraAperture.F_9_DOT_5;
                case F9_6:
                    return CameraAperture.F_9_DOT_6;
                case F10:
                    return CameraAperture.F_10;
                case F11:
                    return CameraAperture.F_11;
                case F13:
                    return CameraAperture.F_13;
                case F14:
                    return CameraAperture.F_14;
                case F16:
                    return CameraAperture.F_16;
                case F18:
                    return CameraAperture.F_18;
                case F19:
                    return CameraAperture.F_19;
                case F20:
                    return CameraAperture.F_20;
                case F22:
                    return CameraAperture.F_22;
                case F27:
                    return CameraAperture.F_27;
                case F32:
                    return CameraAperture.F_32;
                case UNKNOWN:
                    return CameraAperture.UNKNOWN;
            }
        }
        return CameraAperture.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.camera.CameraAperture getCameraAperture(final @Nullable CameraAperture value) {
        if (value != null) {
            switch (value) {
                case AUTO:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F_AUTO;
                case F_0_DOT_95:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F0_95;
                case F_1_DOT_0:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F1_0;
                case F_1_DOT_2:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F1_2;
                case F_1_DOT_4:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F1_4;
                case F_1_DOT_6:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F1_6;
                case F_1_DOT_7:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F1_7;
                case F_1_DOT_8:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F1_8;
                case F_2:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F2;
                case F_2_DOT_2:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F2_2;
                case F_2_DOT_4:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F2_4;
                case F_2_DOT_5:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F2_5;
                case F_2_DOT_8:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F2_8;
                case F_3_DOT_2:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F3_2;
                case F_3_DOT_4:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F3_4;
                case F_3_DOT_5:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F3_5;
                case F_4:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F4;
                case F_4_DOT_4:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F4_4;
                case F_4_DOT_5:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F4_5;
                case F_4_DOT_8:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F4_8;
                case F_5:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F5;
                case F_5_DOT_6:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F5_6;
                case F_6_DOT_3:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F6_3;
                case F_6_DOT_8:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F6_8;
                case F_7_DOT_1:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F7_1;
                case F_8:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F8;
                case F_9:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F9;
                case F_9_DOT_5:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F9_5;
                case F_9_DOT_6:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F9_6;
                case F_10:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F10;
                case F_11:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F11;
                case F_13:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F13;
                case F_14:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F14;
                case F_16:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F16;
                case F_18:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F18;
                case F_19:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F19;
                case F_20:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F20;
                case F_22:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F22;
                case F_27:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F27;
                case F_32:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.F32;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.camera.CameraAperture.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.camera.CameraAperture.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.camera.CameraColor getCameraColor(final @Nullable CameraColor value) {
        if (value != null) {
            switch (value) {
                case NONE:
                    return dji.sdk.keyvalue.value.camera.CameraColor.NONE;
                case ART:
                    return dji.sdk.keyvalue.value.camera.CameraColor.ART;
                case REMINISCENCE:
                    return dji.sdk.keyvalue.value.camera.CameraColor.REMINISCENCE;
                case D_CINELIKE:
                    return dji.sdk.keyvalue.value.camera.CameraColor.D_CINE_LIKE;
                case BLACK_AND_WHITE:
                    return dji.sdk.keyvalue.value.camera.CameraColor.BLACK_WHITE;
                case D_LOG:
                    return dji.sdk.keyvalue.value.camera.CameraColor.D_LOG;
                case FILM:
                    return dji.sdk.keyvalue.value.camera.CameraColor.FILM;
                case FILM_B:
                    return dji.sdk.keyvalue.value.camera.CameraColor.FILM_B;
                case FILM_C:
                    return dji.sdk.keyvalue.value.camera.CameraColor.FILM_C;
                case FILM_D:
                    return dji.sdk.keyvalue.value.camera.CameraColor.FILM_D;
                case FILM_E:
                    return dji.sdk.keyvalue.value.camera.CameraColor.FILM_E;
                case FILM_F:
                    return dji.sdk.keyvalue.value.camera.CameraColor.FILM_F;
                case FILM_G:
                    return dji.sdk.keyvalue.value.camera.CameraColor.FILM_G;
                case FILM_H:
                    return dji.sdk.keyvalue.value.camera.CameraColor.FILM_H;
                case FILM_I:
                    return dji.sdk.keyvalue.value.camera.CameraColor.FILM_I;
                case REC709:
                    return dji.sdk.keyvalue.value.camera.CameraColor.REC709;
                case TRUE_COLOR:
                    return dji.sdk.keyvalue.value.camera.CameraColor.TRUE_COLOR;
                case CINELIKE:
                    return dji.sdk.keyvalue.value.camera.CameraColor.CINE_LIKE;
                case HLG:
                    return dji.sdk.keyvalue.value.camera.CameraColor.HLG;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.camera.CameraColor.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.camera.CameraColor.UNKNOWN;
    }

    public static CameraColor getCameraColor(final @Nullable dji.sdk.keyvalue.value.camera.CameraColor value) {
        if (value != null) {
            switch (value) {
                case NONE:
                    return CameraColor.NONE;
                case ART:
                    return CameraColor.ART;
                case REMINISCENCE:
                    return CameraColor.REMINISCENCE;
                case D_CINE_LIKE:
                    return CameraColor.D_CINELIKE;
                case BLACK_WHITE:
                    return CameraColor.BLACK_AND_WHITE;
                case D_LOG:
                    return CameraColor.D_LOG;
                case FILM:
                    return CameraColor.FILM_A;
                case FILM_B:
                    return CameraColor.FILM_B;
                case FILM_C:
                    return CameraColor.FILM_C;
                case FILM_D:
                    return CameraColor.FILM_D;
                case FILM_E:
                    return CameraColor.FILM_E;
                case FILM_F:
                    return CameraColor.FILM_F;
                case FILM_G:
                    return CameraColor.FILM_G;
                case FILM_H:
                    return CameraColor.FILM_H;
                case FILM_I:
                    return CameraColor.FILM_I;
                case REC709:
                    return CameraColor.REC709;
                case TRUE_COLOR:
                    return CameraColor.TRUE_COLOR;
                case CINE_LIKE:
                    return CameraColor.CINELIKE;
                case HLG:
                    return CameraColor.HLG;
                case UNKNOWN:
                    return CameraColor.UNKNOWN;
            }
        }
        return CameraColor.UNKNOWN;
    }

    public static CameraDisplayMode getCameraDisplayMode(final @Nullable ThermalDisplayMode value) {
        if (value != null) {
            switch (value) {
                case VISUAL_ONLY:
                    return CameraDisplayMode.VISUAL;
                case THERMAL_ONLY:
                    return CameraDisplayMode.THERMAL;
                case PIP:
                    return CameraDisplayMode.PIP;
                case MSX:
                    return CameraDisplayMode.MSX;
                case UNKNOWN:
                    return CameraDisplayMode.UNKNOWN;
            }
        }
        return CameraDisplayMode.UNKNOWN;
    }

    public static ThermalDisplayMode getCameraDisplayMode(final @Nullable CameraDisplayMode value) {
        if (value != null) {
            switch (value) {
                case VISUAL:
                    return ThermalDisplayMode.VISUAL_ONLY;
                case THERMAL:
                    return ThermalDisplayMode.THERMAL_ONLY;
                case PIP:
                    return ThermalDisplayMode.PIP;
                case MSX:
                    return ThermalDisplayMode.MSX;
                case UNKNOWN:
                    return ThermalDisplayMode.UNKNOWN;
            }
        }
        return ThermalDisplayMode.UNKNOWN;
    }

    public static CameraExposureCompensation getCameraExposureCompensation(final @Nullable dji.sdk.keyvalue.value.camera.CameraExposureCompensation value) {
        if (value != null) {
            switch (value) {
                case NEG_5P0EV:
                    return CameraExposureCompensation.N_5_0;
                case NEG_4P7EV:
                    return CameraExposureCompensation.N_4_7;
                case NEG_4P3EV:
                    return CameraExposureCompensation.N_4_3;
                case NEG_4P0EV:
                    return CameraExposureCompensation.N_4_0;
                case NEG_3P7EV:
                    return CameraExposureCompensation.N_3_7;
                case NEG_3P3EV:
                    return CameraExposureCompensation.N_3_3;
                case NEG_3P0EV:
                    return CameraExposureCompensation.N_3_0;
                case NEG_2P7EV:
                    return CameraExposureCompensation.N_2_7;
                case NEG_2P3EV:
                    return CameraExposureCompensation.N_2_3;
                case NEG_2P0EV:
                    return CameraExposureCompensation.N_2_0;
                case NEG_1P7EV:
                    return CameraExposureCompensation.N_1_7;
                case NEG_1P3EV:
                    return CameraExposureCompensation.N_1_3;
                case NEG_1P0EV:
                    return CameraExposureCompensation.N_1_0;
                case NEG_0P7EV:
                    return CameraExposureCompensation.N_0_7;
                case NEG_0P3EV:
                    return CameraExposureCompensation.N_0_3;
                case NEG_0EV:
                    return CameraExposureCompensation.N_0_0;
                case POS_0P3EV:
                    return CameraExposureCompensation.P_0_3;
                case POS_0P7EV:
                    return CameraExposureCompensation.P_0_7;
                case POS_1P0EV:
                    return CameraExposureCompensation.P_1_0;
                case POS_1P3EV:
                    return CameraExposureCompensation.P_1_3;
                case POS_1P7EV:
                    return CameraExposureCompensation.P_1_7;
                case POS_2P0EV:
                    return CameraExposureCompensation.P_2_0;
                case POS_2P3EV:
                    return CameraExposureCompensation.P_2_3;
                case POS_2P7EV:
                    return CameraExposureCompensation.P_2_7;
                case POS_3P0EV:
                    return CameraExposureCompensation.P_3_0;
                case POS_3P3EV:
                    return CameraExposureCompensation.P_3_3;
                case POS_3P7EV:
                    return CameraExposureCompensation.P_3_7;
                case POS_4P0EV:
                    return CameraExposureCompensation.P_4_0;
                case POS_4P3EV:
                    return CameraExposureCompensation.P_4_3;
                case POS_4P7EV:
                    return CameraExposureCompensation.P_4_7;
                case POS_5P0EV:
                    return CameraExposureCompensation.P_5_0;
                case FIXED:
                    return CameraExposureCompensation.FIXED;
                case UNKNOWN:
                    return CameraExposureCompensation.UNKNOWN;
            }
        }
        return CameraExposureCompensation.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.camera.CameraExposureCompensation getCameraExposureCompensation(final @Nullable CameraExposureCompensation value) {
        if (value != null) {
            switch (value) {
                case N_5_0:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_5P0EV;
                case N_4_7:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_4P7EV;
                case N_4_3:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_4P3EV;
                case N_4_0:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_4P0EV;
                case N_3_7:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_3P7EV;
                case N_3_3:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_3P3EV;
                case N_3_0:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_3P0EV;
                case N_2_7:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_2P7EV;
                case N_2_3:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_2P3EV;
                case N_2_0:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_2P0EV;
                case N_1_7:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_1P7EV;
                case N_1_3:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_1P3EV;
                case N_1_0:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_1P0EV;
                case N_0_7:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_0P7EV;
                case N_0_3:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_0P3EV;
                case N_0_0:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.NEG_0EV;
                case P_0_3:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_0P3EV;
                case P_0_7:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_0P7EV;
                case P_1_0:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_1P0EV;
                case P_1_3:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_1P3EV;
                case P_1_7:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_1P7EV;
                case P_2_0:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_2P0EV;
                case P_2_3:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_2P3EV;
                case P_2_7:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_2P7EV;
                case P_3_0:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_3P0EV;
                case P_3_3:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_3P3EV;
                case P_3_7:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_3P7EV;
                case P_4_0:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_4P0EV;
                case P_4_3:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_4P3EV;
                case P_4_7:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_4P7EV;
                case P_5_0:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.POS_5P0EV;
                case FIXED:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.FIXED;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.camera.CameraExposureCompensation.UNKNOWN;
    }

    public static CameraExposureMode getCameraExposureMode(final @Nullable dji.sdk.keyvalue.value.camera.CameraExposureMode value) {
        if (value != null) {
            switch (value) {
                case PROGRAM:
                    return CameraExposureMode.PROGRAM;
                case SHUTTER_PRIORITY:
                    return CameraExposureMode.SHUTTER_PRIORITY;
                case APERTURE_PRIORITY:
                    return CameraExposureMode.APERTURE_PRIORITY;
                case MANUAL:
                    return CameraExposureMode.MANUAL;
                case UNKNOWN:
                    return CameraExposureMode.UNKNOWN;
            }
        }
        return CameraExposureMode.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.camera.CameraExposureMode getCameraExposureMode(final @Nullable CameraExposureMode value) {
        if (value != null) {
            switch (value) {
                case PROGRAM:
                    return dji.sdk.keyvalue.value.camera.CameraExposureMode.PROGRAM;
                case SHUTTER_PRIORITY:
                    return dji.sdk.keyvalue.value.camera.CameraExposureMode.SHUTTER_PRIORITY;
                case APERTURE_PRIORITY:
                    return dji.sdk.keyvalue.value.camera.CameraExposureMode.APERTURE_PRIORITY;
                case MANUAL:
                    return dji.sdk.keyvalue.value.camera.CameraExposureMode.MANUAL;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.camera.CameraExposureMode.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.camera.CameraExposureMode.UNKNOWN;
    }

    public static CameraFileIndexMode getCameraFileIndexMode(final @Nullable dji.sdk.keyvalue.value.camera.CameraFileIndexMode value) {
        if (value != null) {
            switch (value) {
                case RESET:
                    return CameraFileIndexMode.RESET;
                case SEQUENCE:
                    return CameraFileIndexMode.SEQUENCE;
                case UNKNOWN:
                    return CameraFileIndexMode.UNKNOWN;
            }
        }
        return CameraFileIndexMode.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.camera.CameraFileIndexMode getCameraFileIndexMode(final @Nullable CameraFileIndexMode value) {
        if (value != null) {
            switch (value) {
                case RESET:
                    return dji.sdk.keyvalue.value.camera.CameraFileIndexMode.RESET;
                case SEQUENCE:
                    return dji.sdk.keyvalue.value.camera.CameraFileIndexMode.SEQUENCE;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.camera.CameraFileIndexMode.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.camera.CameraFileIndexMode.UNKNOWN;
    }

    public static CameraFocusMode getCameraFocusMode(final @Nullable dji.sdk.keyvalue.value.camera.CameraFocusMode value) {
        if (value != null) {
            switch (value) {
                case MANUAL:
                    return CameraFocusMode.MANUAL;
                case AF:
                    return CameraFocusMode.AUTO;
                case AFC:
                    return CameraFocusMode.AFC;
                case FINE_TUNE:
                    return CameraFocusMode.FINE_TUNE;
                case UNKNOWN:
                    return CameraFocusMode.UNKNOWN;
            }
        }
        return CameraFocusMode.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.camera.CameraFocusMode getCameraFocusMode(final @Nullable CameraFocusMode value) {
        if (value != null) {
            switch (value) {
                case MANUAL:
                    return dji.sdk.keyvalue.value.camera.CameraFocusMode.MANUAL;
                case AUTO:
                    return dji.sdk.keyvalue.value.camera.CameraFocusMode.AF;
                case AFC:
                    return dji.sdk.keyvalue.value.camera.CameraFocusMode.AFC;
                case FINE_TUNE:
                    return dji.sdk.keyvalue.value.camera.CameraFocusMode.FINE_TUNE;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.camera.CameraFocusMode.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.camera.CameraFocusMode.UNKNOWN;
    }

    public static CameraISO getCameraISO(final @Nullable dji.sdk.keyvalue.value.camera.CameraISO value) {
        if (value != null) {
            switch (value) {
                case ISO_AUTO:
                    return CameraISO.AUTO;
                case ISO_50:
                    return CameraISO._50;
                case ISO_100:
                    return CameraISO._100;
                case ISO_200:
                    return CameraISO._200;
                case ISO_400:
                    return CameraISO._400;
                case ISO_800:
                    return CameraISO._800;
                case ISO_1600:
                    return CameraISO._1600;
                case ISO_3200:
                    return CameraISO._3200;
                case ISO_6400:
                    return CameraISO._6400;
                case ISO_12800:
                    return CameraISO._12800;
                case ISO_25600:
                    return CameraISO._25600;
                case ISO_51200:
                    return CameraISO._51200;
                case ISO_102400:
                    return CameraISO._102400;
                case ISO_FIXED:
                    return CameraISO.FIXED;
                case UNKNOWN:
                    return CameraISO.UNKNOWN;
            }
        }
        return CameraISO.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.camera.CameraISO getCameraISO(final @Nullable CameraISO value) {
        if (value != null) {
            switch (value) {
                case AUTO:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_AUTO;
                case _50:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_50;
                case _100:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_100;
                case _200:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_200;
                case _400:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_400;
                case _800:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_800;
                case _1600:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_1600;
                case _3200:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_3200;
                case _6400:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_6400;
                case _12800:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_12800;
                case _25600:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_25600;
                case _51200:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_51200;
                case _102400:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_102400;
                case FIXED:
                    return dji.sdk.keyvalue.value.camera.CameraISO.ISO_FIXED;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.camera.CameraISO.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.camera.CameraISO.UNKNOWN;
    }

    public static CameraMeteringMode getCameraMeteringMode(final @Nullable dji.sdk.keyvalue.value.camera.CameraMeteringMode value) {
        if (value != null) {
            switch (value) {
                case CENTER:
                    return CameraMeteringMode.CENTER;
                case AVERAGE:
                    return CameraMeteringMode.AVERAGE;
                case REGION:
                    return CameraMeteringMode.SPOT;
                case UNKNOWN:
                    return CameraMeteringMode.UNKNOWN;
            }
        }
        return CameraMeteringMode.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.camera.CameraMeteringMode getCameraMeteringMode(final @Nullable CameraMeteringMode value) {
        if (value != null) {
            switch (value) {
                case CENTER:
                    return dji.sdk.keyvalue.value.camera.CameraMeteringMode.CENTER;
                case AVERAGE:
                    return dji.sdk.keyvalue.value.camera.CameraMeteringMode.AVERAGE;
                case SPOT:
                    return dji.sdk.keyvalue.value.camera.CameraMeteringMode.REGION;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.camera.CameraMeteringMode.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.camera.CameraMeteringMode.UNKNOWN;
    }

    public static CameraBurstCount getCameraPhotoBurstCount(final @Nullable PhotoBurstCount value) {
        if (value != null) {
            switch (value) {
                case COUNT3:
                    return CameraBurstCount._3;
                case COUNT5:
                    return CameraBurstCount._5;
                case COUNT7:
                    return CameraBurstCount._7;
                case COUNT10:
                    return CameraBurstCount._10;
                case COUNT14:
                    return CameraBurstCount._14;
                case CONTINUOUS:
                    return CameraBurstCount.continuous;
                case UNKNOWN:
                    return CameraBurstCount.UNKNOWN;
            }
        }
        return CameraBurstCount.UNKNOWN;
    }

    public static CameraAEBCount getCameraPhotoAEBCount(final @Nullable PhotoAEBPhotoCount value) {
        if (value != null) {
            switch (value) {
                case COUNT_3:
                    return CameraAEBCount._3;
                case COUNT_5:
                    return CameraAEBCount._5;
                case COUNT_7:
                    return CameraAEBCount._7;
                case UNKNOWN:
                    return CameraAEBCount.UNKNOWN;
            }
        }
        return CameraAEBCount.UNKNOWN;
    }

    public static PhotoAEBPhotoCount getCameraPhotoAEBCount(final @Nullable CameraAEBCount value) {
        if (value != null) {
            switch (value) {
                case _3:
                    return PhotoAEBPhotoCount.COUNT_3;
                case _5:
                    return PhotoAEBPhotoCount.COUNT_5;
                case _7:
                    return PhotoAEBPhotoCount.COUNT_7;
                case UNKNOWN:
                    return PhotoAEBPhotoCount.UNKNOWN;
            }
        }
        return PhotoAEBPhotoCount.UNKNOWN;
    }

    public static CameraPhotoAspectRatio getCameraPhotoAspectRatio(final @Nullable PhotoRatio value) {
        if (value != null) {
            switch (value) {
                case RATIO_4COLON3:
                    return CameraPhotoAspectRatio._4_3;
                case RATIO_16COLON9:
                    return CameraPhotoAspectRatio._16_9;
                case RATIO_3COLON2:
                    return CameraPhotoAspectRatio._3_2;
                case RATIO_SQUARE:
                    return CameraPhotoAspectRatio._1_1;
                case RATIO_18COLON9:
                    return CameraPhotoAspectRatio._18_9;
                case RATIO_5COLON4:
                    return CameraPhotoAspectRatio._5_4;
                case UNKNOWN:
                    return CameraPhotoAspectRatio.UNKNOWN;
            }
        }
        return CameraPhotoAspectRatio.UNKNOWN;
    }

    public static PhotoRatio getCameraPhotoAspectRatio(final @Nullable CameraPhotoAspectRatio value) {
        if (value != null) {
            switch (value) {
                case _4_3:
                    return PhotoRatio.RATIO_4COLON3;
                case _16_9:
                    return PhotoRatio.RATIO_16COLON9;
                case _3_2:
                    return PhotoRatio.RATIO_3COLON2;
                case _1_1:
                    return PhotoRatio.RATIO_SQUARE;
                case _18_9:
                    return PhotoRatio.RATIO_18COLON9;
                case _5_4:
                    return PhotoRatio.RATIO_5COLON4;
                case UNKNOWN:
                    return PhotoRatio.UNKNOWN;
            }
        }
        return PhotoRatio.UNKNOWN;
    }

    public static CameraPhotoMode getCameraPhotoMode(final @Nullable CameraShootPhotoMode value) {
        if (value != null) {
            switch (value) {
                case NORMAL:
                    return CameraPhotoMode.SINGLE;
                case HDR:
                    return CameraPhotoMode.HDR;
                case BURST:
                    return CameraPhotoMode.BURST;
                case AEB:
                    return CameraPhotoMode.AEB;
                case INTERVAL:
                    return CameraPhotoMode.INTERVAL;
                case PANO_APP:
                    return CameraPhotoMode.PANORAMA;
                case RAW_BURST:
                    return CameraPhotoMode.RAW_BURST;
                case EHDR:
                    return CameraPhotoMode.EHDR;
                case HYPER_LIGHT:
                    return CameraPhotoMode.HYPER_LIGHT;
                case HYPER_LAPSE:
                    return CameraPhotoMode.HYPER_LAPSE;
                case VISION_PANO:
                case VISION_BOKEH:
                case STATIONARY_TIME_LAPSE:
                case MOTION_TIME_LAPSE:
                case HYPER_TIME_LAPSE:
                case UNKNOWN:
                    return CameraPhotoMode.UNKNOWN;
            }
        }
        return CameraPhotoMode.UNKNOWN;
    }

    public static CameraPhotoMode getCameraPhotoMode(final @Nullable CameraFlatMode value) {
        if (value != null) {
            switch (value) {
                case PHOTO_AEB:
                    return CameraPhotoMode.AEB;
                case PHOTO_NORMAL:
                    return CameraPhotoMode.SINGLE;
                case PHOTO_BURST:
                    return CameraPhotoMode.BURST;
                case PHOTO_HDR:
                    return CameraPhotoMode.HDR;
                case PHOTO_INTERVAL:
                    return CameraPhotoMode.INTERVAL;
                case PHOTO_COUNTDOWN:
                    return CameraPhotoMode.COUNTDOWN;
                case PHOTO_HYPERLIGHT:
                    return CameraPhotoMode.HYPER_LIGHT;
                case PHOTO_PANO:
                    return CameraPhotoMode.PANORAMA;
                case PHOTO_EHDR:
                    return CameraPhotoMode.EHDR;
                case PHOTO_HIGH_RESOLUTION:
                    return CameraPhotoMode.HIGH_RESOLUTION;
                case PHOTO_REGIONAL_SR:
                    return CameraPhotoMode.REGIONAL_SR;
                case PHOTO_SMART:
                    return CameraPhotoMode.SMART;
                case INTERNAL_AI_SPOT_CHECKING:
                    return CameraPhotoMode.INTERNAL_AI_SPOT_CHECKING;
                case PHOTO_VR:
                    return CameraPhotoMode.VR;
                case VIDEO_HYPERLAPSE:
                case VIDEO_ASTEROID:
                case VIDEO_ROCKET:
                case VIDEO_OBLIQUE:
                case VIDEO_SURROUND:
                case VIDEO_SCREW:
                case VIDEO_COMET:
                case VIDEO_DOLLYZOOM:
                case VIDEO_MOTIONSLAPSE:
                case VIDEO_LOOP_RECORD:
                case VIDEO_SLOW_MOTION:
                case VIDEO_NORMAL:
                case VIDEO_TIMELAPSE:
                case VIDEO_HDR:
                case VIDEO_VR:
                case VIDEO_MASTER_SHOT_NORMAL:
                case VIDEO_MASTER_SHOT_BIG_OBJECT:
                case VIDEO_MASTER_SHOT_PANO:
                case VIDEO_MASTER_SHOT_PERSON:
                case VIDEO_SHORT:
                case UVC_LIVE_STREAMING:
                case EXPLORE_PHOTO_NORMAL:
                case EXPLORE_VIDEO_NORMAL:
                case UNKNOWN:
                    break;
            }
        }
        return CameraPhotoMode.UNKNOWN;
    }

    public static CameraFlatMode getCameraFlatMode(final @Nullable CameraPhotoMode value) {
        if (value != null) {
            switch (value) {
                case SINGLE:
                    return CameraFlatMode.PHOTO_NORMAL;
                case HDR:
                    return CameraFlatMode.PHOTO_HDR;
                case BURST:
                case RAW_BURST:
                    return CameraFlatMode.PHOTO_BURST;
                case AEB:
                    return CameraFlatMode.PHOTO_AEB;
                case INTERVAL:
                    return CameraFlatMode.PHOTO_INTERVAL;
                case PANORAMA:
                    return CameraFlatMode.PHOTO_PANO;
                case EHDR:
                    return CameraFlatMode.PHOTO_EHDR;
                case COUNTDOWN:
                    return CameraFlatMode.PHOTO_COUNTDOWN;
                case HYPER_LIGHT:
                    return CameraFlatMode.PHOTO_HYPERLIGHT;
                case HIGH_RESOLUTION:
                    return CameraFlatMode.PHOTO_HIGH_RESOLUTION;
                case REGIONAL_SR:
                    return CameraFlatMode.PHOTO_REGIONAL_SR;
                case SMART:
                    return CameraFlatMode.PHOTO_SMART;
                case INTERNAL_AI_SPOT_CHECKING:
                    return CameraFlatMode.INTERNAL_AI_SPOT_CHECKING;
                case VR:
                    return CameraFlatMode.PHOTO_VR;
                case TIME_LAPSE:
                case HYPER_LAPSE:
                case SHALLOW_FOCUS:
                case SUPER_RESOLUTION:
                case UNKNOWN:
                    break;
            }
        }
        return CameraFlatMode.UNKNOWN;
    }

    public static CameraPhotoFileFormat getCameraPhotoFileFormat(final @Nullable PhotoFileFormat value) {
        if (value != null) {
            switch (value) {
                case RAW: return CameraPhotoFileFormat.RAW;
                case JPEG: return CameraPhotoFileFormat.JPEG;
                case RAW_JPEG: return CameraPhotoFileFormat.RAW_JPEG;
                case TIFF_8_BIT: return CameraPhotoFileFormat.TIFF_8_BIT;
                case TIFF_14_BIT: return CameraPhotoFileFormat.TIFF_14_BIT;
                case TIFF_14_BIT_LINEAR_LOW_TEMP_RESOLUTION: return CameraPhotoFileFormat.TIFF_14_BIT_LINEAR_LOW_TEMP_RESOLUTION;
                case TIFF_14_BIT_LINEAR_HIGH_TEMP_RESOLUTION: return CameraPhotoFileFormat.TIFF_14_BIT_LINEAR_HIGH_TEMP_RESOLUTION;
                case RADIOMETRIC_JPEG: return CameraPhotoFileFormat.RADIOMETRIC_JPEG;
                case RADIOMETRIC_JPEG_LOW: return CameraPhotoFileFormat.RADIOMETRIC_JPEG_LOW;
                case RADIOMETRIC_JPEG_HIGH: return CameraPhotoFileFormat.RADIOMETRIC_JPEG_HIGH;
                case UNKNOWN: return CameraPhotoFileFormat.UNKNOWN;
            }
        }
        return CameraPhotoFileFormat.UNKNOWN;
    }

    public static PhotoFileFormat getCameraPhotoFileFormat(final @Nullable CameraPhotoFileFormat value) {
        if (value != null) {
            switch (value) {
                case RAW: return PhotoFileFormat.RAW;
                case JPEG: return PhotoFileFormat.JPEG;
                case RAW_JPEG: return PhotoFileFormat.RAW_JPEG;
                case TIFF_8_BIT: return PhotoFileFormat.TIFF_8_BIT;
                case TIFF_14_BIT: return PhotoFileFormat.TIFF_14_BIT;
                case TIFF_14_BIT_LINEAR_LOW_TEMP_RESOLUTION: return PhotoFileFormat.TIFF_14_BIT_LINEAR_LOW_TEMP_RESOLUTION;
                case TIFF_14_BIT_LINEAR_HIGH_TEMP_RESOLUTION: return PhotoFileFormat.TIFF_14_BIT_LINEAR_HIGH_TEMP_RESOLUTION;
                case RADIOMETRIC_JPEG: return PhotoFileFormat.RADIOMETRIC_JPEG;
                case RADIOMETRIC_JPEG_LOW: return PhotoFileFormat.RADIOMETRIC_JPEG_LOW;
                case RADIOMETRIC_JPEG_HIGH: return PhotoFileFormat.RADIOMETRIC_JPEG_HIGH;
                case UNKNOWN: return PhotoFileFormat.UNKNOWN;
            }
        }
        return PhotoFileFormat.UNKNOWN;
    }

    public static CameraShutterSpeed getCameraShutterSpeed(final @Nullable dji.sdk.keyvalue.value.camera.CameraShutterSpeed value) {
        if (value != null) {
            switch (value) {
                case SHUTTER_SPEED1_20000:
                    return CameraShutterSpeed._1_20000;
                case SHUTTER_SPEED1_16000:
                    return CameraShutterSpeed._1_16000;
                case SHUTTER_SPEED1_12800:
                    return CameraShutterSpeed._1_12800;
                case SHUTTER_SPEED1_10000:
                    return CameraShutterSpeed._1_10000;
                case SHUTTER_SPEED1_8000:
                    return CameraShutterSpeed._1_8000;
                case SHUTTER_SPEED1_6400:
                    return CameraShutterSpeed._1_6400;
                case SHUTTER_SPEED1_6000:
                    return CameraShutterSpeed._1_6000;
                case SHUTTER_SPEED1_5000:
                    return CameraShutterSpeed._1_5000;
                case SHUTTER_SPEED1_4000:
                    return CameraShutterSpeed._1_4000;
                case SHUTTER_SPEED1_3200:
                    return CameraShutterSpeed._1_3200;
                case SHUTTER_SPEED1_3000:
                    return CameraShutterSpeed._1_3000;
                case SHUTTER_SPEED1_2500:
                    return CameraShutterSpeed._1_2500;
                case SHUTTER_SPEED1_2000:
                    return CameraShutterSpeed._1_2000;
                case SHUTTER_SPEED1_1600:
                    return CameraShutterSpeed._1_1600;
                case SHUTTER_SPEED1_1500:
                    return CameraShutterSpeed._1_1500;
                case SHUTTER_SPEED1_1250:
                    return CameraShutterSpeed._1_1250;
                case SHUTTER_SPEED1_1000:
                    return CameraShutterSpeed._1_1000;
                case SHUTTER_SPEED1_800:
                    return CameraShutterSpeed._1_800;
                case SHUTTER_SPEED1_725:
                    return CameraShutterSpeed._1_725;
                case SHUTTER_SPEED1_640:
                    return CameraShutterSpeed._1_640;
                case SHUTTER_SPEED1_500:
                    return CameraShutterSpeed._1_500;
                case SHUTTER_SPEED1_400:
                    return CameraShutterSpeed._1_400;
                case SHUTTER_SPEED1_350:
                    return CameraShutterSpeed._1_350;
                case SHUTTER_SPEED1_320:
                    return CameraShutterSpeed._1_320;
                case SHUTTER_SPEED1_250:
                    return CameraShutterSpeed._1_250;
                case SHUTTER_SPEED1_240:
                    return CameraShutterSpeed._1_240;
                case SHUTTER_SPEED1_200:
                    return CameraShutterSpeed._1_200;
                case SHUTTER_SPEED1_180:
                    return CameraShutterSpeed._1_180;
                case SHUTTER_SPEED1_160:
                    return CameraShutterSpeed._1_160;
                case SHUTTER_SPEED1_125:
                    return CameraShutterSpeed._1_125;
                case SHUTTER_SPEED1_120:
                    return CameraShutterSpeed._1_120;
                case SHUTTER_SPEED1_100:
                    return CameraShutterSpeed._1_100;
                case SHUTTER_SPEED1_90:
                    return CameraShutterSpeed._1_90;
                case SHUTTER_SPEED1_80:
                    return CameraShutterSpeed._1_80;
                case SHUTTER_SPEED1_60:
                    return CameraShutterSpeed._1_60;
                case SHUTTER_SPEED1_50:
                    return CameraShutterSpeed._1_50;
                case SHUTTER_SPEED1_40:
                    return CameraShutterSpeed._1_40;
                case SHUTTER_SPEED1_30:
                    return CameraShutterSpeed._1_30;
                case SHUTTER_SPEED1_25:
                    return CameraShutterSpeed._1_25;
                case SHUTTER_SPEED1_20:
                    return CameraShutterSpeed._1_20;
                case SHUTTER_SPEED1_15:
                    return CameraShutterSpeed._1_15;
                case SHUTTER_SPEED1_12DOT5:
                    return CameraShutterSpeed._1_12_DOT_5;
                case SHUTTER_SPEED1_10:
                    return CameraShutterSpeed._1_10;
                case SHUTTER_SPEED1_8:
                    return CameraShutterSpeed._1_8;
                case SHUTTER_SPEED1_6DOT25:
                    return CameraShutterSpeed._1_6_DOT_25;
                case SHUTTER_SPEED1_5:
                    return CameraShutterSpeed._1_5;
                case SHUTTER_SPEED1_4:
                    return CameraShutterSpeed._1_4;
                case SHUTTER_SPEED1_3:
                    return CameraShutterSpeed._1_3;
                case SHUTTER_SPEED1_2DOT5:
                    return CameraShutterSpeed._1_2_DOT_5;
                case SHUTTER_SPEED1_2:
                    return CameraShutterSpeed._1_2;
                case SHUTTER_SPEED1_1DOT67:
                    return CameraShutterSpeed._1_1_DOT_67;
                case SHUTTER_SPEED1_1DOT25:
                    return CameraShutterSpeed._1_1_DOT_25;
                case SHUTTER_SPEED1:
                    return CameraShutterSpeed._1;
                case SHUTTER_SPEED1DOT3:
                    return CameraShutterSpeed._1_DOT_3;
                case SHUTTER_SPEED1DOT6:
                    return CameraShutterSpeed._1_DOT_6;
                case SHUTTER_SPEED2:
                    return CameraShutterSpeed._2;
                case SHUTTER_SPEED2DOT5:
                    return CameraShutterSpeed._2_DOT_5;
                case SHUTTER_SPEED3:
                    return CameraShutterSpeed._3;
                case SHUTTER_SPEED3DOT2:
                    return CameraShutterSpeed._3_DOT_2;
                case SHUTTER_SPEED4:
                    return CameraShutterSpeed._4;
                case SHUTTER_SPEED5:
                    return CameraShutterSpeed._5;
                case SHUTTER_SPEED6:
                    return CameraShutterSpeed._6;
                case SHUTTER_SPEED7:
                    return CameraShutterSpeed._7;
                case SHUTTER_SPEED8:
                    return CameraShutterSpeed._8;
                case SHUTTER_SPEED9:
                    return CameraShutterSpeed._9;
                case SHUTTER_SPEED10:
                    return CameraShutterSpeed._10;
                case SHUTTER_SPEED11:
                    return CameraShutterSpeed._11;
                case SHUTTER_SPEED13:
                    return CameraShutterSpeed._13;
                case SHUTTER_SPEED15:
                    return CameraShutterSpeed._15;
                case SHUTTER_SPEED16:
                    return CameraShutterSpeed._16;
                case SHUTTER_SPEED20:
                    return CameraShutterSpeed._20;
                case SHUTTER_SPEED23:
                    return CameraShutterSpeed._23;
                case SHUTTER_SPEED25:
                    return CameraShutterSpeed._25;
                case SHUTTER_SPEED30:
                    return CameraShutterSpeed._30;
                case SHUTTER_SPEED40:
                    return CameraShutterSpeed._40;
                case SHUTTER_SPEED50:
                    return CameraShutterSpeed._50;
                case SHUTTER_SPEED60:
                    return CameraShutterSpeed._60;
                case SHUTTER_SPEED80:
                    return CameraShutterSpeed._80;
                case SHUTTER_SPEED100:
                    return CameraShutterSpeed._100;
                case SHUTTER_SPEED120:
                    return CameraShutterSpeed._120;
                case SHUTTER_SPEED_AUTO:
                    return CameraShutterSpeed.AUTO;
                case UNKNOWN:
                    return CameraShutterSpeed.UNKNOWN;
            }
        }
        return CameraShutterSpeed.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.camera.CameraShutterSpeed getCameraShutterSpeed(final @Nullable CameraShutterSpeed value) {
        if (value != null) {
            switch (value) {
                case _1_20000:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_20000;
                case _1_16000:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_16000;
                case _1_12800:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_12800;
                case _1_10000:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_10000;
                case _1_8000:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_8000;
                case _1_6400:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_6400;
                case _1_6000:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_6000;
                case _1_5000:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_5000;
                case _1_4000:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_4000;
                case _1_3200:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_3200;
                case _1_3000:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_3000;
                case _1_2500:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_2500;
                case _1_2000:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_2000;
                case _1_1600:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_1600;
                case _1_1500:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_1500;
                case _1_1250:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_1250;
                case _1_1000:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_1000;
                case _1_800:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_800;
                case _1_725:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_725;
                case _1_640:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_640;
                case _1_500:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_500;
                case _1_400:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_400;
                case _1_350:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_350;
                case _1_320:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_320;
                case _1_250:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_250;
                case _1_240:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_240;
                case _1_200:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_200;
                case _1_180:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_180;
                case _1_160:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_160;
                case _1_125:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_125;
                case _1_120:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_120;
                case _1_100:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_100;
                case _1_90:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_90;
                case _1_80:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_80;
                case _1_60:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_60;
                case _1_50:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_50;
                case _1_40:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_40;
                case _1_30:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_30;
                case _1_25:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_25;
                case _1_20:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_20;
                case _1_15:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_15;
                case _1_12_DOT_5:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_12DOT5;
                case _1_10:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_10;
                case _1_8:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_8;
                case _1_6_DOT_25:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_6DOT25;
                case _1_5:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_5;
                case _1_4:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_4;
                case _1_3:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_3;
                case _1_2_DOT_5:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_2DOT5;
                case _1_2:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_2;
                case _1_1_DOT_67:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_1DOT67;
                case _1_1_DOT_25:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1_1DOT25;
                case _1:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1;
                case _1_DOT_3:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1DOT3;
                case _1_DOT_6:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED1DOT6;
                case _2:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED2;
                case _2_DOT_5:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED2DOT5;
                case _3:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED3;
                case _3_DOT_2:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED3DOT2;
                case _4:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED4;
                case _5:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED5;
                case _6:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED6;
                case _7:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED7;
                case _8:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED8;
                case _9:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED9;
                case _10:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED10;
                case _11:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED11;
                case _13:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED13;
                case _15:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED15;
                case _16:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED16;
                case _20:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED20;
                case _23:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED23;
                case _25:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED25;
                case _30:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED30;
                case _40:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED40;
                case _50:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED50;
                case _60:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED60;
                case _80:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED80;
                case _100:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED100;
                case _120:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED120;
                case AUTO:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.SHUTTER_SPEED_AUTO;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.camera.CameraShutterSpeed.UNKNOWN;
    }

    public static CameraVideoFileCompressionStandard getCameraVideoFileCompressionStandard(final @Nullable VideoFileCompressionStandard value) {
        if (value != null) {
            switch (value) {
                case H264:
                    return CameraVideoFileCompressionStandard.H264;
                case H265:
                    return CameraVideoFileCompressionStandard.H265;
                case ProRes:
                    return CameraVideoFileCompressionStandard.PRO_RES;
                case UNKNOWN:
                    break;
            }
        }
        return CameraVideoFileCompressionStandard.UNKNOWN;
    }

    public static VideoFileCompressionStandard getCameraVideoFileCompressionStandard(final @Nullable CameraVideoFileCompressionStandard value) {
        if (value != null) {
            switch (value) {
                case H264:
                    return VideoFileCompressionStandard.H264;
                case H265:
                    return VideoFileCompressionStandard.H265;
                case PRO_RES:
                    return VideoFileCompressionStandard.ProRes;
                case UNKNOWN:
                    break;
            }
        }
        return VideoFileCompressionStandard.UNKNOWN;
    }

    public static CameraVideoFileFormat getCameraVideoFileFormat(final @Nullable VideoFileFormat value) {
        if (value != null) {
            switch (value) {
                case MOV:
                    return CameraVideoFileFormat.MOV;
                case MP4:
                    return CameraVideoFileFormat.MP4;
                case TIFF_SEQ:
                    return CameraVideoFileFormat.TIFF_SEQ;
                case SEQ:
                    return CameraVideoFileFormat.SEQ;
                case CDNG:
                    return CameraVideoFileFormat.CDNG;
                case MXF:
                    return CameraVideoFileFormat.MXF;
                case UNKNOWN:
                    return CameraVideoFileFormat.UNKNOWN;
            }
        }
        return CameraVideoFileFormat.UNKNOWN;
    }

    public static VideoFileFormat getCameraVideoFileFormat(final @Nullable CameraVideoFileFormat value) {
        if (value != null) {
            switch (value) {
                case MOV:
                    return VideoFileFormat.MOV;
                case MP4:
                    return VideoFileFormat.MP4;
                case TIFF_SEQ:
                    return VideoFileFormat.TIFF_SEQ;
                case SEQ:
                    return VideoFileFormat.SEQ;
                case CDNG:
                    return VideoFileFormat.CDNG;
                case MXF:
                    return VideoFileFormat.MXF;
                case UNKNOWN:
                    return VideoFileFormat.UNKNOWN;
            }
        }
        return VideoFileFormat.UNKNOWN;
    }

    public static CameraVideoFieldOfView getCameraVideoFieldOfView(final @Nullable VideoResolutionFrameRateAndFov value) {
        if (value != null) {
            switch (value.fov) {
                case NONE:
                    return CameraVideoFieldOfView.DEFAULT;
                case MEDIUM:
                    return CameraVideoFieldOfView.MIDDLE;
                case NARROW:
                    return CameraVideoFieldOfView.NARROW;
                case WIDE:
                    return CameraVideoFieldOfView.WIDE;
                case SNARROW:
                case UNKNOWN:
                    break;
            }
        }
        return CameraVideoFieldOfView.UNKNOWN;
    }

    public static VideoFovType getCameraVideoFieldOfView(final @Nullable CameraVideoFieldOfView value) {
        if (value != null) {
            switch (value) {
                case DEFAULT:
                    return VideoFovType.NONE;
                case MIDDLE:
                    return VideoFovType.MEDIUM;
                case NARROW:
                    return VideoFovType.NARROW;
                case WIDE:
                    return VideoFovType.WIDE;
                case UNKNOWN:
                    return VideoFovType.UNKNOWN;
            }
        }
        return VideoFovType.UNKNOWN;
    }

    public static CameraVideoFrameRate getCameraVideoFrameRate(final @Nullable VideoResolutionFrameRateAndFov value) {
        if (value != null) {
            switch (value.frameRateAndResolution.frameRate) {
                case RATE_24FPS:
                    return CameraVideoFrameRate._23_DOT_976;
                case RATE_PRECISE_24FPS:
                    return CameraVideoFrameRate._24;
                case RATE_25FPS:
                    return CameraVideoFrameRate._25;
                case RATE_30FPS:
                    return CameraVideoFrameRate._29_DOT_970;
                case RATE_PRECISE_30FPS:
                    return CameraVideoFrameRate._30;
                case RATE_48FPS:
                    return CameraVideoFrameRate._47_DOT_950;
                case RATE_PRECISE_48FPS:
                    return CameraVideoFrameRate._48;
                case RATE_50FPS:
                    return CameraVideoFrameRate._50;
                case RATE_60FPS:
                    return CameraVideoFrameRate._59_DOT_940;
                case RATE_PRECISE_60FPS:
                    return CameraVideoFrameRate._60;
                case RATE_90FPS:
                    return CameraVideoFrameRate._90;
                case RATE_96FPS:
                case RATE_PRECISE_96FPS:
                    return CameraVideoFrameRate._96;
                case RATE_100FPS:
                    return CameraVideoFrameRate._100;
                case RATE_120FPS:
                case RATE_PRECISE_120FPS:
                    return CameraVideoFrameRate._120;
                case RATE_240FPS:
                    return CameraVideoFrameRate._240;
                case RATE_1FPS:
                case RATE_2FPS:
                case RATE_3FPS:
                case RATE_4FPS:
                case RATE_6FPS:
                case RATE_7FPS:
                case RATE_5FPS:
                case RATE_8FPS:
                case RATE_8DOT7_FPS:
                case RATE_9FPS:
                case RATE_10FPS:
                case RATE_11FPS:
                case RATE_12FPS:
                case RATE_13FPS:
                case RATE_14FPS:
                case RATE_15FPS:
                case RATE_16FPS:
                case RATE_17FPS:
                case RATE_18FPS:
                case RATE_19FPS:
                case RATE_20FPS:
                case RATE_21FPS:
                case RATE_22FPS:
                case RATE_23FPS:
                case RATE_26FPS:
                case RATE_27FPS:
                case RATE_28FPS:
                case RATE_29FPS:
                case RATE_31FPS:
                case RATE_32FPS:
                case RATE_33FPS:
                case RATE_34FPS:
                case RATE_35FPS:
                case RATE_36FPS:
                case RATE_37FPS:
                case RATE_38FPS:
                case RATE_39FPS:
                case RATE_40FPS:
                case RATE_41FPS:
                case RATE_42FPS:
                case RATE_43FPS:
                case RATE_44FPS:
                case RATE_45FPS:
                case RATE_46FPS:
                case RATE_47FPS:
                case RATE_49FPS:
                case RATE_51FPS:
                case RATE_52FPS:
                case RATE_53FPS:
                case RATE_54FPS:
                case RATE_55FPS:
                case RATE_56FPS:
                case RATE_57FPS:
                case RATE_58FPS:
                case RATE_59FPS:
                case RATE_61FPS:
                case RATE_62FPS:
                case RATE_63FPS:
                case RATE_64FPS:
                case RATE_65FPS:
                case RATE_66FPS:
                case RATE_67FPS:
                case RATE_68FPS:
                case RATE_69FPS:
                case RATE_70FPS:
                case RATE_71FPS:
                case RATE_73FPS:
                case RATE_74FPS:
                case RATE_76FPS:
                case RATE_77FPS:
                case RATE_78FPS:
                case RATE_79FPS:
                case RATE_80FPS:
                case RATE_81FPS:
                case RATE_82FPS:
                case RATE_83FPS:
                case RATE_84FPS:
                case RATE_85FPS:
                case RATE_86FPS:
                case RATE_87FPS:
                case RATE_88FPS:
                case RATE_72FPS:
                case RATE_PRECISE_72FPS:
                case RATE_75FPS:
                case RATE_89FPS:
                case RATE_91FPS:
                case RATE_92FPS:
                case RATE_93FPS:
                case RATE_94FPS:
                case RATE_95FPS:
                case RATE_97FPS:
                case RATE_98FPS:
                case RATE_99FPS:
                case RATE_101FPS:
                case RATE_102FPS:
                case RATE_103FPS:
                case RATE_104FPS:
                case RATE_105FPS:
                case RATE_106FPS:
                case RATE_107FPS:
                case RATE_108FPS:
                case RATE_109FPS:
                case RATE_110FPS:
                case RATE_111FPS:
                case RATE_112FPS:
                case RATE_113FPS:
                case RATE_114FPS:
                case RATE_115FPS:
                case RATE_116FPS:
                case RATE_117FPS:
                case RATE_118FPS:
                case RATE_119FPS:
                case RATE_180FPS:
                case RATE_192FPS:
                case RATE_200FPS:
                case RATE_400FPS:
                case RATE_480FPS:
                    return CameraVideoFrameRate.UNKNOWN;
            }
        }
        return CameraVideoFrameRate.UNKNOWN;
    }

    public static VideoFrameRate getCameraVideoFrameRate(final @Nullable CameraVideoFrameRate value) {
        if (value != null) {
            switch (value) {
                case _23_DOT_976:
                    return VideoFrameRate.RATE_24FPS;
                case _24:
                    return VideoFrameRate.RATE_PRECISE_24FPS;
                case _25:
                    return VideoFrameRate.RATE_25FPS;
                case _29_DOT_970:
                    return VideoFrameRate.RATE_30FPS;
                case _30:
                    return VideoFrameRate.RATE_PRECISE_30FPS;
                case _47_DOT_950:
                    return VideoFrameRate.RATE_48FPS;
                case _48:
                    return VideoFrameRate.RATE_PRECISE_48FPS;
                case _50:
                    return VideoFrameRate.RATE_50FPS;
                case _59_DOT_940:
                    return VideoFrameRate.RATE_60FPS;
                case _60:
                    return VideoFrameRate.RATE_PRECISE_60FPS;
                case _90:
                    return VideoFrameRate.RATE_90FPS;
                case _96:
                    return VideoFrameRate.RATE_PRECISE_96FPS;
                case _100:
                    return VideoFrameRate.RATE_100FPS;
                case _120:
                    return VideoFrameRate.RATE_PRECISE_120FPS;
                case _240:
                    return VideoFrameRate.RATE_240FPS;
                case _8_DOT_7:
                    return VideoFrameRate.RATE_8DOT7_FPS;
                case UNKNOWN:
                    break;
            }
        }
        return VideoFrameRate.UNKNOWN;
    }

    public static VideoRecordMode getCameraVideoMode(final @Nullable CameraVideoMode value) {
        if (value != null) {
            switch (value) {
                case NORMAL:
                    return VideoRecordMode.NORMAL;
                case HDR:
                    return VideoRecordMode.HDR;
                case SLOW_MOTION:
                    return VideoRecordMode.SLOW_MOTION;
                case FAST_MOTION:
                    return VideoRecordMode.FAST_MOTION;
                case TIME_LAPSE:
                    return VideoRecordMode.TIME_LAPSE;
                case HYPER_LAPSE:
                    return VideoRecordMode.HYPER_LAPSE;
                case QUICK_SHOT:
                    return VideoRecordMode.QUICK_SHOT;
                case UNKNOWN:
                    return VideoRecordMode.UNKNOWN;
            }
        }
        return VideoRecordMode.UNKNOWN;
    }

    public static CameraVideoMode getCameraVideoMode(final @Nullable VideoRecordMode value) {
        if (value != null) {
            switch (value) {
                case NORMAL:
                    return CameraVideoMode.NORMAL;
                case HDR:
                    return CameraVideoMode.HDR;
                case SLOW_MOTION:
                    return CameraVideoMode.SLOW_MOTION;
                case FAST_MOTION:
                    return CameraVideoMode.FAST_MOTION;
                case TIME_LAPSE:
                    return CameraVideoMode.TIME_LAPSE;
                case HYPER_LAPSE:
                    return CameraVideoMode.HYPER_LAPSE;
                case QUICK_SHOT:
                    return CameraVideoMode.QUICK_SHOT;
                case UNKNOWN:
                    return CameraVideoMode.UNKNOWN;
            }
        }
        return CameraVideoMode.UNKNOWN;
    }

    public static CameraVideoResolution getCameraVideoResolution(final @Nullable VideoResolutionFrameRateAndFov value) {
        if (value != null) {
            switch (value.frameRateAndResolution.resolution) {
                case RESOLUTION_640x480:
                    return CameraVideoResolution._640x480;
                case RESOLUTION_640x512:
                    return CameraVideoResolution._640x512;
                case RESOLUTION_864X480P:
                case RESOLUTION_864x480:
                    return CameraVideoResolution._864x480;
                case RESOLUTION_1280x720:
                    return CameraVideoResolution._1280x720;
                case RESOLUTION_1920x960:
                    return CameraVideoResolution._1920x960;
                case RESOLUTION_1920x1080:
                    return CameraVideoResolution._1920x1080;
                case RESOLUTION_2048x1080:
                    return CameraVideoResolution._2048x1080;
                case RESOLUTION_2704x1520:
                    return CameraVideoResolution._2704x1520;
                case RESOLUTION_2720x1530:
                    return CameraVideoResolution._2720x1530;
                case RESOLUTION_3840x1572:
                    return CameraVideoResolution._3840x1572;
                case RESOLUTION_3840x2160:
                    return CameraVideoResolution._3840x2160;
                case RESOLUTION_4096x2160:
                    return CameraVideoResolution._4096x2160;
                case RESOLUTION_4608x2160:
                    return CameraVideoResolution._4608x2160;
                case RESOLUTION_4608x2592:
                    return CameraVideoResolution._4608x2592;
                case RESOLUTION_5280x2160:
                    return CameraVideoResolution._5280x2160;
                case RESOLUTION_5280x2972:
                    return CameraVideoResolution._5280x2972;
                case RESOLUTION_5760x3240:
                    return CameraVideoResolution._5760x3240;
                case RESOLUTION_6016x3200:
                    return CameraVideoResolution._6016x3200;
                case RESOLUTION_336x256:
                    return CameraVideoResolution._336x256;
                case RESOLUTION_5120x2880:
                    return CameraVideoResolution._5120x2880;
                case RESOLUTION_2688x1512:
                    return CameraVideoResolution._2688x1512;
                case RESOLUTION_640x360:
                    return CameraVideoResolution._640x360;
                case RESOLUTION_4000x3000:
                    return CameraVideoResolution._4000x3000;
                case RESOLUTION_2880x1620:
                    return CameraVideoResolution._2880x1620;
                case RESOLUTION_2720x2040P:
                    return CameraVideoResolution._2720x2040;
                case RESOLUTION_720x576:
                    return CameraVideoResolution._720x576;
                case RESOLUTION_7680x4320:
                    return CameraVideoResolution._7680x4320;
                case RESOLUTION_8192x4320:
                    return CameraVideoResolution._8192x4320;
                case RESOLUTION_5576X2952:
                    return CameraVideoResolution._5576x2952;
                case RESOLUTION_5248X2952:
                    return CameraVideoResolution._5248x2952;
                case RESOLUTION_4096x3072:
                    return CameraVideoResolution._4096x3072;
                case RESOLUTION_4096x2728:
                    return CameraVideoResolution._4096x2728;
                case RESOLUTION_2688x2016:
                    return CameraVideoResolution._2688x2016;
                case RESOLUTION_5472x3078:
                    return CameraVideoResolution._5472x3078;
                case RESOLUTION_8192X3424:
                    return CameraVideoResolution._8192x3424;
                case RESOLUTION_5120x2700:
                    return CameraVideoResolution._5120x2700;
                case RESOLUTION_640x340:
                    return CameraVideoResolution._640x340;
                case RESOLUTION_1280x1024:
                    return CameraVideoResolution._1280x1024;
                case RESOLUTION_5472X3648P:
                    return CameraVideoResolution._5472x3648;
                case RESOLUTION_1080X1920P:
                    return CameraVideoResolution._1080x1920;
                case RESOLUTION_1512X2688P:
                    return CameraVideoResolution._1512x2688;
                case RESOLUTION_MAX:
                    return CameraVideoResolution.MAX;
                case RESOLUTION_16x9_480P:
                case RESOLUTION_4X3_720P:
                case RESOLUTION_UNSET:
                case UNKNOWN:
                    return CameraVideoResolution.UNKNOWN;
            }
        }
        return CameraVideoResolution.UNKNOWN;
    }

    public static VideoResolution getCameraVideoResolution(final @Nullable CameraVideoResolution value) {
        if (value != null) {
            switch (value) {
                case _640x480:
                    return VideoResolution.RESOLUTION_640x480;
                case _640x512:
                    return VideoResolution.RESOLUTION_640x512;
                case _864x480:
                    return VideoResolution.RESOLUTION_864x480;
                case _1280x720:
                    return VideoResolution.RESOLUTION_1280x720;
                case _1920x960:
                    return VideoResolution.RESOLUTION_1920x960;
                case _1920x1080:
                    return VideoResolution.RESOLUTION_1920x1080;
                case _2048x1080:
                    return VideoResolution.RESOLUTION_2048x1080;
                case _2704x1520:
                    return VideoResolution.RESOLUTION_2704x1520;
                case _2720x1530:
                    return VideoResolution.RESOLUTION_2720x1530;
                case _3840x1572:
                    return VideoResolution.RESOLUTION_3840x1572;
                case _3840x2160:
                    return VideoResolution.RESOLUTION_3840x2160;
                case _4096x2160:
                    return VideoResolution.RESOLUTION_4096x2160;
                case _4608x2160:
                    return VideoResolution.RESOLUTION_4608x2160;
                case _4608x2592:
                    return VideoResolution.RESOLUTION_4608x2592;
                case _5280x2160:
                    return VideoResolution.RESOLUTION_5280x2160;
                case _5280x2972:
                    return VideoResolution.RESOLUTION_5280x2972;
                case _5760x3240:
                    return VideoResolution.RESOLUTION_5760x3240;
                case _6016x3200:
                    return VideoResolution.RESOLUTION_6016x3200;
                case _336x256:
                    return VideoResolution.RESOLUTION_336x256;
                case _5120x2880:
                    return VideoResolution.RESOLUTION_5120x2880;
                case _2688x1512:
                    return VideoResolution.RESOLUTION_2688x1512;
                case _640x360:
                    return VideoResolution.RESOLUTION_640x360;
                case _4000x3000:
                    return VideoResolution.RESOLUTION_4000x3000;
                case _2880x1620:
                    return VideoResolution.RESOLUTION_2880x1620;
                case _2720x2040:
                    return VideoResolution.RESOLUTION_2720x2040P;
                case _720x576:
                    return VideoResolution.RESOLUTION_720x576;
                case _7680x4320:
                    return VideoResolution.RESOLUTION_7680x4320;
                case _8192x4320:
                    return VideoResolution.RESOLUTION_8192x4320;
                case _5576x2952:
                    return VideoResolution.RESOLUTION_5576X2952;
                case _5248x2952:
                    return VideoResolution.RESOLUTION_5248X2952;
                case _4096x3072:
                    return VideoResolution.RESOLUTION_4096x3072;
                case _4096x2728:
                    return VideoResolution.RESOLUTION_4096x2728;
                case _2688x2016:
                    return VideoResolution.RESOLUTION_2688x2016;
                case _5472x3078:
                    return VideoResolution.RESOLUTION_5472x3078;
                case _8192x3424:
                    return VideoResolution.RESOLUTION_8192X3424;
                case _5120x2700:
                    return VideoResolution.RESOLUTION_5120x2700;
                case _640x340:
                    return VideoResolution.RESOLUTION_640x340;
                case _1280x1024:
                    return VideoResolution.RESOLUTION_1280x1024;
                case _5472x3648:
                    return VideoResolution.RESOLUTION_5472X3648P;
                case _1080x1920:
                    return VideoResolution.RESOLUTION_1080X1920P;
                case _1512x2688:
                    return VideoResolution.RESOLUTION_1512X2688P;
                case MAX:
                    return VideoResolution.RESOLUTION_MAX;
                case UNKNOWN:
                    return VideoResolution.UNKNOWN;
            }
        }
        return VideoResolution.UNKNOWN;
    }

    public static CameraVideoStandard getCameraVideoStandard(final @Nullable VideoStandard value) {
        if (value != null) {
            switch (value) {
                case PAL:
                    return CameraVideoStandard.PAL;
                case NTSC:
                    return CameraVideoStandard.NTSC;
                case UNKNOWN:
                    return CameraVideoStandard.UNKNOWN;
            }
        }
        return CameraVideoStandard.UNKNOWN;
    }

    public static VideoStandard getCameraVideoStandard(final @Nullable CameraVideoStandard value) {
        if (value != null) {
            switch (value) {
                case PAL:
                    return VideoStandard.PAL;
                case NTSC:
                    return VideoStandard.NTSC;
                case UNKNOWN:
                    return VideoStandard.UNKNOWN;
            }
        }
        return VideoStandard.UNKNOWN;
    }

    public static CameraWhiteBalancePreset getCameraWhiteBalancePreset(final @Nullable CameraWhiteBalanceMode value) {
        if (value != null) {
            switch (value) {
                case AUTO:
                    return CameraWhiteBalancePreset.AUTO;
                case SUNNY:
                    return CameraWhiteBalancePreset.SUNNY;
                case CLOUDY:
                    return CameraWhiteBalancePreset.CLOUDY;
                case WATER_SURFACE:
                    return CameraWhiteBalancePreset.WATER_SURFACE;
                case INDOOR_INCANDESCENT:
                    return CameraWhiteBalancePreset.INDOOR_INCANDESCENT;
                case INDOOR_FLUORESCENT:
                    return CameraWhiteBalancePreset.INDOOR_FLUORESCENT;
                case MANUAL:
                    return CameraWhiteBalancePreset.CUSTOM;
                case NATURAL:
                    return CameraWhiteBalancePreset.NEUTRAL;
                case UNDERWATER:
                    return CameraWhiteBalancePreset.UNDERWATER;
                case UNKNOWN:
                    return CameraWhiteBalancePreset.UNKNOWN;
            }
        }
        return CameraWhiteBalancePreset.UNKNOWN;
    }

    public static CameraWhiteBalanceMode getCameraWhiteBalancePreset(final @Nullable CameraWhiteBalancePreset value) {
        if (value != null) {
            switch (value) {
                case AUTO:
                    return CameraWhiteBalanceMode.AUTO;
                case SUNNY:
                    return CameraWhiteBalanceMode.SUNNY;
                case CLOUDY:
                    return CameraWhiteBalanceMode.CLOUDY;
                case WATER_SURFACE:
                    return CameraWhiteBalanceMode.WATER_SURFACE;
                case INDOOR_INCANDESCENT:
                    return CameraWhiteBalanceMode.INDOOR_INCANDESCENT;
                case INDOOR_FLUORESCENT:
                    return CameraWhiteBalanceMode.INDOOR_FLUORESCENT;
                case CUSTOM:
                    return CameraWhiteBalanceMode.MANUAL;
                case NEUTRAL:
                    return CameraWhiteBalanceMode.NATURAL;
                case UNDERWATER:
                    return CameraWhiteBalanceMode.UNDERWATER;
                case UNKNOWN:
                    return CameraWhiteBalanceMode.UNKNOWN;
            }
        }
        return CameraWhiteBalanceMode.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.camera.CameraStorageLocation getCameraStorageLocation(final @Nullable CameraStorageLocation value) {
        if (value != null) {
            switch (value) {
                case SD_CARD:
                    return dji.sdk.keyvalue.value.camera.CameraStorageLocation.SDCARD;
                case INTERNAL:
                    return dji.sdk.keyvalue.value.camera.CameraStorageLocation.INTERNAL;
                case INTERNAL_SSD:
                    return dji.sdk.keyvalue.value.camera.CameraStorageLocation.INTERNAL_SSD;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.camera.CameraStorageLocation.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.camera.CameraStorageLocation.UNKNOWN;
    }

    public static CameraStorageLocation getCameraStorageLocation(final @Nullable dji.sdk.keyvalue.value.camera.CameraStorageLocation value) {
        if (value != null) {
            switch (value) {
                case SDCARD:
                    return CameraStorageLocation.SD_CARD;
                case INTERNAL:
                    return CameraStorageLocation.INTERNAL;
                case INTERNAL_SSD:
                    return CameraStorageLocation.INTERNAL_SSD;
                case UNKNOWN:
                    return CameraStorageLocation.UNKNOWN;
            }
        }
        return CameraStorageLocation.UNKNOWN;
    }

    public static CameraLensType getCameraLensType(final @Nullable DCFCameraType value) {
        if (value != null) {
            switch (value) {
                case INFRARED:
                    return CameraLensType.THERMAL;
                case ZOOM:
                    return CameraLensType.ZOOM;
                case WIDE:
                    return CameraLensType.WIDE;
                case SUPER_RESOLUTION:
                    return CameraLensType.UNKNOWN;
                case SCREEN:
                    return CameraLensType.UNKNOWN;
                case VISIBLE:
                    return CameraLensType.VISIBLE;
                case UNKNOWN:
                    return CameraLensType.UNKNOWN;
            }
        }
        return CameraLensType.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.common.CameraLensType getCameraLensType(final @Nullable CameraVideoStreamSourceType value) {
        if (value != null) {
            switch (value) {
                case DEFAULT_CAMERA:
                    return dji.sdk.keyvalue.value.common.CameraLensType.CAMERA_LENS_DEFAULT;
                case WIDE_CAMERA:
                    return dji.sdk.keyvalue.value.common.CameraLensType.CAMERA_LENS_WIDE;
                case ZOOM_CAMERA:
                    return dji.sdk.keyvalue.value.common.CameraLensType.CAMERA_LENS_ZOOM;
                case INFRARED_CAMERA:
                    return dji.sdk.keyvalue.value.common.CameraLensType.CAMERA_LENS_THERMAL;
                case NDVI_CAMERA:
                    return dji.sdk.keyvalue.value.common.CameraLensType.CAMERA_LENS_MS_NDVI;
                case VISION_CAMERA:
                    return dji.sdk.keyvalue.value.common.CameraLensType.CAMERA_LENS_RGB;
                case MS_G_CAMERA:
                    return dji.sdk.keyvalue.value.common.CameraLensType.CAMERA_LENS_MS_G;
                case MS_R_CAMERA:
                    return dji.sdk.keyvalue.value.common.CameraLensType.CAMERA_LENS_MS_R;
                case MS_RE_CAMERA:
                    return dji.sdk.keyvalue.value.common.CameraLensType.CAMERA_LENS_MS_RE;
                case MS_NIR_CAMERA:
                    return dji.sdk.keyvalue.value.common.CameraLensType.CAMERA_LENS_MS_NIR;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.common.CameraLensType.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.common.CameraLensType.UNKNOWN;
    }

    public static CameraVideoStreamSource getCameraVideoStreamSource(final @Nullable CameraVideoStreamSourceType value) {
        if (value != null) {
            switch (value) {
                case DEFAULT_CAMERA:
                    return CameraVideoStreamSource.DEFAULT;
                case WIDE_CAMERA:
                    return CameraVideoStreamSource.WIDE;
                case ZOOM_CAMERA:
                    return CameraVideoStreamSource.ZOOM;
                case INFRARED_CAMERA:
                    return CameraVideoStreamSource.THERMAL;
                case NDVI_CAMERA:
                    return CameraVideoStreamSource.NDVI;
                case VISION_CAMERA:
                    return CameraVideoStreamSource.VISIBLE;
                case MS_G_CAMERA:
                    return CameraVideoStreamSource.MS_G;
                case MS_R_CAMERA:
                    return CameraVideoStreamSource.MS_R;
                case MS_RE_CAMERA:
                    return CameraVideoStreamSource.MS_RE;
                case MS_NIR_CAMERA:
                    return CameraVideoStreamSource.MS_NIR;
                case UNKNOWN:
                    return CameraVideoStreamSource.UNKNOWN;
                case RGB_CAMERA:
                    break;
            }
        }
        return CameraVideoStreamSource.UNKNOWN;
    }

    public static CameraVideoStreamSourceType getCameraVideoStreamSource(final @Nullable CameraVideoStreamSource value) {
        if (value != null) {
            switch (value) {
                case DEFAULT:
                    return CameraVideoStreamSourceType.DEFAULT_CAMERA;
                case WIDE:
                    return CameraVideoStreamSourceType.WIDE_CAMERA;
                case ZOOM:
                    return CameraVideoStreamSourceType.ZOOM_CAMERA;
                case THERMAL:
                    return CameraVideoStreamSourceType.INFRARED_CAMERA;
                case NDVI:
                    return CameraVideoStreamSourceType.NDVI_CAMERA;
                case VISIBLE:
                    return CameraVideoStreamSourceType.RGB_CAMERA;
                case MS_G:
                    return CameraVideoStreamSourceType.MS_G_CAMERA;
                case MS_R:
                    return CameraVideoStreamSourceType.MS_R_CAMERA;
                case MS_RE:
                    return CameraVideoStreamSourceType.MS_RE_CAMERA;
                case MS_NIR:
                    return CameraVideoStreamSourceType.MS_NIR_CAMERA;
                case UNKNOWN:
                    return CameraVideoStreamSourceType.UNKNOWN;
            }
        }
        return CameraVideoStreamSourceType.UNKNOWN;
    }

    public static FailsafeAction getDroneConnectionFailSafeBehavior(final @Nullable DroneConnectionFailSafeBehavior value) {
        if (value != null) {
            switch (value) {
                case HOVER:
                    return FailsafeAction.HOVER;
                case RETURN_HOME:
                    return FailsafeAction.GOHOME;
                case AUTO_LAND:
                    return FailsafeAction.LANDING;
                case UNKNOWN:
                    return FailsafeAction.UNKNOWN;
            }
        }
        return FailsafeAction.UNKNOWN;
    }

    public static GimbalMode getGimbalMode(final @Nullable dji.sdk.keyvalue.value.gimbal.GimbalMode value) {
        if (value != null) {
            switch (value) {
                case FREE:
                    return GimbalMode.FREE;
                case FPV:
                    return GimbalMode.FPV;
                case YAW_FOLLOW:
                    return GimbalMode.YAW_FOLLOW;
                case UNKNOWN:
                    return GimbalMode.UNKNOWN;
            }
        }
        return GimbalMode.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.gimbal.GimbalMode getGimbalMode(final @Nullable GimbalMode value) {
        if (value != null) {
            switch (value) {
                case FREE:
                    return dji.sdk.keyvalue.value.gimbal.GimbalMode.FREE;
                case FPV:
                    return dji.sdk.keyvalue.value.gimbal.GimbalMode.FPV;
                case YAW_FOLLOW:
                    return dji.sdk.keyvalue.value.gimbal.GimbalMode.YAW_FOLLOW;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.gimbal.GimbalMode.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.gimbal.GimbalMode.UNKNOWN;
    }

    public static String getString(final Context context, final @Nullable dji.sdk.keyvalue.value.common.CameraLensType value) {
        if (value != null) {
            switch (value) {
                case CAMERA_LENS_ZOOM:
                    return context.getString(R.string.DronelinkDJI2_CameraLensType_CAMERA_LENS_ZOOM);
                case CAMERA_LENS_WIDE:
                    return context.getString(R.string.DronelinkDJI2_CameraLensType_CAMERA_LENS_WIDE);
                case CAMERA_LENS_THERMAL:
                    return context.getString(R.string.DronelinkDJI2_CameraLensType_CAMERA_LENS_THERMAL);
                case CAMERA_LENS_MS_G:
                    return context.getString(R.string.DronelinkDJI2_CameraLensType_CAMERA_LENS_MS_G);
                case CAMERA_LENS_MS_R:
                    return context.getString(R.string.DronelinkDJI2_CameraLensType_CAMERA_LENS_MS_R);
                case CAMERA_LENS_MS_RE:
                    return context.getString(R.string.DronelinkDJI2_CameraLensType_CAMERA_LENS_MS_RE);
                case CAMERA_LENS_MS_NIR:
                    return context.getString(R.string.DronelinkDJI2_CameraLensType_CAMERA_LENS_MS_NIR);
                case CAMERA_LENS_MS_NDVI:
                    return context.getString(R.string.DronelinkDJI2_CameraLensType_CAMERA_LENS_MS_NDVI);
                case CAMERA_LENS_RGB:
                    return context.getString(R.string.DronelinkDJI2_CameraLensType_CAMERA_LENS_RGB);
                case CAMERA_LENS_DEFAULT:
                    return context.getString(R.string.DronelinkDJI2_CameraLensType_CAMERA_LENS_DEFAULT);
                case UNKNOWN:
                    return context.getString(R.string.DronelinkDJI2_CameraLensType_CAMERA_LENS_UNKNOWN);
            }
        }
        return context.getString(R.string.DronelinkDJI2_CameraLensType_CAMERA_LENS_UNKNOWN);
    }

    public static String getString(final Context context, final @Nullable ProductType value) {
        if (value != null) {
            switch (value) {
                case DJI_MAVIC_3_ENTERPRISE_SERIES:
                    return context.getString(R.string.DronelinkDJI2_ProductType_value_DJI_MAVIC_3_ENTERPRISE_SERIES);
                case M30_SERIES:
                    return context.getString(R.string.DronelinkDJI2_ProductType_value_M30_SERIES);
                case M300_RTK:
                    return context.getString(R.string.DronelinkDJI2_ProductType_value_M300_RTK);
                case UNRECOGNIZED:
                    return context.getString(R.string.DronelinkDJI2_ProductType_value_UNRECOGNIZED);
                case UNKNOWN:
                    return context.getString(R.string.DronelinkDJI2_ProductType_value_UNKNOWN);
                case OSMO:
                case P4:
                case MAVIC_PRO:
                case OSMO_PRO:
                case OSMO_RAW:
                case OSMO_PLUS:
                case P4P:
                case P4A:
                case P4R:
                case OSMO_MOBILE1:
                case MAVIC_AIR:
                case OSMO_ACTION:
                case OSMO_POCKET:
                case MAVIC_2:
                case P4P_V2:
                case OSMO_MOBILE2:
                case MAVIC_2_ENTERPRISE:
                case MAVIC_MINI:
                case M200_V2_SERIES:
                case M200_V2_PRO:
                case M200_V2_RTK:
                case MG_T16:
                case DRTK_2:
                case OSMO_MOBILE3:
                case MAVIC_AIR_2:
                case FPV_SERIAL_2:
                case DJI_AIR_2S:
                case MAVIC_MINI_2:
                case OSMO_MOBILE5:
                case OSMO_ACTION_2:
                case OSMO_ACTION_2_HASSELBLAD:
                case AC202:
                case HG212:
                case MG_T20:
                case NOT_SUPPORTED_1:
                case NOT_SUPPORTED_2:
                case NOT_SUPPORTED_3:
                case NOT_SUPPORTED_4:
                case NOT_SUPPORTED_5:
                case NOT_SUPPORTED_6:
                case OSMO_POCKET2:
                case OSMO_MOBILE4:
                case OSMO_MOBILE6:
                case OSMO_MOBILE5_SE:
                case DJI_MAVIC_3:
                case MAVIC_MINI_SE:
                case TA101:
                case EA210:
                    return value.name();
            }
        }
        return context.getString(R.string.DronelinkDJI2_ProductType_value_UNKNOWN);
    }

    public static boolean isWaypointMissionState(final WaypointMissionExecuteState value, final WaypointMissionExecuteState[] states) {
        for (final WaypointMissionExecuteState state : states) {
            if (state.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static String getString(final Context context, final @Nullable FlightControlAuthorityChangeReason value) {
        if (value != null) {
            switch (value) {
                case MSDK_REQUEST:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_MSDK_REQUEST);
                case AUTO_TEST_REQUEST:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_AUTO_TEST_REQUEST);
                case OSDK_REQUEST:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_OSDK_REQUEST);
                case RC_LOST:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_RC_LOST);
                case RC_NOT_P_MODE:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_RC_NOT_P_MODE);
                case RC_SWITCH:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_RC_SWITCH);
                case RC_PAUSE_STOP:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_RC_PAUSE_STOP);
                case RC_ONE_KEY_GO_HOME:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_RC_ONE_KEY_GO_HOME);
                case BATTERY_LOW_GO_HOME:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_BATTERY_LOW_GO_HOME);
                case BATTERY_SUPER_LOW_LANDING:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_BATTERY_SUPER_LOW_LANDING);
                case OSDK_LOST:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_OSDK_LOST);
                case NEAR_BOUNDARY:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_NEAR_BOUNDARY);
                case UNKNOWN:
                    return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_UNKNOWN);
            }
        }
        return context.getString(R.string.DronelinkDJI2_FlightControlAuthorityChangeReason_UNKNOWN);
    }

    public static RTKReferenceStationSource getRTKReferenceStationSource(final @Nullable dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource value) {
        if (value != null) {
            switch (value) {
                case NONE:
                    return RTKReferenceStationSource.NONE;
                case QX_NETWORK_SERVICE:
                    return RTKReferenceStationSource.QX_NETWORK_SERVICE;
                case BASE_STATION:
                    return RTKReferenceStationSource.BASE_STATION;
                case DPS:
                    return RTKReferenceStationSource.DPS;
                case CUSTOM_NETWORK_SERVICE:
                    return RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE;
                case NTRIP_NETWORK_SERVICE:
                    return RTKReferenceStationSource.NTRIP_NETWORK_SERVICE;
                case DOCK_BASE:
                    return RTKReferenceStationSource.DOCK_BASE;
                case RSV_RTK_SERVICE2:
                    return RTKReferenceStationSource.RSV_RTK_SERVICE2;
                case RSV_RTK_SERVICE3:
                    return RTKReferenceStationSource.RSV_RTK_SERVICE3;
                case UNKNOWN:
                    return RTKReferenceStationSource.UNKNOWN;
            }
        }
        return RTKReferenceStationSource.UNKNOWN;
    }

    public static dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource getRTKReferenceStationSource(final @Nullable RTKReferenceStationSource value) {
        if (value != null) {
            switch (value) {
                case NONE:
                    return dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource.NONE;
                case QX_NETWORK_SERVICE:
                    return dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource.QX_NETWORK_SERVICE;
                case BASE_STATION:
                    return dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource.BASE_STATION;
                case DPS:
                    return dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource.DPS;
                case CUSTOM_NETWORK_SERVICE:
                    return dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE;
                case NTRIP_NETWORK_SERVICE:
                    return dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource.NTRIP_NETWORK_SERVICE;
                case DOCK_BASE:
                    return dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource.DOCK_BASE;
                case RSV_RTK_SERVICE2:
                    return dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource.RSV_RTK_SERVICE2;
                case RSV_RTK_SERVICE3:
                    return dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource.RSV_RTK_SERVICE3;
                case UNKNOWN:
                    return dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource.UNKNOWN;
            }
        }
        return dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource.UNKNOWN;
    }
    public static RTKServiceState getRTKServiceState(final @Nullable dji.sdk.keyvalue.value.rtkbasestation.RTKServiceState value) {
        if (value != null) {
            switch (value) {
                case RTCM_CONNECTED:
                    return RTKServiceState.RTCM_CONNECTED;
                case RTCM_NORMAL:
                    return RTKServiceState.RTCM_NORMAL;
                case RTCM_USER_HAS_ACTIVATE:
                    return RTKServiceState.RTCM_USER_HAS_ACTIVATE;
                case RTCM_USER_ACCOUNT_EXPIRES_SOON:
                    return RTKServiceState.RTCM_USER_ACCOUNT_EXPIRES_SOON;
                case RTCM_USE_DEFAULT_MOUNT_POINT:
                    return RTKServiceState.RTCM_USE_DEFAULT_MOUNT_POINT;
                case RTCM_AUTH_FAILED:
                    return RTKServiceState.RTCM_AUTH_FAILED;
                case RTCM_USER_NOT_BOUNDED:
                    return RTKServiceState.RTCM_USER_NOT_BOUNDED;
                case RTCM_USER_NOT_ACTIVATED:
                    return RTKServiceState.RTCM_USER_NOT_ACTIVATED;
                case ACCOUNT_EXPIRED:
                    return RTKServiceState.ACCOUNT_EXPIRED;
                case RTCM_ILLEGAL_UTC_TIME:
                    return RTKServiceState.RTCM_ILLEGAL_UTC_TIME;
                case RTCM_SET_COORDINATE_FAILURE:
                    return RTKServiceState.RTCM_SET_COORDINATE_FAILURE;
                case RTCM_CONNECTING:
                    return RTKServiceState.RTCM_CONNECTING;
                case RTCM_ACTIVATED_FAILED:
                    return RTKServiceState.RTCM_ACTIVATED_FAILED;
                case DISABLED:
                    return RTKServiceState.DISABLED;
                case AIRCRAFT_DISCONNECTED:
                    return RTKServiceState.AIRCRAFT_DISCONNECTED;
                case CONNECTING:
                    return RTKServiceState.CONNECTING;
                case TRANSMITTING:
                    return RTKServiceState.TRANSMITTING;
                case LOGIN_FAILURE:
                    return RTKServiceState.LOGIN_FAILURE;
                case INVALID_REQUEST:
                    return RTKServiceState.INVALID_REQUEST;
                case ACCOUNT_ERROR:
                    return RTKServiceState.ACCOUNT_ERROR;
                case NETWORK_NOT_REACHABLE:
                    return RTKServiceState.NETWORK_NOT_REACHABLE;
                case SERVER_NOT_REACHABLE:
                    return RTKServiceState.SERVER_NOT_REACHABLE;
                case SERVICE_SUSPENSION:
                    return RTKServiceState.SERVICE_SUSPENSION;
                case DISCONNECTED:
                    return RTKServiceState.DISCONNECTED;
                case READY:
                    return RTKServiceState.READY;
                case SEND_GGA_NO_VALID_BASE:
                    return RTKServiceState.SEND_GGA_NO_VALID_BASE;
                case RTK_START_PROCESSING:
                    return RTKServiceState.RTK_START_PROCESSING;
                case UNKNOWN:
                    return RTKServiceState.UNKNOWN;
            }
        }
        return RTKServiceState.UNKNOWN;
    }
}