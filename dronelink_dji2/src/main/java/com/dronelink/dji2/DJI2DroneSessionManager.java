//  DJI2DroneSessionManager.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 10/4/22.
//  Copyright © 2022 Dronelink. All rights reserved.
//
package com.dronelink.dji2;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import com.dronelink.core.DroneSession;
import com.dronelink.core.DroneSessionManager;
import com.dronelink.core.Dronelink;
import com.dronelink.core.LocaleUtil;
import com.dronelink.core.command.Command;
import com.dronelink.core.kernel.core.Message;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.common.register.DJISDKInitEvent;
import dji.v5.manager.KeyManager;
import dji.v5.manager.SDKManager;
import dji.v5.manager.interfaces.SDKManagerCallback;

public class DJI2DroneSessionManager implements DroneSessionManager {
    private static final String TAG = DJI2DroneSessionManager.class.getCanonicalName();

    private DJI2DroneSession session;
    private final AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private DJISDKInitEvent initEvent;
    private Boolean registered;
    private IDJIError registerError;
    private final List<Listener> listeners = new LinkedList<>();

    public DJI2DroneSessionManager() {
        initAppActivationManagerStateListener(0);
    }

    private void initAppActivationManagerStateListener(final int attempt) {
        SDKManager.getInstance().registerApp();
    }

    @Override
    public void addListener(final Listener listener) {
        listeners.add(listener);
        final DroneSession session = this.session;
        if (session != null) {
            listener.onOpened(session);
        }
    }

    @Override
    public void removeListener(final Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void closeSession() {
        final DroneSession previousSession = session;
        if (previousSession != null) {
            previousSession.close();
            session = null;

            for (final Listener listener : listeners) {
                listener.onClosed(previousSession);
            }
        }
    }

    @Override
    public void startRemoteControllerLinking(final Command.Finisher finisher) {
        KeyManager.getInstance().performAction(KeyTools.createKey(RemoteControllerKey.KeyRequestPairing), DronelinkDJI2.createCompletionCallbackWithParam(finisher));
    }

    @Override
    public void stopRemoteControllerLinking(final Command.Finisher finisher) {
        KeyManager.getInstance().performAction(KeyTools.createKey(RemoteControllerKey.KeyStopPairing), DronelinkDJI2.createCompletionCallbackWithParam(finisher));
    }

    @Override
    public DroneSession getSession() {
        return session;
    }

    @Override
    public List<Message> getStatusMessages() {
        final List<Message> messages = new ArrayList<>();

        if (initEvent != null) {
            switch (initEvent) {
                case START_TO_INITIALIZE:
                    messages.add(new Message(Dronelink.getInstance().context.getString(R.string.DJI2DroneSessionManager_initializing), Message.Level.WARNING));
                    break;

                case INITIALIZE_COMPLETE:
                    if (registerError != null) {
                        messages.add(new Message(Dronelink.getInstance().context.getString(R.string.DJI2DroneSessionManager_register_failed), registerError.description(), Message.Level.ERROR));
                    }
                    break;
            }
        }

        return messages;
    }

    public void register() {
        if (registered != null && registered) {
            return;
        }

        if (isRegistrationInProgress.compareAndSet(false, true)) {
            final DJI2DroneSessionManager self = this;
            AsyncTask.execute(() -> SDKManager.getInstance().init(Dronelink.getInstance().context, new SDKManagerCallback() {
                @Override
                public void onRegisterSuccess() {
                    registered = true;
                    Log.i(TAG, "DJI SDK registered successfully");

                    KeyManager.getInstance().listen(KeyTools.createKey(ProductKey.KeyProductType), this, new CommonCallbacks.KeyListener<ProductType>() {
                        @Override
                        public void onValueChange(final @Nullable ProductType oldValue, final @Nullable ProductType newValue) {
                            if (newValue == null || newValue == ProductType.UNKNOWN || newValue == ProductType.UNRECOGNIZED) {
                                closeSession();
                            }
                            else {
                                if (session != null) {
                                    closeSession();
                                }

                                session = new DJI2DroneSession(self);
                                for (final Listener listener : listeners) {
                                    listener.onOpened(session);
                                }
                            }
                        }
                    });
                }

                @Override
                public void onRegisterFailure(final IDJIError error) {
                    registered = false;
                    registerError = error;
                    Log.e(TAG, "DJI SDK registered with error: " + error.description());
                }

                @Override
                public void onProductDisconnect(int productId) {
                    //FIXME remove (favoring KeyManager.getInstance().listen(KeyTools.createKey(ProductKey.KeyProductType))
//                    closeSession();
                }

                @Override
                public void onProductConnect(final int productId) {
                    //FIXME remove (favoring KeyManager.getInstance().listen(KeyTools.createKey(ProductKey.KeyProductType))
//                    if (session != null) {
//                        closeSession();
//                    }
//
//                    session = new DJI2DroneSession(self);
//                    for (final Listener listener : listeners) {
//                        listener.onOpened(session);
//                    }
                }

                @Override
                public void onProductChanged(final int productId) {}

                @Override
                public void onInitProcess(final DJISDKInitEvent event, int totalProcess) {
                    initEvent = event;
                    if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                        SDKManager.getInstance().registerApp();
                    }
                }

                @Override
                public void onDatabaseDownloadProgress(final long current, final long total) {}
            }));
        }
    }
}
