package com.hiit.steps.filter;

import com.hiit.steps.Sample;

public class SampleDelayFilter extends OutputFilter {
    public static final int TYPE_SAMPLE_DELAY = 2237; // added to original type

    private Sample[] buffer;
    private Sample sample;
    private int index;

    public SampleDelayFilter(Filter output, int width, int sampleDelay) {
        super(output);
        setSampleDelay(sampleDelay);
    }

    @Override
    public void filter(Sample next) {
        if (sample == null) {
            sample = new Sample(next);
            for (int i = 0; i < this.buffer.length; ++i) {
                this.buffer[i] = new Sample(next);
            }
        } else {
            sample.copyFrom(this.buffer[index]);
            this.buffer[index].copyFrom(next);
            if (++index >= buffer.length)
                index = 0;
        }
        sample.type += TYPE_SAMPLE_DELAY;
        super.filter(sample);
    }

    public void setSampleDelay(int sampleDelay) {
        this.buffer = new Sample[sampleDelay];
    }
}
