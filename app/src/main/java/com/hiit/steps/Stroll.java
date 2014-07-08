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

    private SensorLoop sensorLoop;
    private SensorBuffer sensorBuffer;
    private AlgoLoop algoLoop;
    private IOBuffer ioBuffer;
    private IOLoop ioLoop;
    private boolean running;

    Stroll(Context context) {
        sensorBuffer = new SensorBuffer(1, 3);
        ioBuffer = new IOBuffer(10);
        sensorLoop = new SensorLoop(context, sensorBuffer);
        algoLoop = new AlgoLoop(sensorBuffer, ioBuffer);
        ioLoop = new IOLoop(context, sensorBuffer, ioBuffer);
        running = false;
    }

    public void start() {
        ioLoop.start();
        algoLoop.start();
        sensorLoop.start();
        running = true;
    }

    public void stop() {
        sensorLoop.stop();
        algoLoop.stop();
        ioLoop.stop();
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public int getSteps() {
        return sensorLoop.getSteps();
    }

    public static Stroll start(Context context) {
        Stroll stroll = new Stroll(context);
        stroll.start();
        return stroll;
    }
}
