package com.hiit.steps;

public class SquareFilter extends OutputFilter {

    public static final int TYPE_SQUARE = 4000; // added to original type

    private Sample sample;

    public SquareFilter(Filter output, int width) {
        super(output);
        this.sample = new Sample(width);
    }

    @Override
    public void filter(Sample next) {
        sample.type =  next.type + TYPE_SQUARE;
        sample.timestamp = next.timestamp;
        sample.valueCount = next.valueCount;
        for (int i = 0; i < next.valueCount; ++i) {
            float v = next.values[i];
            sample.values[i] = v * v;
        }
        super.filter(sample);
    }
}
