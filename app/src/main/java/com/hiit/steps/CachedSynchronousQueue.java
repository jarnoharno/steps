package com.hiit.steps;

import android.util.Log;

import java.util.concurrent.SynchronousQueue;

public class CachedSynchronousQueue<T> implements CachedQueue<T> {

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.d(TAG, "Queue(" + System.identityHashCode(this) + "): " + msg);
    }

    private SynchronousQueue<T> queue = new SynchronousQueue<T>();

    private T t1;
    private T t2;
    private boolean first;

    CachedSynchronousQueue(TypeFactory<T> factory) {
        t1 = factory.create();
        t2 = factory.create();
    }

    @Override
    public T obtain() {
        return first ? t1 : t2;
    }

    @Override
    public void put() {
        T t = obtain();
        first = !first;
        if (queue.offer(t))
            return;
        try {
            log("Waiting for consumer");
            queue.put(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public T take() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return take();
        }
    }
}
