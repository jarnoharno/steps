package com.hiit.steps.filter;

import com.hiit.steps.Sample;

public class TimeShiftFilter extends OutputFilter {

    public static final int TYPE_TIME_SHIFT = 765; // added to original type

    private Sample sample;
    private long timeShift;

    public TimeShiftFilter(Filter output, int width, long timeShift) {
        super(output);
        this.sample = new Sample(width);
        this.timeShift = timeShift;
    }

    public void filter(Sample next) {
        sample.copyFrom(next);
        sample.type += TYPE_TIME_SHIFT;
        sample.timestamp += timeShift;
        super.filter(sample);
    }

    public void setTimeShift(long timeShift) {
        this.timeShift = timeShift;
    }
}
