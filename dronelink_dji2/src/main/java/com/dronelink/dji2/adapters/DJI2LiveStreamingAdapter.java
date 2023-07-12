//  DJI2LiveStreamingAdapter.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 4/28/23.
//  Copyright © 2023 Dronelink. All rights reserved.
//
package com.dronelink.dji2.adapters;

import android.content.Context;

import com.dronelink.core.DatedValue;
import com.dronelink.core.adapters.LiveStreamingAdapter;
import com.dronelink.core.adapters.LiveStreamingStateAdapter;
import com.dronelink.core.command.Command;
import com.dronelink.core.command.CommandError;
import com.dronelink.core.kernel.command.livestreaming.LiveStreamingCommand;
import com.dronelink.core.kernel.command.livestreaming.ModuleLiveStreamingCommand;
import com.dronelink.core.kernel.command.livestreaming.RTMPLiveStreamingCommand;
import com.dronelink.core.kernel.command.livestreaming.RTMPSettingsLiveStreamingCommand;
import com.dronelink.core.kernel.core.Message;
import com.dronelink.dji2.DronelinkDJI2;
import com.dronelink.dji2.R;

import java.util.List;

import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.datacenter.livestream.LiveStreamSettings;
import dji.v5.manager.datacenter.livestream.LiveStreamType;
import dji.v5.manager.datacenter.livestream.settings.RtmpSettings;
import dji.v5.manager.interfaces.ILiveStreamManager;
import dji.v5.manager.interfaces.IMediaDataCenter;

public class DJI2LiveStreamingAdapter implements LiveStreamingAdapter {
    private final DJI2LiveStreamingStateAdapter state;

    public DJI2LiveStreamingAdapter(final Context context) {
        this.state = new DJI2LiveStreamingStateAdapter(context);
    }

    public void close() {
        state.close();
    }

    public DatedValue<LiveStreamingStateAdapter> getState() {
        return state.asDatedValue();
    }

    public List<Message> getStatusMessages() {
        return state.getStatusMessages();
    }

    public CommandError executeCommand(final Context context, final LiveStreamingCommand command, final Command.Finisher finished) {
        final IMediaDataCenter mediaDataCenter = MediaDataCenter.getInstance();
        if (mediaDataCenter == null) {
            return new CommandError(context.getString(R.string.MissionDisengageReason_live_streaming_unavailable_title));
        }

        final ILiveStreamManager liveStreamManager = mediaDataCenter.getLiveStreamManager();
        if (liveStreamManager == null) {
            return new CommandError(context.getString(R.string.MissionDisengageReason_live_streaming_unavailable_title));
        }

        if (command instanceof ModuleLiveStreamingCommand) {
            if (((ModuleLiveStreamingCommand) command).enabled) {
                liveStreamManager.startStream(DronelinkDJI2.createCompletionCallback(finished));
            } else {
                liveStreamManager.stopStream(DronelinkDJI2.createCompletionCallback(finished));
            }
            return null;
        }

        if (command instanceof RTMPLiveStreamingCommand) {
            return executeRTMPLiveStreamingCommand(context, liveStreamManager, (RTMPLiveStreamingCommand)command, finished);
        }

        return new CommandError(context.getString(R.string.MissionDisengageReason_command_type_unhandled) + ": " + command.type);
    }

    private CommandError executeRTMPLiveStreamingCommand(final Context context, final ILiveStreamManager liveStreamManager, final RTMPLiveStreamingCommand command, final Command.Finisher finished) {
        if (command instanceof RTMPSettingsLiveStreamingCommand) {
            final RTMPSettingsLiveStreamingCommand target = (RTMPSettingsLiveStreamingCommand) command;
            final RtmpSettings.Builder rtmpSettingsBuilder = new RtmpSettings.Builder();
            rtmpSettingsBuilder.setUrl(target.url);
            final LiveStreamSettings.Builder liveStreamSettingsBuilder = new LiveStreamSettings.Builder();
            liveStreamSettingsBuilder.setRtmpSettings(rtmpSettingsBuilder.build());
            liveStreamSettingsBuilder.setLiveStreamType(LiveStreamType.RTMP);
            liveStreamManager.setLiveStreamSettings(liveStreamSettingsBuilder.build());
            finished.execute(null);
            return null;
        }

        return new CommandError(context.getString(R.string.MissionDisengageReason_command_type_unhandled) + ": " + command.type);
    }
}