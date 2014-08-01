package com.hiit.steps.filter;

import com.hiit.steps.Sample;
import com.hiit.steps.filter.Filter;
import com.hiit.steps.filter.OutputFilter;

public class SlidingStandardDeviationFilter extends OutputFilter {

    public static final int TYPE_STANDARD_DEVIATION = 16000; // added to original type

    private Sample[] buffer;
    private Sample sum;
    private double[][] sqBuffer;
    private double[] sqSum;

    private Sample sample;
    private int window;
    private int width;
    private int index;

    public SlidingStandardDeviationFilter(Filter output, int width, int windowLength) {
        super(output);
        this.width = width;
        setWindowLength(windowLength);
    }

    @Override
    public void filter(Sample next) {
        if (sample == null) {

            // first sample

            sample = new Sample(width);
            sample.type = next.type + TYPE_STANDARD_DEVIATION;
            sample.valueCount = next.valueCount;

            // update buffer
            for (int i = 0; i < buffer.length; ++i) {
                buffer[i].copyFrom(next);
            }
            sum = new Sample(width);
            sum.valueCount = next.valueCount;
            for (int i = 0; i < sum.valueCount; ++i) {
                sum.values[i] = window * next.values[i];
            }

            //update sqBuffer
            for (int i = 0; i < sqBuffer.length; ++i) {
                buffer[i].valueCount = next.valueCount;
            }

        } else {
            for (int i = 0; i < sample.valueCount; ++i) {
                sum.values[i] = sum.values[i] - buffer[index].values[i] + next.values[i];
                buffer[index].values[i] = next.values[i];

                double sq = (double) next.values[i] - (double) (sum.values[i] / window);
                sq *= sq;
                sqSum[i] = sqSum[i] - sqBuffer[index][i] + sq;
                sqBuffer[index][i] = sq;

                sample.values[i] = (float) Math.sqrt(sqSum[i] / window);
            }
            if (++index >= buffer.length)
                index = 0;
        }
        sample.timestamp = next.timestamp;
        super.filter(sample);
    }

    public void setWindowLength(int windowLength) {
        this.window = windowLength;
        this.buffer = new Sample[windowLength];
        for (int i = 0; i < this.buffer.length; ++i) {
            this.buffer[i] = new Sample(width);
        }
        this.sqBuffer = new double[windowLength][width];
        sqSum = new double[width];
    }
}
