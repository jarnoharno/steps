package com.hiit.steps;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StepsService extends Service implements StepsCallback {

    private List<ILifecycleCallback> lifecycleCallbacks = new ArrayList<ILifecycleCallback>();

    private synchronized void emitStop(int samples, String outputFile) {
        for (int i = lifecycleCallbacks.size() - 1; i >= 0; --i) {
            try {
                Log.d("Steps", "messaging callback " + i);
                lifecycleCallbacks.get(i).stopped(samples, outputFile);
            } catch (RemoteException e) {
                // callback is dead
            } finally {
                // remove lifecycle callbacks after stop()
                lifecycleCallbacks.remove(i);
            }
        }
    }

    private synchronized boolean addLifecycleCallback(ILifecycleCallback callback) {
        if (lifecycleCallbacks.contains(callback))
            return false;
        return lifecycleCallbacks.add(callback);
    }

    private synchronized boolean removeLifecycleCallback(ILifecycleCallback callback) {
        return lifecycleCallbacks.remove(callback);
    }

    private void addLifecycleCallback(Bundle bundle) {
        if (bundle == null)
            return;
        LifecycleCallback callback = bundle.getParcelable(Configuration.EXTRA_LIFECYCLE_CALLBACK);
        if (callback == null)
            return;
        addLifecycleCallback(callback.getTarget());
    }

    private List<IStepsCallback> stepsCallbacks = new ArrayList<IStepsCallback>();

    private synchronized boolean addStepsCallback(IStepsCallback callback) {
        if (stepsCallbacks.add(callback)) {
            try {
                callback.onStepEvent(getSteps());
                callback.onSampleEvent(getSamples());
            } catch (RemoteException e) {
                stepsCallbacks.remove(callback);
                return false;
            }
            return true;
        }
        return false;
    }

    private synchronized boolean removeStepsCallback(IStepsCallback callback) {
        return stepsCallbacks.remove(callback);
    }

    private final IStepsService.Stub binder = new IStepsService.Stub() {

        @Override
        public boolean addStepsCallback(IStepsCallback callback) throws RemoteException {
            return StepsService.this.addStepsCallback(callback);
        }

        @Override
        public boolean removeStepsCallback(IStepsCallback callback) throws RemoteException {
            return StepsService.this.removeStepsCallback(callback);
        }

        @Override
        public boolean addLifecycleCallback(ILifecycleCallback callback) throws RemoteException {
            return StepsService.this.addLifecycleCallback(callback);
        }

        @Override
        public boolean removeLifecycleCallback(ILifecycleCallback callback) throws RemoteException {
            return StepsService.this.removeLifecycleCallback(callback);
        }

        @Override
        public boolean stop() throws RemoteException {
            return StepsService.this.stop();
        }

        @Override
        public int getSamples() throws RemoteException {
            return StepsService.this.getSamples();
        }

        @Override
        public int getSteps() throws RemoteException {
            return StepsService.this.getSteps();
        }

        @Override
        public boolean isRunning() throws RemoteException {
            return StepsService.this.isRunning();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false; // don't call onRebind
    }

    public static boolean bind(Context context, ServiceConnection serviceConnection) {
        Intent intent = new Intent(context, StepsService.class);
        return context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private Stroll stroll;

    @Override
    public void onStepEvent(int steps) {
        for (int i = stepsCallbacks.size() - 1; i >= 0; --i) {
            try {
                stepsCallbacks.get(i).onStepEvent(steps);
            } catch (RemoteException e) {
                stepsCallbacks.remove(i);
            }
        }
    }

    @Override
    public void onSampleEvent(int samples) {
        for (int i = stepsCallbacks.size() - 1; i >= 0; --i) {
            try {
                stepsCallbacks.get(i).onSampleEvent(samples);
            } catch (RemoteException e) {
                stepsCallbacks.remove(i);
            }
        }
    }

    @Override
    public void onDestroy() {
        // in case service is destroyed without explicit stop
        if (stroll != null && stroll.isRunning())
            stroll.stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        addLifecycleCallback(bundle);
        // ridiculous hack to make sure the system has created a remote IBinder
        // for the service
        ServiceConnection dummyConnection = new AbstractServiceConnection();
        if (bindService(new Intent(this, StepsService.class), dummyConnection,
                Context.BIND_AUTO_CREATE)) {
            unbindService(dummyConnection);
        }
        start(bundle);
        return START_STICKY;
    }

    public void start(Bundle bundle) {
        stroll = new Stroll(this, this, done);

        // configuration
        if (bundle != null) {
            Long maxTimestamp = (Long) bundle.get(Configuration.EXTRA_MAX_TIMESTAMP);
            if (maxTimestamp != null) {
                stroll.setMaxTimestamp(maxTimestamp.longValue());
            }
            Integer rateUs = (Integer) bundle.get(Configuration.EXTRA_RATE_US);
            if (rateUs != null) {
                stroll.setRateUs(rateUs.intValue());
            }
        }

        stroll.start();
        setForeground();
    }

    public boolean stop() {
        if (!isRunning())
            return false;
        stroll.stop();
        return true;
    }

    private Runnable done = new Runnable() {
        @Override
        public void run() {
            stopForeground(true);
            stopSelf();
            emitStop(getRows(), getOutputFile().toString());
            stroll = null;
        }
    };

    public boolean isRunning() {
        return stroll != null && stroll.isRunning();
    }

    public int getSamples() {
        return stroll == null ? 0 : stroll.getSamples();
    }

    public int getSteps() {
        return stroll == null ? 0 : stroll.getSteps();
    }

    public int getRows() {
        return stroll == null ? 0 : stroll.getRows();
    }

    public File getOutputFile() {
        return stroll == null ? null : stroll.getOutputFile();
    }

    private static final int SERVICE_NOTIFICATION_ID = 1;

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
