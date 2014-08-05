package com.hiit.steps.filter;

import android.util.Log;

import com.hiit.steps.Sample;
import com.hiit.steps.StepsCallback;

import java.util.concurrent.atomic.AtomicInteger;

public class PeakFilter extends OutputFilter implements ResetFilter {

    public static final int TYPE_PEAK = 32000; // added to original type

    private Sample sample;
    private int window;
    private int index;
    private int minDistance;


    private StepsCallback stepsCallback;

    public PeakFilter(Filter output, int width, int windowLength, int minDistance, StepsCallback stepsCallback) {
        super(output);
        this.minDistance = minDistance;
        this.stepsCallback = stepsCallback;
        this.sample = new Sample(width);
        setWindowLength(windowLength);
    }

    @Override
    public void filter(Sample next) {
        if (index < 0) {
            ++index;
            return;
        }
        if (index == 0) {
            sample.copyFrom(next);
            sample.type += TYPE_PEAK;
        } else if (next.values[0] > sample.values[0]) {
            sample.timestamp = next.timestamp;
            sample.values[0] = next.values[0];
        }
        if (++index == window) {
            index = -minDistance;
            emit();
        }
    }

    public void setWindowLength(int windowLength) {
        this.window = windowLength;
    }

    // TODO: abstract this elsewhere
    private AtomicInteger steps = new AtomicInteger();

    public int getSteps() {
        return steps.get();
    }

    @Override
    public void reset() {
        if (index > 0) {
            emit();
        }
        index = 0;
    }

    private void emit() {
        super.filter(sample);
        stepsCallback.onStepEvent(steps.incrementAndGet());
    }
}
