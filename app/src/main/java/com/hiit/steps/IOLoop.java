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

    private CachedIntArrayBufferQueue queue;

    private int samples = 0;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (;;) {
                CachedIntArrayBufferQueue.Message message = queue.take();
                log("received window");
                writeBuffer(message.data);
                if (message.getCommand() == CachedIntArrayBufferQueue.Command.Quit) {
                    return;
                }
            }
        }

        public void writeBuffer(IntArrayBuffer buffer) {
            for (int i = 0; i < buffer.getEnd(); ++i) {
                ++samples;
                SensorEventSerializer.formatIntArray(buffer.buffer, buffer.get(i), formatter);
            }
        }

    };

    IOLoop(Context context, CachedIntArrayBufferQueue queue) {
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
            log(samples + " samples received");
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
