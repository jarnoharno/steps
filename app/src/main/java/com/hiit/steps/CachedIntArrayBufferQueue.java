package com.hiit.steps;

public class CachedIntArrayBufferQueue extends CachedMessageQueue<IntArrayBuffer> {
    public CachedIntArrayBufferQueue(final int length, final int width) {
        super(new AbstractTypeFactory<IntArrayBuffer>() {
            @Override
            public IntArrayBuffer create() {
                return new IntArrayBuffer(length, width);
            }
        });
    }

    public void put() {
        IntArrayBuffer buffer = super.obtain().data;
        buffer.put();
        if (buffer.full()) {
            super.put();
            super.obtain().data.reset();
        }
    }
}
