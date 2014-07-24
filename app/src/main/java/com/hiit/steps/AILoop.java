package com.hiit.steps;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

public class AILoop {

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.d(TAG, "AILoop(" + System.identityHashCode(this) + "): " + msg);
    }


    private WindowQueue windowQueue;
    private Thread thread;

    private CachedIntArrayBufferQueue sensorQueue;
    private CachedIntArrayBufferQueue ioQueue;

    private AtomicInteger steps = new AtomicInteger();

    int samples = 0;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (;;) {
                CachedIntArrayBufferQueue.Message message = sensorQueue.take();
                transferBuffer(message.data);
                if (message.getCommand() == CachedIntArrayBufferQueue.Command.Quit) {
                    ioQueue.quit();
                    return;
                }
            }
        }

    };

    public void transferBuffer(IntArrayBuffer src) {
        for (int i = 0; i < src.getEnd(); ++i) {
            ++samples;
            IntArrayBuffer buffer = ioQueue.obtain().data;
            System.arraycopy(
                    src.buffer, src.get(i),
                    buffer.buffer, buffer.obtain(),
                    buffer.getWidth());
            ioQueue.put();
        }
    }

    AILoop(Context context,
           CachedIntArrayBufferQueue sensorQueue,
           CachedIntArrayBufferQueue ioQueue,
           StepsListener stepsListener) {
        this.thread = new Thread(runnable);
        this.sensorQueue = sensorQueue;
        this.ioQueue = ioQueue;
    }

    public void start() {
        thread.start();
    }

    public int getSteps() {
        return steps.get();
    }
}
