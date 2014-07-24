package com.hiit.steps;

public class QueueFilter implements Filter {

    private CachedIntArrayBufferQueue queue;

    public QueueFilter(CachedIntArrayBufferQueue queue) {
        this.queue = queue;
    }

    @Override
    public void filter(Sample sample) {
        IntArrayBuffer buffer = queue.obtain().data;
        sample.copyTo(buffer.buffer, buffer.obtain());
        queue.put();
    }
}
