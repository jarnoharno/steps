package com.hiit.steps;

import java.util.concurrent.SynchronousQueue;

public class SyncPair<T> {

    public static interface Factory<T> {
        public T create();
    }

    private SynchronousQueue<T> queue;

    private T t1;
    private T t2;
    private boolean first;

    SyncPair(Factory<T> factory) {
        t1 = factory.create();
        t2 = factory.create();
    }

    public T obtain() {
        return first ? t1 : t2;
    }

    public void put() {
        T t = obtain();
        first = !first;
        try {
            queue.put(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public T take() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return take();
        }
    }
}
