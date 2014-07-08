package com.hiit.steps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public class SensorLoop {

    private static final String SENSOR_LOOP_THREAD_NAME = "SensorLoop";

    private SensorManager sensorManager;
    private Sensor sensor;

    private HandlerThread thread;
    private Handler handler;

    private SensorBuffer buffer;

    private int steps;

    private static final int HANDLER_QUIT = 1;

    private Handler.Callback handlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what != HANDLER_QUIT)
                return false;
            buffer.put(SensorBuffer.EntryType.Quit);
            thread.quit();
            return false;
        }
    };

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            buffer.put(SensorBuffer.EntryType.Accelerator, event.timestamp, event.values);
            ++steps;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    SensorLoop(Context context, SensorBuffer buffer) {
        this.buffer = buffer;
        thread = new HandlerThread(SENSOR_LOOP_THREAD_NAME);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        steps = 0;
    }

    public void start() {
        thread.start();
        handler = new Handler(thread.getLooper(), handlerCallback);
        sensorManager.registerListener(sensorEventListener, sensor,
                SensorManager.SENSOR_DELAY_FASTEST, handler);
    }

    public void stop() {
        sensorManager.unregisterListener(sensorEventListener);
        handler.sendMessage(handler.obtainMessage(HANDLER_QUIT));
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getSteps() {
        return steps;
    }
}
