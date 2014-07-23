package com.hiit.steps;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class StepsService extends Service implements StepsListener {

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.i(TAG, "StepsService(" + Thread.currentThread().getId() + "): " + msg);
    }

    private static final int SERVICE_NOTIFICATION_ID = 1;

    public static final int MSG_STOP = 1;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STOP:
                    stop();
                    break;
            }
        }
    };

    private Messenger messenger = new Messenger(handler);

    public static final String ACTION_BIND_LOCAL = "intent.action.BIND_LOCAL";
    public static final String EXTRA_CALLBACK = "intent.extra.CALLBACK";

    public static final int MSG_CALLBACK_STOPPED = 1;

    private List<Messenger> callbacks = new ArrayList<Messenger>();

    private void sendCallback(int msg) {
        for (int i = callbacks.size() - 1; i >= 0; --i) {
            Message message = Message.obtain();
            message.what = msg;
            try {
                callbacks.get(i).send(message);
            } catch (RemoteException e) {
                // callback is dead
                callbacks.remove(i);
            }
        }
    }

    private void addCallback(Messenger callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    private void addCallback(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null)
            return;
        Messenger callback = (Messenger) bundle.getParcelable(EXTRA_CALLBACK);
        if (callback == null)
            return;
        addCallback(callback);
    }

    @Override
    public IBinder onBind(Intent intent) {
        addCallback(intent);
        String action = intent.getAction();
        if (action != null && action.equals(ACTION_BIND_LOCAL)) {
            return localBinder;
        }
        return messenger.getBinder();
    }

    // bind service

    public static class Local {

        private StepsService service;

        Local(IBinder service) {
            this.service = ((LocalBinder)service).getService();
        }

        public void stop() {
            service.stop();
        }

        public boolean addListener(StepsListener listener) {
            return service.addListener(listener);
        }

        public boolean removeListener(StepsListener listener) {
            return service.removeListener(listener);
        }

        public boolean isRunning() {
            return service.isRunning();
        }

        public int getSamples() {
            return service.getSamples();
        }

        public int getSteps() {
            return service.getSteps();
        }
    }

    // remotely only stop() is available

    public static class Remote {
        private Messenger messenger;
        Remote(IBinder service) {
            messenger = new Messenger(service);
        }

        public void stop() {
            Message message = Message.obtain();
            message.what = MSG_STOP;
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean bindLocal(Context context, ServiceConnection serviceConnection, Messenger callback) {
        Intent intent = new Intent(context, StepsService.class);
        intent.setAction(ACTION_BIND_LOCAL);
        if (callback != null) {
            intent.putExtra(EXTRA_CALLBACK, callback);
        }
        return context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public static boolean bindRemote(Context context, ServiceConnection serviceConnection, Messenger callback) {
        Intent intent = new Intent(context, StepsService.class);
        if (callback != null) {
            intent.putExtra(EXTRA_CALLBACK, callback);
        }
        return context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public class LocalBinder extends Binder {
        public StepsService getService() {
            return StepsService.this;
        }
    }

    private LocalBinder localBinder = new LocalBinder();

    private List<StepsListener> clients = new ArrayList<StepsListener>();

    private Stroll stroll;

    @Override
    public synchronized void onSampleEvent() {
        for (StepsListener client : clients) {
            client.onSampleEvent();
        }
    }

    @Override
    public synchronized void onStepEvent() {
        for (StepsListener client : clients) {
            client.onStepEvent();
        }
    }

    public synchronized boolean addListener(StepsListener client) {
        if (clients.contains(client))
            return false;
        clients.add(client);
        return true;
    }

    public synchronized boolean removeListener(StepsListener client) {
        if (clients.contains(client))
            return false;
        clients.remove(client);
        return true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false; // don't call onRebind
    }

    @Override
    public void onDestroy() {
        // in case service is destroyed without explicit stop
        if (stroll != null && stroll.isRunning())
            stroll.stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        addCallback(intent);
        // ridiculous hack to make sure the system has created a remote IBinder
        // for the service
        ServiceConnection dummyConnection = new AbstractServiceConnection();
        if (bindService(new Intent(this, StepsService.class), dummyConnection,
                Context.BIND_AUTO_CREATE)) {
            unbindService(dummyConnection);
        }
        start();
        return START_STICKY;
    }

    public void start() {
        log("start");
        stroll = Stroll.start(this, this);
        setForeground();
    }

    public void stop() {
        log("stop");
        if (stroll == null)
            return;
        stroll.stop();
        stopForeground(true);
        stopSelf();
        sendCallback(MSG_CALLBACK_STOPPED);
    }

    public boolean isRunning() {
        return stroll != null && stroll.isRunning();
    }

    public int getSamples() {
        return stroll == null ? 0 : stroll.getSamples();
    }

    public int getSteps() {
        return stroll == null ? 0 : stroll.getSteps();
    }

    private void setForeground() {
        final Intent notificationIntent = new Intent(this, StepsActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentIntent(contentIntent)
                .setContentTitle(getText(R.string.service_notification_title))
                .setContentText(getText(R.string.service_notification_message))
                .setTicker(getText(R.string.service_notification))
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();

        startForeground(SERVICE_NOTIFICATION_ID, notification);
    }

}
