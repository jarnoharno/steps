package com.hiit.steps.filter;

import com.hiit.steps.Sample;
import com.hiit.steps.filter.Filter;

public class ResamplingFilter implements Filter {

    public static final int TYPE_RESAMPLE = 1000; // added to original type

    private long resampleRate;

    private Sample sample; // last sample
    private long prevTimestamp;
    private double[] prevValues;

    private Filter output;

    public ResamplingFilter(Filter output, int width, long resampleRate) {
        this.resampleRate = resampleRate;
        this.output = output;
        this.prevTimestamp = -resampleRate;
        this.prevValues = new double[width];
        this.sample = new Sample(width);
        this.sample.timestamp = -resampleRate;
        this.deltas = new double[width];
    }

    private double[] deltas;

    public void filter(final Sample next) {
        long timeDiff = next.timestamp - sample.timestamp;
        if (timeDiff >= resampleRate) {
            int sampleCount = (int) (timeDiff / resampleRate);
            long prevTimeDiff = next.timestamp - prevTimestamp;
            // calculate deltas
            for (int j = 0; j < next.valueCount; ++j) {
                double valueDiff = (double) next.values[j] - prevValues[j];
                deltas[j] = valueDiff / prevTimeDiff;
            }
            // interpolate
            for (int k = 0; k < sampleCount; ++k) {
                sample.type = next.type + TYPE_RESAMPLE;
                sample.valueCount = next.valueCount;
                sample.timestamp += resampleRate;
                long dt = sample.timestamp - prevTimestamp;
                for (int j = 0; j < next.valueCount; ++j) {
                    sample.values[j] = (float) (prevValues[j] + deltas[j] * dt);
                }
                // forward
                output.filter(sample);
            }
        }
        prevTimestamp = next.timestamp;
        for (int i = 0; i < next.valueCount; ++i) {
            prevValues[i] = next.values[i];
        }
    }

    public void setResampleRate(long resampleRate) {
        this.resampleRate = resampleRate;
        if (this.sample.timestamp < 0) {
            this.prevTimestamp = -resampleRate;
            this.sample.timestamp = -resampleRate;
        }
    }
}
