package com.hiit.steps.filter;

import com.hiit.steps.Sample;

public class DelayFilter extends OutputFilter {

    public static final int TYPE_DELAY = 32000; // added to original type

    private Sample[] buffer;
    private Sample sample;
    private int index;
    private int width;
    private int sampleDelay;
    private long sampleRate;

    public DelayFilter(Filter output, int width, int sampleDelay, long sampleRate) {
        super(output);
        this.width = width;
        this.setSampleRate(sampleRate);
        this.setSampleDelay(sampleDelay);
    }

    @Override
    public void filter(Sample next) {
        if (sample == null) {
            sample = new Sample(next);
            for (int i = 0; i < this.buffer.length; ++i) {
                this.buffer[i] = new Sample(next);
                this.buffer[i].timestamp = (i + 1) * sampleRate + next.timestamp;
            }
        } else {
            sample.copyFrom(this.buffer[index]);
            this.buffer[index].copyFrom(next);
            this.buffer[index].timestamp += sampleDelay * sampleRate;
            if (++index >= buffer.length)
                index = 0;
        }
        sample.type += TYPE_DELAY;
        super.filter(sample);
    }

    public void setSampleDelay(int sampleDelay) {
        this.sampleDelay = sampleDelay;
        this.buffer = new Sample[sampleDelay];
    }

    public void setSampleRate(long sampleRate) {
        this.sampleRate = sampleRate;
    }
}
