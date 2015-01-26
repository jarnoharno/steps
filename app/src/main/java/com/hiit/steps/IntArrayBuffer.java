package com.hiit.steps;

public class IntArrayBuffer {

    public int[] buffer;

    private int length;
    private int width;
    private int end;

    IntArrayBuffer(int length, int width) {
        this.buffer = new int[length * width];
        this.length = length;
        this.width = width;
        this.end = 0;
    }

    public int obtain() {
        return get(end);
    }

    public int get(int i) {
        return i * width;
    }

    public void put() {
        ++end;
    }

    public boolean full() {
        return end == length;
    }

    public void reset() {
        end = 0;
    }

    public int getLength() {
        return length;
    }

    public int getWidth() {
        return width;
    }

    public int getEnd() {
        return end;
    }

}
