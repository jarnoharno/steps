package com.hiit.steps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class Trace {
    public interface SamplerClient extends StepsService.ContextClient {
        void SensorEventReceived(SensorEvent sensorEvent);
        void LocationReceived(Location location);
    }

    public Trace(SamplerClient samplerClient) {
        this.samplerClient = samplerClient;
    }

    void start() {
        enabled = true;
        Context context = samplerClient.getContext();
        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
        SensorManager sensorManager = (SensorManager)
                context.getSystemService(Context.SENSOR_SERVICE);

        for (String locationProvider: locationProviders) {
            locationManager.requestLocationUpdates(locationProvider, 0, 0,
                    locationListener);
        }
        for (int sensorType: sensorTypes) {
            Sensor sensor = sensorManager.getDefaultSensor(sensorType);
            sensorManager.registerListener(sensorEventListener, sensor,
                    SAMPLE_RATE_US);
        }
    }

    void stop() {
        enabled = false;
        timestampCorrection = 0;
        Context context = samplerClient.getContext();
        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
        SensorManager sensorManager = (SensorManager)
                context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(sensorEventListener);
        locationManager.removeUpdates(locationListener);
    }

    // private
    private long timestampCorrection = 0;
    private boolean enabled = false;

    // sampling rate can only be set between 10-25 milliseconds
    private static int SAMPLE_RATE_US = 25000;

    private static int[] sensorTypes = {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_ROTATION_VECTOR,
    };

    private static String[] locationProviders = {
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
    };

    private SamplerClient samplerClient;

    private void checkTimestampCorrection(long timestamp) {
        if (timestampCorrection == 0) {
            timestampCorrection =
                    System.currentTimeMillis() * 1000000 - timestamp;
        }
    }

    private SensorEventListener sensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (enabled) {
                checkTimestampCorrection(event.timestamp);
                event.timestamp += timestampCorrection;
                samplerClient.SensorEventReceived(event);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // ignore
        }
    };

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            if (enabled) {
                long ts = location.getElapsedRealtimeNanos();
                checkTimestampCorrection(ts);
                location.setElapsedRealtimeNanos(ts + timestampCorrection);
                samplerClient.LocationReceived(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // ignore
        }

        @Override
        public void onProviderEnabled(String provider) {
            // ignore
        }

        @Override
        public void onProviderDisabled(String provider) {
            // ignore
        }
    };
}
