package com.hiit.steps;

public class AbstractTypeFactory<T> implements TypeFactory<T> {

    @Override
    public T create() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public T[] createArray(int size) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
