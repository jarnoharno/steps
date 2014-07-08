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

import java.util.concurrent.atomic.AtomicInteger;

public class SensorLoop {

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.d(TAG, "SensorLoop(" + System.identityHashCode(this) + "): " + msg);
    }

    private static final String SENSOR_LOOP_THREAD_NAME = "SensorLoop";

    private SensorManager sensorManager;
    private Sensor sensor;

    private HandlerThread thread = new HandlerThread(SENSOR_LOOP_THREAD_NAME);
    private Handler handler;
    private StepsListener listener;

    private AtomicInteger samples = new AtomicInteger();

    private static final int HANDLER_QUIT = 1;

    private Handler.Callback handlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what != HANDLER_QUIT)
                return false;
            thread.quit();
            return false;
        }
    };

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            //log("onSensorChanged");
            samples.incrementAndGet();
            listener.onStepEvent();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    SensorLoop(Context context, StepsListener stepsListener) {
        log("construct");
        listener = stepsListener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void start() {
        log("start");
        thread.start();
        handler = new Handler(thread.getLooper(), handlerCallback);
        sensorManager.registerListener(sensorEventListener, sensor,
                SensorManager.SENSOR_DELAY_FASTEST, handler);
    }

    public void stop() {
        log("stop");
        sensorManager.unregisterListener(sensorEventListener);
        handler.sendMessage(handler.obtainMessage(HANDLER_QUIT));
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getSamples() {
        return samples.get();
    }
}
