package com.hiit.steps;

public class Buffer<T> {

    private Object[] buffer;
    private int end;

    public Buffer(int size) {
        buffer = new Object[size];
        end = 0;
    }

    public void put(T object) {
        buffer[end++] = object;
    }

    public T get(int i) {
        return (T) buffer[i];
    }

    public boolean full() {
        return end == buffer.length;
    }

    public void reset() {
        end = 0;
    }

    public int getEnd() {
        return end;
    }

}
