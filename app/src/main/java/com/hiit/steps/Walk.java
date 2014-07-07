package com.hiit.steps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;

public class Walk implements SensorEventListener {

    private static final String FILE_PREFIX = "steps";
    private static final String FILE_SUFFIX = "";

    private static final String TAG = "Steps";

    private static final String STEPS_LOOP = "steps_loop";
    private static final int MAX_ENTRIES_IN_MEMORY = 1024;

    private boolean running = false;
    private int steps = 0;
    private HandlerThread thread = null;
    private Handler handler = null;
    private StepsService service = null;

    // buffer
    private long[] tbuffer = new long[MAX_ENTRIES_IN_MEMORY * 2];
    private float[][] buffer = new float[MAX_ENTRIES_IN_MEMORY * 2][3];
    private int index = 0;
    private File file = null;
    private FileOutputStream stream = null;
    private Formatter formatter = null;

    // called in sensor looper thread

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        if (!running)
            return;

        increaseSteps();
        //Log.d(TAG, String.format("%f, %f, %f",
        // event.values[0], event.values[1], event.values[2]));
        tbuffer[index] = event.timestamp;
        System.arraycopy(event.values, 0, buffer[index], 0, 3);
        ++index;
        if (index == MAX_ENTRIES_IN_MEMORY) {
            copyFile(0);
        } else if (index == MAX_ENTRIES_IN_MEMORY * 2) {
            copyFile(MAX_ENTRIES_IN_MEMORY);
            index = 0;
        }
    }

    private static final int LOOP_QUIT = 1;

    Handler.Callback loopHandler = new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what != LOOP_QUIT)
                return false;
            if (index < MAX_ENTRIES_IN_MEMORY) {
                copyFile(0, index);
            } else {
                copyFile(MAX_ENTRIES_IN_MEMORY, index);
            }
            running = false;
            thread.quit();
            return true;
        }
    };

    private void copyFile(int offset) {
        copyFile(offset, offset + MAX_ENTRIES_IN_MEMORY);
    }

    private void copyFile(int offset, int end) {
        if (stream == null)
            return;
        for (int i = offset; i < end; ++i) {
            formatter.format("%d %f %f %f\n", tbuffer[i], buffer[i][0],
                    buffer[i][1], buffer[i][2]);
        }
    }

    private void increaseSteps() {
        ++steps;
        service.onStep();
    }

    Walk(StepsService service) {
        this.service = service;
    }

    public void start() {
        // start looper thread
        thread = new HandlerThread(STEPS_LOOP);
        thread.start();
        handler = new Handler(thread.getLooper(), loopHandler);
        service.sensorManager.registerListener(this, service.sensor,
                SensorManager.SENSOR_DELAY_FASTEST, handler);
        // open file
        try {
            file = File.createTempFile(FILE_PREFIX, FILE_SUFFIX,
                    service.getCacheDir());
            stream = new FileOutputStream(file);
            formatter = new Formatter(stream, "UTF-8", null);
            Log.d(TAG, "writing to " + file.toString());
        } catch (IOException e) {
            e.printStackTrace();

        }
        running = true;
    }

    public void stop() {
        service.sensorManager.unregisterListener(this);
        running = false;
        handler.sendMessage(handler.obtainMessage(LOOP_QUIT));
        try {
            thread.join();
            formatter.close();
            Log.d(TAG, "wrote " + file.toString());

            if (FileSystem.isExternalStorageWritable()) {
                // move to files dir
                File externalFolder = service.getExternalFilesDir(null);
                File externalFile = FileSystem.nextAvailableFile(
                        FILE_PREFIX, FILE_SUFFIX, externalFolder);
                try {
                    FileSystem.moveFile(file, externalFile);
                    file = externalFile;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Toast toast = Toast.makeText(service, ("wrote " + file.toString()), Toast.LENGTH_LONG);
            toast.show();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        thread = null;
        handler = null;
    }

    public boolean isRunning() {
        return running;
    }

    public int getSteps() {
        return steps;
    }

    public static Walk start(StepsService service) {
        Walk walk = new Walk(service);
        walk.start();
        return walk;
    }
}
