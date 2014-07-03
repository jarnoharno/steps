package com.hiit.steps;

import android.app.Service;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

public class Walk implements SensorEventListener {

    private static final String STEPS_LOOP = "steps_loop";
    private static final int MAX_ENTRIES_IN_MEMORY = 1024;

    private boolean running = false;
    private int steps = 0;
    private HandlerThread thread = null;
    private Handler handler = null;
    private StepsService service = null;

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        increaseSteps();
    }

    private void increaseSteps() {
        ++steps;
        service.onStep();
    }

    Walk(StepsService service) {
        this.service = service;
    }

    public void start() {
        thread = new HandlerThread(STEPS_LOOP);
        thread.start();
        handler = new Handler(thread.getLooper());
        service.sensorManager.registerListener(this, service.sensor, SensorManager.SENSOR_DELAY_FASTEST, handler);
        running = true;
    }

    public void stop() {
        service.sensorManager.unregisterListener(this);
        thread.quit(); // does not wait

        // optimistically set to null...
        thread = null;
        handler = null;

        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public int getSteps() {
        return steps;
    }
}
