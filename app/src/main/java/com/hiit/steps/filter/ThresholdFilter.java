package com.hiit.steps.filter;

import com.hiit.steps.Sample;

public class ThresholdFilter extends OutputFilter {

    public static final int TYPE_THRESHOLD = 3473;

    private Sample sample;
    private float threshold;
    private boolean currentlyOverThreshold;

    public ThresholdFilter(Filter output, float threshold) {
        super(output);

        this.threshold = threshold;
    }

    @Override
    public void filter(Sample next) {
        if (sample == null) {
            sample = new Sample(1);
            sample.type = next.type + TYPE_THRESHOLD;
            sample.timestamp = next.timestamp;
            sample.valueCount = 1;
            if (next.values[0] < threshold) {
                currentlyOverThreshold = false;
                sample.values[0] = 0f;
            } else {
                currentlyOverThreshold = true;
                sample.values[0] = 1f;
            }
            super.filter(sample);
        } else {
            if (next.values[0] < threshold && currentlyOverThreshold) {
                sample.timestamp = next.timestamp;
                currentlyOverThreshold = false;
                sample.values[0] = 0f;
                super.filter(sample);
            } else if (next.values[0] >= threshold && !currentlyOverThreshold) {
                sample.timestamp = next.timestamp;
                currentlyOverThreshold = true;
                sample.values[0] = 1f;
                super.filter(sample);
            }
        }
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
}
