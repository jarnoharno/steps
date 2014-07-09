package com.hiit.steps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;

public class Stroll {

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.d(TAG, "Stroll(" + System.identityHashCode(this) + "): " + msg);
    }

    private SensorLoop sensorLoop;
    private WindowBuffer sensorBuffer;
    //private WindowBuffer sensorBuffer;
    //private AlgoLoop algoLoop;
    //private IOBuffer ioBuffer;
    private IOLoop ioLoop;
    private boolean running;

    Stroll(Context context, StepsListener stepsListener) {
        log("construct");
        //sensorBuffer = new WindowBuffer(1, 3);
        //ioBuffer = new IOBuffer(10);
        sensorBuffer = new WindowBuffer(9, 256, 0);
        sensorLoop = new SensorLoop(context, sensorBuffer, stepsListener);
        //algoLoop = new AlgoLoop(sensorBuffer, ioBuffer);
        ioLoop = new IOLoop(context, sensorBuffer);
        //running = false;
    }

    public void start() {
        log("start");
        ioLoop.start();
        //algoLoop.start();
        sensorLoop.start();
        running = true;
    }

    public void stop() {
        log("stop");
        sensorLoop.stop();
        //algoLoop.stop();
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
