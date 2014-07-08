package com.hiit.steps;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;

public class IOLoop {

    private static final String FILE_PREFIX = "steps";
    private static final String FILE_SUFFIX = "";

    private static final String TAG = "Steps";

    private Thread thread;
    private Context context;

    private File file = null;
    private FileOutputStream stream = null;
    private Formatter formatter = null;

    private IOBuffer ioBuffer;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (;;) {
                IOBuffer.Message message = ioBuffer.take();
                transfer(message);
                if (message.type == IOBuffer.Message.Type.Quit) {
                    return;
                }
            }
        }

        public void transfer(IOBuffer.Message message) {
            if (message.end >= message.begin) {
                for (int i = message.begin; i < message.end; ++i) {
                    transferEntry(i);
                }
                return;
            }
            for (int i = message.begin; i < ioBuffer.bufferSize; ++i) {
                transferEntry(i);
            }
            for (int i = 0; i < message.end; ++i) {
                transferEntry(i);
            }

        }

        public void transferEntry(int index) {
            formatter.format("%d %f %f %f\n",
                    ioBuffer.timestampEntries[index],
                    ioBuffer.floatEntries[index][0],
                    ioBuffer.floatEntries[index][1],
                    ioBuffer.floatEntries[index][2]);
        }
    };

    IOLoop(Context context, SensorBuffer sensorBuffer, IOBuffer ioBuffer) {
        this.context = context;
        this.thread = new Thread(runnable);
        this.ioBuffer = ioBuffer;
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
        thread.start();
    }

    public void stop() {
        // just wait and hope the source quits the loop
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
