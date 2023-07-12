//  DJI2LiveStreamingStateAdapter.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 4/28/23.
//  Copyright © 2023 Dronelink. All rights reserved.
//
package com.dronelink.dji2.adapters;

import android.annotation.SuppressLint;
import android.content.Context;

import com.dronelink.core.DatedValue;
import com.dronelink.core.adapters.LiveStreamingStateAdapter;
import com.dronelink.core.kernel.core.Message;
import com.dronelink.dji2.DJI2ListenerGroup;
import com.dronelink.dji2.DronelinkDJI2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dji.v5.common.error.IDJIError;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.datacenter.livestream.LiveStreamStatus;
import dji.v5.manager.datacenter.livestream.LiveStreamStatusListener;
import dji.v5.manager.interfaces.ILiveStreamManager;
import dji.v5.manager.interfaces.IMediaDataCenter;

public class DJI2LiveStreamingStateAdapter implements LiveStreamingStateAdapter, LiveStreamStatusListener {
    private final Context context;
    private final DJI2ListenerGroup listeners = new DJI2ListenerGroup();

    private LiveStreamStatus status;
    private IDJIError error;

    public DJI2LiveStreamingStateAdapter(final Context context) {
        this.context = context;

        final IMediaDataCenter mediaDataCenter = MediaDataCenter.getInstance();
        if (mediaDataCenter != null) {
            final ILiveStreamManager liveStreamManager = mediaDataCenter.getLiveStreamManager();
            if (liveStreamManager != null) {
                liveStreamManager.addLiveStreamStatusListener(this);
            }
        }
    }

    public void close() {
        listeners.cancelAll();
        final IMediaDataCenter mediaDataCenter = MediaDataCenter.getInstance();
        if (mediaDataCenter != null) {
            final ILiveStreamManager liveStreamManager = mediaDataCenter.getLiveStreamManager();
            if (liveStreamManager != null) {
                liveStreamManager.removeLiveStreamStatusListener(this);
            }
        }
    }

    public DatedValue<LiveStreamingStateAdapter> asDatedValue() {
        return new DatedValue<>(this, new Date());
    }

    @Override
    public boolean isEnabled() {
        final IMediaDataCenter mediaDataCenter = MediaDataCenter.getInstance();
        if (mediaDataCenter != null) {
            final ILiveStreamManager liveStreamManager = mediaDataCenter.getLiveStreamManager();
            if (liveStreamManager != null) {
                return liveStreamManager.isStreaming();
            }
        }

        //this doesn't seem reliable?
        return status != null && status.isStreaming();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public List<Message> getStatusMessages() {
        final List<Message> messages = new ArrayList<>();
        Message message = error == null ? null : DronelinkDJI2.getMessage(context, error);
        if (message != null) {
            messages.add(message);
        }

        return messages;
    }

    @Override
    public void onLiveStreamStatusUpdate(final LiveStreamStatus status) {
        this.status = status;
    }

    @Override
    public void onError(final IDJIError error) {
        this.error = error;
    }
}