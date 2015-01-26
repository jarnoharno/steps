package com.hiit.steps.filter;

import com.hiit.steps.CachedIntArrayBufferQueue;
import com.hiit.steps.IntArrayBuffer;
import com.hiit.steps.Sample;
import com.hiit.steps.filter.Filter;

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
