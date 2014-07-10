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

        // The following rates were detected with SENSOR_DELAY_FASTEST on galaxy s3:
        //
        // Sensor.TYPE_ACCELEROMETER                 10 Hz
        // Sensor.TYPE_GYROSCOPE                     5 Hz
        // Sensor.TYPE_MAGNETIC_FIELD                10 Hz
        // Sensor.TYPE_GYROSCOPE_UNCALIBRATED        NA
        // Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED   10 Hz

        sensorQueue = new CachedIntArrayBufferQueue(100, 10); // ~200 ms lag with all sensors on
        ioQueue = new CachedIntArrayBufferQueue(1000, 10); // ~2 s lag with all sensors on
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
