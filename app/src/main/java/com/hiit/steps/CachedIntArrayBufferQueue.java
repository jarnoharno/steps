package com.hiit.steps;

public class CachedIntArrayBufferQueue extends CachedMessageQueue<IntArrayBuffer> {

    private int length;
    private int width;

    public CachedIntArrayBufferQueue(final int length, final int width) {
        super(new AbstractTypeFactory<IntArrayBuffer>() {
            @Override
            public IntArrayBuffer create() {
                return new IntArrayBuffer(length, width);
            }
        });
        this.length = length;
        this.width = width;
    }

    public void put() {
        IntArrayBuffer buffer = super.obtain().data;
        buffer.put();
        if (buffer.full()) {
            super.put();
            super.obtain().data.reset();
        }
    }

    public int getLength() {
        return length;
    }

    public int getWidth() {
        return width;
    }
}
