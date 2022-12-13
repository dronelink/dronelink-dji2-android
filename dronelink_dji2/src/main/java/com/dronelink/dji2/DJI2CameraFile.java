//  DJI2CameraFile.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 10/5/22.
//  Copyright © 2022 Dronelink. All rights reserved.
//
package com.dronelink.dji2;

import android.annotation.SuppressLint;
import android.location.Location;

import com.dronelink.core.CameraFile;
import com.dronelink.core.kernel.core.Orientation3;
import com.dronelink.core.kernel.core.enums.CameraLensType;

import java.util.Date;

import dji.sdk.keyvalue.value.camera.GeneratedMediaFileInfo;
import dji.sdk.keyvalue.value.media.FileExifInfo;
import dji.v5.manager.datacenter.media.MediaFile;

public class DJI2CameraFile implements CameraFile {
    private final int channel;
    private final Date created;
    private final Location coordinate;
    private final Double altitude;
    private final Orientation3 orientation;
    public final GeneratedMediaFileInfo generatedMediaFileInfo;
    public MediaFile mediaFile;

    public DJI2CameraFile(final int channel, final GeneratedMediaFileInfo generatedMediaFileInfo, final Location coordinate, final Double altitude, final Orientation3 orientation) {
        this.channel = channel;
        this.generatedMediaFileInfo = generatedMediaFileInfo;
        this.created = new Date();
        this.coordinate = coordinate;
        this.altitude = altitude;
        this.orientation = orientation;
    }

    @Override
    public int getChannel() {
        return channel;
    }

    @Override
    public CameraLensType getLensType() {
        return DronelinkDJI2.getCameraLensType(generatedMediaFileInfo.dcf_type);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String getName() {
        //can't use MediaDataCenter.getInstance().getMediaManager() (per DJI's instructions)
        //because it causes the camera to error when the number of files is too great
        if (mediaFile != null) {
            return mediaFile.getFileName();
        }

        return generatedMediaFileInfo.getDir_no() + "_" + generatedMediaFileInfo.getFile_no() + "_" + generatedMediaFileInfo.getType();
//        final StringBuilder name = new StringBuilder("DJI_");
//
//        DateTime createTime = generatedMediaFileInfo.getCreateTime();
//        Date date = new Date(createTime.year, createTime.month, createTime.day, createTime.hour, createTime.minute, createTime.second);
////        Integer video_time_ms = generatedMediaFileInfo.video_time_ms;
////        if (video_time_ms != null) {
////            date.setTime(date.getTime() + (video_time_ms * 1000));
////        }
//        name.append(date.getYear());
//        name.append(String.format("%02d", date.getMonth()));
//        name.append(String.format("%02d", date.getDay()));
//        name.append(String.format("%02d", date.getHours()));
//        name.append(String.format("%02d", date.getMinutes()));
//        name.append(String.format("%02d", date.getSeconds() - 1));
//        name.append(String.format("_%04d", generatedMediaFileInfo.getFile_no()));
//        switch (generatedMediaFileInfo.dcf_type) {
//            case INFRARED:
//                name.append("_T");
//                break;
//            case ZOOM:
//                name.append("_Z");
//                break;
//            case WIDE:
//                name.append("_W");
//                break;
//            case SUPER_RESOLUTION:
//                name.append("_S");
//                break;
//            case VISIBLE:
//                name.append("_V");
//                break;
//            case SCREEN:
//            case UNKNOWN:
//                break;
//        }
//
//        switch (generatedMediaFileInfo.getType()) {
//            case JPEG:
//                name.append(".JPG");
//                break;
//            case DNG:
//                name.append(".DNG");
//                break;
//            case MOV:
//                name.append(".MOV");
//                break;
//            case MP4:
//                name.append(".MP4");
//                break;
//            case TIFF:
//                name.append(".TIFF");
//                break;
//            case PANORAMA:
//            case UL_CTRL_INFO:
//            case UL_CTRL_INFO_LZ4:
//            case SEQ:
//            case TIFF_SEQ:
//            case AUDIO:
//            case PAYLOAD_WIDGET_JSON:
//            case PHOTO_FOLDER:
//            case VIDEO_FOLDER:
//            case FOLDER_ATTR:
//            case LRF:
//            case THM:
//            case SCR:
//            case CNDG:
//            case UNKNOWN:
//                name.append(".").append(generatedMediaFileInfo.getType().name());
//                break;
//        }
//
//        return name.toString();
    }

    @Override
    public long getSize() {
        return mediaFile == null ? generatedMediaFileInfo.FileSize : mediaFile.getFileSize();
    }

    @Override
    public String getMetadata() {
        if (mediaFile == null) {
            return null;
        }

        final FileExifInfo exifInfo = mediaFile.getEXIFInfo();
        return exifInfo == null ? null : exifInfo.toString();
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Location getCoordinate() {
        return coordinate;
    }

    @Override
    public Double getAltitude() {
        return altitude;
    }

    @Override
    public Orientation3 getOrientation() {
        return orientation;
    }
}
