package com.hiit.steps;

public class DelayFilter extends OutputFilter {

    public static final int TYPE_DELAY = 16000; // added to original type

    private Sample sample;
    private long delay;

    public DelayFilter(Filter output, int width, long delay) {
        super(output);
        this.sample = new Sample(width);
        this.delay = delay;
    }

    @Override
    public void filter(Sample next) {
        sample.copyFrom(next);
        sample.type += TYPE_DELAY;
        sample.timestamp -= delay;
        super.filter(sample);
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
}
