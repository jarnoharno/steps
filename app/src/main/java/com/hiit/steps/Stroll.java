package com.hiit.steps;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;

public class Stroll {

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.d(TAG, "Stroll(" + System.identityHashCode(this) + "): " + msg);
    }

    private static final String WAKE_LOCK_TAG = "StepsServiceWakeLockTag";

    private Runnable serviceDone;
    private SensorLoop sensorLoop;
    private CachedIntArrayBufferQueue sensorQueue;
    private AILoop aiLoop;
    private CachedIntArrayBufferQueue ioQueue;
    private IOLoop ioLoop;
    private boolean running;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    public Stroll(Context context, StepsCallback stepsCallback, Runnable serviceDone) {
        log("construct");
        this.serviceDone = serviceDone;
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

        sensorQueue = new CachedIntArrayBufferQueue(20, 14); // ~400 ms lag with accelerometer at 50 Hz
        ioQueue = new CachedIntArrayBufferQueue(1000, 14);
        ioLoop = new IOLoop(context, ioQueue, done);
        aiLoop = new AILoop(context, sensorQueue, ioQueue, stepsCallback);
        sensorLoop = new SensorLoop(context, sensorQueue, stepsCallback);
        running = false;
    }

    public void setMaxTimestamp(long maxTimestamp) {
        sensorLoop.setMaxTimestamp(maxTimestamp);
    }

    public long getMaxTimestamp() {
        return sensorLoop.getMaxTimestamp();
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
    }

    private Runnable done = new Runnable() {
        @Override
        public void run() {
            log("done");
            wakeLock.release();
            running = false;
            if (serviceDone != null) {
                serviceDone.run();
            }
        }
    };

    public boolean isRunning() {
        return running;
    }

    public int getSamples() {
        return sensorLoop.getSamples();
    }

    public int getSteps() {
        return aiLoop.getSteps();
    }

    public void setSampleRate(int rateUs) {
        sensorLoop.setSampleRate(rateUs);
    }

    public int getRateUs() {
        return sensorLoop.getSampleRate();
    }

    public int getRows() {
        return ioLoop.getRows();
    }

    public File getOutputFile() {
        return ioLoop.getFile();
    }

    public void setMaWindow(int window) {
        aiLoop.setMaWindow(window);
    }

    public void setStdWindow(int window) {
        aiLoop.setStdWindow(window);
    }

    public void setResampleRate(long resampleRateUs) {
        aiLoop.setResampleRate(resampleRateUs);
    }

    public void setStdThreshold(float threshold) {
        aiLoop.setStdThreshold(threshold);
    }
}
