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
    private Sensor sensorAccelerometer;
    private Sensor sensorGyroscope;
    private Sensor sensorMagnetic;

    private HandlerThread thread = new HandlerThread(SENSOR_LOOP_THREAD_NAME);
    private Handler handler;
    private StepsListener listener;

    private AtomicInteger samples = new AtomicInteger();

    private WindowBuffer buffer;

    private static final int HANDLER_QUIT = 1;

    private Handler.Callback handlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what != HANDLER_QUIT)
                return false;
            buffer.quit();
            thread.quit();
            return false;
        }
    };

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            samples.incrementAndGet();
            listener.onStepEvent();

            // fill buffer entry
            int i = buffer.index * buffer.bufferWidth;
            int buf[] = buffer.buffer;
            buf[i] = event.sensor.getType();
            Conversion.longToIntArray(event.timestamp, buf, i + 1);
            for (int j = 0; j < event.values.length; ++j) {
                buf[i + 3 + j] = Float.floatToRawIntBits(event.values[j]);
            }
            buffer.next();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    SensorLoop(Context context, WindowBuffer buffer, StepsListener stepsListener) {
        log("construct");
        this.buffer = buffer;
        listener = stepsListener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);
    }

    public void start() {
        log("start");
        thread.start();
        handler = new Handler(thread.getLooper(), handlerCallback);
        sensorManager.registerListener(sensorEventListener, sensorAccelerometer,
                SensorManager.SENSOR_DELAY_FASTEST, handler);
        sensorManager.registerListener(sensorEventListener, sensorGyroscope,
                SensorManager.SENSOR_DELAY_FASTEST, handler);
        sensorManager.registerListener(sensorEventListener, sensorMagnetic,
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
