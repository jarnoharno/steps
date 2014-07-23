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
    private int[] sensorTypes = {
            Sensor.TYPE_ACCELEROMETER,
            //Sensor.TYPE_GYROSCOPE,
            //Sensor.TYPE_MAGNETIC_FIELD,
            //Sensor.TYPE_GYROSCOPE_UNCALIBRATED, // doesn't work for galaxy s3???
            //Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED
    };
    private Sensor[] sensors = new Sensor[sensorTypes.length];

    private HandlerThread thread = new HandlerThread(SENSOR_LOOP_THREAD_NAME);
    private Handler handler;
    private StepsListener listener;

    private AtomicInteger samples = new AtomicInteger();

    private CachedIntArrayBufferQueue queue;

    private static final int HANDLER_QUIT = 1;

    private Handler.Callback handlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what != HANDLER_QUIT)
                return false;
            queue.quit();
            thread.quit();
            return false;
        }
    };

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            samples.incrementAndGet();
            listener.onSampleEvent();
            SensorEventSerializer.toIntArray(event, queue.obtain().data);
            queue.put();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            String acc = "UNDEFINED";
            switch (accuracy) {
                case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                    acc = "HIGH";
                    break;
                case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                    acc = "MEDIUM";
                    break;
                case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                    acc = "LOW";
                    break;
            }
            log("onAccuracyChanged: " + acc);
        }
    };

    SensorLoop(Context context,
               CachedIntArrayBufferQueue queue,
               StepsListener stepsListener) {
        log("construct");
        this.queue = queue;
        listener = stepsListener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        for (int i = 0; i < sensorTypes.length; ++i) {
            sensors[i] = sensorManager.getDefaultSensor(sensorTypes[i]);
        }
    }

    public void start() {
        log("start");
        thread.start();
        handler = new Handler(thread.getLooper(), handlerCallback);
        for (Sensor sensor: sensors) {
            sensorManager.registerListener(sensorEventListener, sensor,
                    SensorManager.SENSOR_DELAY_FASTEST, handler);
            log("listening " + sensor.getName() +
                    ", minDelay: " + sensor.getMinDelay() +
                    " μs, maximumRange: " + sensor.getMaximumRange() +
                    ", resolution: " + sensor.getResolution());
        }
    }

    public void stop() {
        log("stop");
        sensorManager.unregisterListener(sensorEventListener);
        handler.sendMessage(handler.obtainMessage(HANDLER_QUIT));
        try {
            thread.join();
            log(samples.get() + " samples sent");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getSamples() {
        return samples.get();
    }

    private void logSensors() {
        for (Sensor sensor : sensorManager.getSensorList(Sensor.TYPE_ALL)) {
            log(sensor.toString());
        }
    }
}
