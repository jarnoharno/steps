package com.hiit.steps;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class StepsService extends Service implements StepsListener {

    private static final String TAG = "Steps";
    private static final int SERVICE_NOTIFICATION_ID = 1;

    public class LocalBinder extends Binder {
        StepsService getService() {
            return StepsService.this;
        }
    }

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    StepsListener listener;

    Stroll stroll;

    @Override
    public void onSampleEvent() {
        // atomic, no read-ahead
        StepsListener l = listener;
        if (l == null)
            return;
        l.onSampleEvent();
    }

    @Override
    public void onStepEvent() {
        // atomic, no read-ahead
        StepsListener l = listener;
        if (l == null)
            return;
        l.onStepEvent();
    }

    private void log(String msg) {
        Log.d(TAG, "Service(" + System.identityHashCode(this) + "): " + msg);
    }

    @Override
    public void onCreate() {
        log("onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("onStartCommand");
        return START_STICKY;
    }
    @Override
    public IBinder onBind(Intent intent) {
        log("onBind");
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        log("onUnbind");
        return false; // don't call onRebind
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        // in case service gets destroyed without explicit stop()
        if (stroll == null)
            return;
        if (stroll.isRunning())
            stroll.stop();
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
    }

    public void setListener(StepsListener listener) {
        this.listener = listener;
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
