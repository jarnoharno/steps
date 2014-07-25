package com.hiit.steps;

import android.util.Log;

public class MovingAverageFilter extends OutputFilter {

    public static final int TYPE_MOVING_AVERAGE = 8000; // added to original type

    private Sample[] buffer; // buffer of previous values
    private Sample mean;
    private int width;
    private int index;

    public MovingAverageFilter(Filter output, int width, int windowLength) {
        super(output);
        this.width = width;
        setWindowLength(windowLength);
    }

    @Override
    public void filter(Sample next) {
        if (mean == null) {
            // first sample
            mean = new Sample(next);
            mean.type += TYPE_MOVING_AVERAGE;
            for (int i = 0; i < buffer.length; ++i) {
                buffer[i].copyFrom(mean);
            }
        } else {
            for (int i = 0; i < mean.valueCount; ++i) {
                mean.values[i] = (buffer.length * mean.values[i] - buffer[index].values[i] + next.values[i]) / buffer.length;
                buffer[index].values[i] = next.values[i];
            }
            if (++index >= buffer.length)
                index = 0;
        }
        mean.timestamp = next.timestamp;
        super.filter(mean);
    }

    public void setWindowLength(int windowLength) {
        this.buffer = new Sample[windowLength];
        for (int i = 0; i < this.buffer.length; ++i) {
            this.buffer[i] = new Sample(width);
        }
    }
}
