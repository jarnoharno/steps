package com.hiit.steps;

import android.content.Context;
import android.hardware.SensorEvent;
import android.util.Log;

public class Stroll {

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.d(TAG, "Stroll(" + System.identityHashCode(this) + "): " + msg);
    }

    private SensorLoop sensorLoop;
    private CachedBufferQueue<SensorEvent> sensorBuffer;
    //private WindowBuffer sensorBuffer;
    //private AILoop aiLoop;
    //private IOBuffer ioBuffer;
    private IOLoop ioLoop;
    private boolean running;

    Stroll(Context context, StepsListener stepsListener) {
        log("construct");
        //sensorBuffer = new WindowBuffer(1, 3);
        //ioBuffer = new IOBuffer(10);
        sensorBuffer = new CachedBufferQueue<SensorEvent>(256);
        sensorLoop = new SensorLoop(context, sensorBuffer, stepsListener);
        //aiLoop = new AILoop(sensorBuffer, ioBuffer);
        ioLoop = new IOLoop(context, sensorBuffer);
        //running = false;
    }

    public void start() {
        log("start");
        ioLoop.start();
        //aiLoop.start();
        sensorLoop.start();
        running = true;
    }

    public void stop() {
        log("stop");
        sensorLoop.stop();
        //aiLoop.stop();
        ioLoop.stop();
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public int getSamples() {
        return sensorLoop.getSamples();
    }

    public static Stroll start(Context context, StepsListener stepsListener) {
        Stroll stroll = new Stroll(context, stepsListener);
        stroll.start();
        return stroll;
    }
}
