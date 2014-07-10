package com.hiit.steps;

public interface CachedQueue<T> {
    public T obtain();
    public void put();
    public T take();
}
