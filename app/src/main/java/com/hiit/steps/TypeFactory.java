package com.hiit.steps;

public interface TypeFactory<T> {
    public T create();
    public T[] createArray(int size);
}
