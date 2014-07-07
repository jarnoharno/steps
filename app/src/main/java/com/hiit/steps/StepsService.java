package com.hiit.steps;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

public class StepsService extends Service {

    private static final String TAG = "Steps";
    private static final int SERVICE_NOTIFICATION_ID = 1;

    public class LocalBinder extends Binder {
        StepsService getService() {
            return StepsService.this;
        }
    }

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    SensorManager sensorManager = null;
    Sensor sensor = null;
    StepsListener listener = null;

    Walk walk = null;

    public void onStep() {
        // atomic, no read-ahead
        StepsListener l = listener;
        if (l == null)
            return;
        l.onStep();
    }

    private void log(String msg) {
        Log.d(TAG, "Service(" + System.identityHashCode(this) + "): " + msg);
    }

    @Override
    public void onCreate() {
        log("onCreate");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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
        if (walk == null)
            return;
        if (walk.isRunning())
            walk.stop();
    }

    public void start() {
        walk = Walk.start(this);
        setForeground();
    }

    public void stop() {
        if (walk == null)
            return;
        walk.stop();
        stopForeground(true);
    }

    public void setListener(StepsListener listener) {
        this.listener = listener;
    }

    public boolean isRunning() {
        return walk != null && walk.isRunning();
    }

    public int getSteps() {
        return walk == null ? 0 : walk.getSteps();
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
