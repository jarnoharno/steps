package com.hiit.steps;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class Stroll {

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.d(TAG, "Stroll(" + System.identityHashCode(this) + "): " + msg);
    }

    private static final String WAKE_LOCK_TAG = "StepsServiceWakeLockTag";

    private SensorLoop sensorLoop;
    private CachedIntArrayBufferQueue sensorQueue;
    private AILoop aiLoop;
    private CachedIntArrayBufferQueue ioQueue;
    private IOLoop ioLoop;
    private boolean running;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    Stroll(Context context, StepsListener stepsListener) {
        log("construct");
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG);

        sensorQueue = new CachedIntArrayBufferQueue(100, 10); // ~1 s lag
        ioQueue = new CachedIntArrayBufferQueue(1000, 10); // ~10 s lag
        ioLoop = new IOLoop(context, ioQueue);
        aiLoop = new AILoop(context, sensorQueue, ioQueue, stepsListener);
        sensorLoop = new SensorLoop(context, sensorQueue, stepsListener);
        running = false;
    }

    public void start() {
        log("start");
        wakeLock.acquire();
        ioLoop.start();
        aiLoop.start();
        sensorLoop.start();
        running = true;
    }

    public void stop() {
        log("stop");
        sensorLoop.stop();
        aiLoop.stop();
        ioLoop.stop();
        wakeLock.release();
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public int getSamples() {
        return sensorLoop.getSamples();
    }

    public int getSteps() {
        return aiLoop.getSteps();
    }

    public static Stroll start(Context context, StepsListener stepsListener) {
        Stroll stroll = new Stroll(context, stepsListener);
        stroll.start();
        return stroll;
    }
}
