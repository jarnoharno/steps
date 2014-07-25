package com.hiit.steps;

public class MovingAverageFilter extends OutputFilter {

    public static final int TYPE_MOVING_AVERAGE = 8000; // added to original type

    private Sample sample; // aggregate sample
    private int windowLength;

    public MovingAverageFilter(Filter output, int windowLength) {
        super(output);
        this.windowLength = windowLength;
    }

    @Override
    public void filter(Sample next) {
        if (sample == null) {
            sample = new Sample(next);
        } else {
            int n1 = windowLength + 1;
            for (int i = 0; i < next.valueCount; ++i) {
                sample.values[i] += (next.values[i] - sample.values[i]) / n1;
            }
        }
        sample.type = next.type + TYPE_MOVING_AVERAGE;
        sample.timestamp = next.timestamp;
        super.filter(sample);
    }

    public void setWindowLength(int windowLength) {
        this.windowLength = windowLength;
    }
}
