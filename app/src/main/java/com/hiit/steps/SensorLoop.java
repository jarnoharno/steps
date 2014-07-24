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
    private Handler loopHandler;
    private Handler handler;
    private StepsCallback callback;

    private AtomicInteger samples = new AtomicInteger();

    private CachedIntArrayBufferQueue queue;

    private static final int HANDLER_QUIT = 1;

    private Handler.Callback handlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what != HANDLER_QUIT)
                return false;
            quitLoop();
            return false;
        }
    };

    private long firstTimestamp = -1;
    private long maxTimestamp = -1;

    private boolean reject = false;

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (reject)
                return;
            callback.onSampleEvent(samples.incrementAndGet());
            if (firstTimestamp < 0) {
                firstTimestamp = event.timestamp;
            }
            // in the unlikely event of wrap around
            if (event.timestamp < firstTimestamp) {
                event.timestamp += Long.MAX_VALUE - firstTimestamp;
            } else {
                event.timestamp -= firstTimestamp;
            }
            SensorEventSerializer.toIntArray(event, queue.obtain().data);
            queue.put();
            if (maxTimestamp >= 0 && event.timestamp > maxTimestamp) {
                quitLoop();
            }
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

    public SensorLoop(Context context,
               CachedIntArrayBufferQueue queue,
               StepsCallback stepsCallback) {
        log("construct");
        handler = new Handler();
        this.queue = queue;
        callback = stepsCallback;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        for (int i = 0; i < sensorTypes.length; ++i) {
            sensors[i] = sensorManager.getDefaultSensor(sensorTypes[i]);
        }
    }

    public void setMaxTimestamp(long maxTimestamp) {
        this.maxTimestamp = maxTimestamp;
    }

    public long getMaxTimestamp() {
        return maxTimestamp;
    }

    private int rateUs = SensorManager.SENSOR_DELAY_FASTEST;

    public void setRateUs(int rateUs) {
        this.rateUs = rateUs;
    }

    public int getRateUs() {
        return rateUs;
    }

    public void start() {
        log("start");
        thread.start();
        loopHandler = new Handler(thread.getLooper(), handlerCallback);
        for (Sensor sensor: sensors) {
            sensorManager.registerListener(sensorEventListener, sensor,
                    rateUs, loopHandler);
            log("listening " + sensor.getName() +
                    ", minDelay: " + sensor.getMinDelay() +
                    " μs, maximumRange: " + sensor.getMaximumRange() +
                    ", resolution: " + sensor.getResolution());
        }
    }

    public void stop() {
        log("stop");
        loopHandler.sendMessage(loopHandler.obtainMessage(HANDLER_QUIT));
    }

    private void quitLoop() {
        reject = true;
        queue.quit();
        thread.quit();
        handler.post(new Runnable() {
            @Override
            public void run() {
                sensorManager.unregisterListener(sensorEventListener);
            }
        });
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
