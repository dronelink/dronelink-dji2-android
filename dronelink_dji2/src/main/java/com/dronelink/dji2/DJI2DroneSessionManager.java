//  DJI2DroneSessionManager.java
//  DronelinkDJI2
//
//  Created by Jim McAndrew on 10/4/22.
//  Copyright © 2022 Dronelink. All rights reserved.
//
package com.dronelink.dji2;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dronelink.core.DroneSession;
import com.dronelink.core.DroneSessionManager;
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
import dji.v5.manager.aircraft.flysafe.FlySafeNotificationListener;
import dji.v5.manager.aircraft.flysafe.FlyZoneManager;
import dji.v5.manager.aircraft.flysafe.info.FlySafeReturnToHomeInformation;
import dji.v5.manager.aircraft.flysafe.info.FlySafeSeriousWarningInformation;
import dji.v5.manager.aircraft.flysafe.info.FlySafeTipInformation;
import dji.v5.manager.aircraft.flysafe.info.FlySafeWarningInformation;
import dji.v5.manager.aircraft.flysafe.info.FlyZoneInformation;
import dji.v5.manager.aircraft.uas.AreaStrategy;
import dji.v5.manager.aircraft.uas.UASRemoteIDManager;
import dji.v5.manager.aircraft.uas.UASRemoteIDStatus;
import dji.v5.manager.interfaces.SDKManagerCallback;

public class DJI2DroneSessionManager implements DroneSessionManager {
    private static final String TAG = DJI2DroneSessionManager.class.getCanonicalName();

    private final Context context;
    private DJI2DroneSession session;
    private final AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private DJISDKInitEvent initEvent;
    private Boolean registered;
    private IDJIError registerError;
    private UASRemoteIDStatus uasRemoteIDStatus;
    private FlySafeWarningInformation flySafeWarningInformation;
    private FlySafeSeriousWarningInformation flySafeSeriousWarningInformation;

    private final List<Listener> listeners = new LinkedList<>();
    public DJI2DroneSessionManager(final Context context) {
        this.context = context;
    }

    @Override
    public void setLocale(final String locale) {
        LocaleUtil.selectedLocale = locale;
        LocaleUtil.applyLocalizedContext(context, LocaleUtil.selectedLocale);
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
                    messages.add(new Message(context.getString(R.string.DJI2DroneSessionManager_initializing), Message.Level.WARNING));
                    break;

                case INITIALIZE_COMPLETE:
                    if (registerError != null) {
                        messages.add(new Message(context.getString(R.string.DJI2DroneSessionManager_register_failed), registerError.description(), Message.Level.ERROR));
                    }
                    break;
            }
        }

        final UASRemoteIDStatus uasRemoteIDStatus = this.uasRemoteIDStatus;
        if (uasRemoteIDStatus != null) {
            final Message status = DronelinkDJI2.getMessage(context, uasRemoteIDStatus);
            if (status != null) {
                messages.add(status);
            }
        }

        final FlySafeWarningInformation flySafeWarningInformation = this.flySafeWarningInformation;
        if (flySafeWarningInformation != null) {
            final Message message = DronelinkDJI2.getMessage(flySafeWarningInformation);
            if (message != null) {
                messages.add(message);
            }
        }

        final FlySafeSeriousWarningInformation flySafeSeriousWarningInformation = this.flySafeSeriousWarningInformation;
        if (flySafeSeriousWarningInformation != null) {
            messages.add(new Message(flySafeSeriousWarningInformation.getDescription(), Message.Level.ERROR));
        }

        return messages;
    }

    public void register(final Context context) {
        if (registered != null && registered) {
            return;
        }

        if (isRegistrationInProgress.compareAndSet(false, true)) {
            final DJI2DroneSessionManager self = this;
            AsyncTask.execute(() -> SDKManager.getInstance().init(context, new SDKManagerCallback() {
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

                                session = new DJI2DroneSession(context, self);
                                for (final Listener listener : listeners) {
                                    listener.onOpened(session);
                                }
                            }
                        }
                    });

                    UASRemoteIDManager.getInstance().addUASRemoteIDStatusListener(status -> {
                        uasRemoteIDStatus = status;
                    });

                    FlyZoneManager.getInstance().addFlySafeNotificationListener(new FlySafeNotificationListener() {
                        @Override
                        public void onWarningNotificationUpdate(@NonNull FlySafeWarningInformation info) {
                            flySafeWarningInformation = info;
                        }

                        @Override
                        public void onSeriousWarningNotificationUpdate(@NonNull FlySafeSeriousWarningInformation info) {
                            flySafeSeriousWarningInformation = info;
                        }

                        @Override
                        public void onTipNotificationUpdate(@NonNull FlySafeTipInformation info) {}
                        @Override
                        public void onReturnToHomeNotificationUpdate(@NonNull FlySafeReturnToHomeInformation info) {}
                        @Override
                        public void onSurroundingFlyZonesUpdate(@NonNull List<FlyZoneInformation> infos) {}
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
//                    session = new DJI2DroneSession(context, self);
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
