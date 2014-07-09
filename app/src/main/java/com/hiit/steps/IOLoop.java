package com.hiit.steps;

import android.content.Context;
import android.hardware.Sensor;
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

    private WindowBuffer buffer;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (;;) {
                WindowBuffer.Window window = buffer.take();
                log("received window");
                writeWindow(window);
                if (window.command == WindowBuffer.Window.Command.Quit) {
                    return;
                }
            }
        }

        public void writeWindow(WindowBuffer.Window window) {
            if (window.end >= window.begin) {
                for (int i = window.begin; i < window.end; ++i) {
                    write(i);
                }
                return;
            }
            for (int i = window.begin; i < buffer.bufferLength; ++i) {
                write(i);
            }
            for (int i = 0; i < window.end; ++i) {
                write(i);
            }

        }

        private void write(int index) {
            int[] buf = buffer.buffer;
            int i = index * buffer.bufferWidth;
            int type = buf[i];
            long timestamp = Conversion.intArrayToLong(buf, i + 1);
            switch (type) {
                case Sensor.TYPE_ACCELEROMETER:
                    formatter.format("ACC %d", timestamp);
                    writeFloats(buf, i + 3, 3);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    formatter.format("GYR %d", timestamp);
                    writeFloats(buf, i + 3, 3);
                    break;
                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                    formatter.format("GYU %d", timestamp);
                    writeFloats(buf, i + 3, 6);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    formatter.format("MAG %d", timestamp);
                    writeFloats(buf, i + 3, 3);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    formatter.format("MAU %d", timestamp);
                    writeFloats(buf, i + 3, 6);
                    break;
            }
            formatter.format("\n");
        }

        private void writeFloats(int[] buf, int offset, int length) {
            for (int i = 0; i < length; ++i) {
                formatter.format(" %f", Float.intBitsToFloat(buf[offset + i]));
            }
         }
    };

    IOLoop(Context context, WindowBuffer buffer) {
        this.context = context;
        this.thread = new Thread(runnable);
        this.buffer = buffer;
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
