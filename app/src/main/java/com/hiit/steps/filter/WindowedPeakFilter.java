package com.hiit.steps.filter;

import com.hiit.steps.Sample;
import com.hiit.steps.StepsCallback;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

// The filter loses peaks that happen between windows!

public class WindowedPeakFilter extends OutputFilter implements ResetFilter {

    public static final int TYPE_WINDOWED_PEAK = 15729; // added to original type

    public static class Peak implements Comparable<Peak> {
        public Peak prev;
        public Peak next;
        public int type;
        public long timestamp;
        public float value;
        public boolean included;
        public int index;

        // natural ordering & equality based on sample timestamp

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Peak)) return false;
            Peak that = (Peak) o;
            if (value != that.value) return false;
            return true;
        }

        @Override
        public int hashCode() {
            // http://docs.oracle.com/javase/7/docs/api/java/lang/Float.html#floatToIntBits(float)
            return Float.floatToIntBits(value);
        }

        // descending order
        @Override
        public int compareTo(Peak another) {
            if (another == null)
                return -1;
            if (value < another.value)
                return 1;
            if (value > another.value)
                return -1;
            return 0;
        }
    }

    private Peak[] peakBuffer;
    private int peakIndex;
    private long lastEmittedPeak;

    private float prev;
    private float current;
    private long currentTs;
    private int currentType;

    private int index;
    private long minDistance;
    private int window;
    private Sample sample;

    private StepsCallback stepsCallback;

    public WindowedPeakFilter(Filter output, long minDistance, int window, StepsCallback stepsCallback) {
        super(output);
        this.stepsCallback = stepsCallback;
        this.sample = new Sample(1);
        this.sample.valueCount = 1;
        setMinDistance(minDistance);
        setWindow(window);
    }

    public void setMinDistance(long minDistance) {
        this.lastEmittedPeak = -minDistance;
        this.minDistance = minDistance;
    }

    public void setWindow(int window) {
        this.window = window;
        this.peakBuffer = new Peak[window / 2];
        for (int i = 0; i < this.peakBuffer.length; ++i) {
            this.peakBuffer[i] = new Peak();
        }
    }

    @Override
    public void reset() {
        if (peakIndex > 0) {
            pruneAndEmit();
        }
    }

    @Override
    public void filter(Sample nextSample) {
        float next = nextSample.values[0];
        if (index > 1) {
            if (current > next && current > prev && (currentTs - lastEmittedPeak) >= minDistance) {
                // we have a peak
                Peak peak = peakBuffer[peakIndex];
                peak.type = currentType + TYPE_WINDOWED_PEAK;
                peak.timestamp = currentTs;
                peak.value = current;
                peak.prev = null;
                peak.next = null;
                peak.included = true;
                peak.index = index - 1;
                if (peakIndex > 0) {
                    peakBuffer[peakIndex - 1].next = peak;
                    peak.prev = peakBuffer[peakIndex - 1];
                }
                ++peakIndex;
            }
            // shift buffer
            prev = current;
            current = next;
            currentType = nextSample.type;
            currentTs = nextSample.timestamp;
        } else if (index == 1) {
            current = next;
            currentType = nextSample.type;
            currentTs = nextSample.timestamp;
        } else { // index == 0
            prev = next;
        }

        if (++index >= window && peakIndex > 0) {
            pruneAndEmit();
        }
    }

    private void pruneAndEmit() {

        Peak first = null;
        // O(n log n)
        Arrays.sort(peakBuffer, 0, peakIndex);
        // O(n)
        for (int i = 0; i < peakIndex; ++i) {
            Peak peak = peakBuffer[i];
            if (!peak.included)
                continue;
            // prune left side
            while (peak.prev != null && (peak.timestamp - peak.prev.timestamp) < minDistance) {
                peak.prev.included = false;
                peak.prev = peak.prev.prev;
            }
            // check if first peak in range
            if (peak.prev == null)
                first = peak;
            // prune right side
            while (peak.next != null && (peak.next.timestamp - peak.timestamp) < minDistance) {
                peak.next.included = false;
                peak.next = peak.next.next;
            }
        }
        // emit peaks
        Peak peak = first;
        while (peak != null) {
            sample.type = peak.type;
            sample.timestamp = peak.timestamp;
            sample.values[0] = peak.value;
            emit();
            lastEmittedPeak = peak.timestamp;
            peak = peak.next;
        }
        index = 0;
        peakIndex = 0;
    }

    // TODO: abstract this elsewhere
    private AtomicInteger steps = new AtomicInteger();

    public int getSteps() {
        return steps.get();
    }

    private void emit() {
        super.filter(sample);
        stepsCallback.onStepEvent(steps.incrementAndGet());
    }
}
