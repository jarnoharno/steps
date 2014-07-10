package com.hiit.steps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;

public class IOLoop {

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.d(TAG, "IOLoop(" + System.identityHashCode(this) + "): " + msg);
    }

    private static final String FILE_PREFIX = "steps";
    private static final String FILE_SUFFIX = "";

    private Thread thread;
    private Context context;

    private File file;
    private FileOutputStream stream;
    private Formatter formatter;

    private CachedBufferQueue<SensorEvent> queue;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (;;) {
                CachedBufferQueue.Message<SensorEvent> message = queue.take();
                log("received window");
                writeBuffer(message.buffer);
                if (message.getCommand() == CachedBufferQueue.Message.Command.Quit) {
                    return;
                }
            }
        }

        public void writeBuffer(Buffer<SensorEvent> buffer) {
            for (int i = 0; i < buffer.getEnd(); ++i) {
                write(buffer.get(i));
            }
        }

        private void write(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    formatter.format("acc");
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    formatter.format("gyr");
                    break;
                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                    formatter.format("gyu");
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    formatter.format("mag");
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    formatter.format("mau");
                    break;
            }
            formatter.format(" %d", event.timestamp);
            writeFloats(event.values);
            formatter.format("\n");
        }

        private void writeFloats(float[] buf) {
            for (int i = 0; i < buf.length; ++i) {
                formatter.format(" %f", buf[i]);
            }
         }
    };

    IOLoop(Context context, CachedBufferQueue<SensorEvent> queue) {
        this.context = context;
        this.thread = new Thread(runnable);
        this.queue = queue;
        try {
            file = File.createTempFile(FILE_PREFIX, FILE_SUFFIX,
                    context.getCacheDir());
            stream = new FileOutputStream(file);
            formatter = new Formatter(stream, "UTF-8", null);
            Log.d(TAG, "writing to " + file.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        log("start");
        thread.start();
    }

    public void stop() {
        // just wait and hope the source quits the loop
        log("stop");
        try {
            thread.join();
            formatter.close();
            Log.d(TAG, "wrote " + file.toString());

            if (FileSystem.isExternalStorageWritable()) {
                // move to files dir
                File externalFolder = context.getExternalFilesDir(null);
                File externalFile = FileSystem.nextAvailableFile(
                        FILE_PREFIX, FILE_SUFFIX, externalFolder);
                try {
                    FileSystem.moveFile(file, externalFile);
                    file = externalFile;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Toast toast = Toast.makeText(context, ("wrote " + file.toString()), Toast.LENGTH_LONG);
            toast.show();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
