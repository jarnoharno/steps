package com.hiit.steps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

public class SensorLoop {

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.d(TAG, "SensorLoop(" + System.identityHashCode(this) + "): " + msg);
    }

    private static final String SENSOR_LOOP_THREAD_NAME = "SensorLoop";

    private LocationManager locationManager;

    private SensorManager sensorManager;
    private String[] locationProviders = {
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
    };
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
    private long firstRealTime = -1;
    private long maxTimestamp = -1;

    private boolean reject = false;

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (reject)
                return;
            if (firstTimestamp < 0) {
                firstRealTime = SystemClock.elapsedRealtimeNanos();
                firstTimestamp = event.timestamp;
            }
            callback.onSampleEvent(samples.incrementAndGet());
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

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (reject)
                return;
            // discard if not synchronized yet
            if (firstRealTime < 0)
                return;
            location.setElapsedRealtimeNanos(location.getElapsedRealtimeNanos() - firstRealTime);
            // discard if sample was acquired before synchronization
            if (location.getElapsedRealtimeNanos() < 0)
                return;
            callback.onSampleEvent(samples.incrementAndGet());
            LocationSerializer.toIntArray(location, queue.obtain().data);
            queue.put();

            log("location" +
                    ": provider=" + location.getProvider() +
                    ", timestamp=" + location.getElapsedRealtimeNanos() +
                    ", lat=" + location.getLatitude() +
                    ", lon=" + location.getLongitude() +
                    (location.hasAccuracy() ? ", accuracy=" + location.getAccuracy() : "") +
                    (location.hasAltitude() ? ", altitude=" + location.getAltitude() : "") +
                    (location.hasBearing() ? ", bearing=" + location.getBearing() : "") +
                    (location.hasSpeed() ? ", speed=" + location.getSpeed() : "") +
                    "");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            String statusString = "";
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    statusString = "OUT_OF_SERVICE";
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    statusString = "TEMPORARILY_UNAVAILABLE";
                    break;
                case LocationProvider.AVAILABLE:
                    statusString = "AVAILABLE";
                    break;
            }
            String extrasString = "";
            for (String key: extras.keySet()) {
                extrasString += ", " + key + "=" + extras.get(key).toString();
            }
            log(provider + " status changed: " + statusString + extrasString);
        }

        @Override
        public void onProviderEnabled(String provider) {
            log(provider + " enabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
            log(provider + " disabled");
        }
    };

    public SensorLoop(Context context,
               CachedIntArrayBufferQueue queue,
               StepsCallback stepsCallback) {
        log("construct");
        handler = new Handler();
        this.queue = queue;
        callback = stepsCallback;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
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

    private int sampleRate = 20000; // 20 ms

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void start() {
        log("start");
        thread.start();
        loopHandler = new Handler(thread.getLooper(), handlerCallback);
        for (String locationProvider: locationProviders) {
            locationManager.requestLocationUpdates(locationProvider, 0, 0,
                    locationListener, loopHandler.getLooper());
        }
        for (Sensor sensor: sensors) {
            sensorManager.registerListener(sensorEventListener, sensor,
                    sampleRate, loopHandler);
            log("listening " + sensor.getName() +
                    ", delay: " + sampleRate +
                    " μs, minDelay: " + sensor.getMinDelay() +
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
        thread.quit();
        handler.post(new Runnable() {
            @Override
            public void run() {
                sensorManager.unregisterListener(sensorEventListener);
                locationManager.removeUpdates(locationListener);
            }
        });
        queue.quit();
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
