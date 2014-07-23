package com.hiit.steps;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class StepsService extends Service implements StepsListener {

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.i(TAG, "StepsService(" + Thread.currentThread().getId() + "): " + msg);
    }

    private static final int SERVICE_NOTIFICATION_ID = 1;

    public class LocalBinder extends Binder {
        public StepsService getService() {
            return StepsService.this;
        }
    }

    private LocalBinder binder = new LocalBinder();

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
    public IBinder onBind(Intent intent) {
        return binder;
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
        // ridiculous hack to make sure system has created an IBinder for the
        // service
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
