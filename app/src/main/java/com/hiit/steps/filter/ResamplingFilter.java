package com.hiit.steps.filter;

import com.hiit.steps.Sample;
import com.hiit.steps.filter.Filter;

public class ResamplingFilter implements Filter {

    public static final int TYPE_RESAMPLE = 1000; // added to original type

    private int resampleRate;

    private Sample sample; // last sample
    private Sample previous; // previous source event

    private Filter output;

    public ResamplingFilter(Filter output, int width) {
        this(output, width, 10000); // 10 ms
    }

    public ResamplingFilter(Filter output, int width, int resampleRate) {
        this.resampleRate = resampleRate;
        this.output = output;
        this.previous = new Sample(width);
        this.previous.timestamp = -resampleRate;
        this.sample = new Sample(width);
        this.sample.timestamp = -resampleRate;
        this.deltas = new float[width];
    }

    private float[] deltas;

    public void filter(final Sample next) {
        int timeDiff = (int) (next.timestamp - sample.timestamp);
        if (timeDiff >= resampleRate) {
            int sampleCount = timeDiff / resampleRate;
            int prevTimeDiff = (int) (next.timestamp - previous.timestamp);
            // calculate deltas
            for (int j = 0; j < next.valueCount; ++j) {
                float valueDiff = next.values[j] - previous.values[j];
                deltas[j] = valueDiff / prevTimeDiff;
            }
            // resample
            for (int k = 0; k < sampleCount; ++k) {
                sample.type = next.type + TYPE_RESAMPLE;
                sample.valueCount = next.valueCount;
                sample.timestamp += resampleRate;
                int dt = (int) (sample.timestamp - previous.timestamp);
                for (int j = 0; j < next.valueCount; ++j) {
                    sample.values[j] = previous.values[j] + deltas[j] * dt;
                }
                // forward
                output.filter(sample);
            }
        }
        previous.copyFrom(next);
    }

    public void setResampleRate(int resampleRate) {
        this.resampleRate = resampleRate;
        if (this.sample.timestamp < 0) {
            this.previous.timestamp = -resampleRate;
            this.sample.timestamp = -resampleRate;
        }
    }
}
