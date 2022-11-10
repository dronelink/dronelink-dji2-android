//  DJI2RemoteControllerAdapter.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 10/4/22.
//  Copyright © 2022 Dronelink. All rights reserved.
//
package com.dronelink.dji2.adapters;

import android.content.Context;

import com.dronelink.core.DatedValue;
import com.dronelink.core.adapters.RemoteControllerAdapter;
import com.dronelink.core.adapters.RemoteControllerStateAdapter;
import com.dronelink.core.command.Command;
import com.dronelink.core.command.CommandError;
import com.dronelink.core.kernel.command.remotecontroller.RemoteControllerCommand;
import com.dronelink.core.kernel.command.remotecontroller.TargetGimbalChannelRemoteControllerCommand;
import com.dronelink.dji2.R;

public class DJI2RemoteControllerAdapter implements RemoteControllerAdapter {
    private final DJI2RemoteControllerStateAdapter state;

    public DJI2RemoteControllerAdapter() {
        this.state = new DJI2RemoteControllerStateAdapter();
    }

    @Override
    public int getIndex() {
        return 0;
    }

    public void close() {
        state.close();
    }

    public DatedValue<RemoteControllerStateAdapter> getState() {
        return state.asDatedValue();
    }

    public CommandError executeCommand(final Context context, final RemoteControllerCommand command, final Command.Finisher finished) {
        if (command instanceof TargetGimbalChannelRemoteControllerCommand) {
            //TODO
            return null;
        }

        return new CommandError(context.getString(R.string.MissionDisengageReason_command_type_unhandled) + ": " + command.type);
    }
}