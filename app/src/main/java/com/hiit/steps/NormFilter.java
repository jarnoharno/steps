package com.hiit.steps;

public class NormFilter implements Filter {

    public static final int TYPE_NORM = 2000; // added to original type

    private Filter output;
    private Sample sample;
    private float offset;

    NormFilter(Filter output) {
        this(output, 0);
    }

    NormFilter(Filter output, float offset) {
        this.output = output;
        this.sample = new Sample(1);
        this.sample.valueCount = 1;
        this.offset = offset;
    }

    @Override
    public void filter(Sample src) {
        sample.copyHeaderForm(src);
        sample.type += TYPE_NORM;
        float v = 0;
        for (int i = 0; i < src.valueCount; ++i) {
            float x = src.values[i];
            v += x * x;
        }
        v = (float) Math.sqrt(v) - offset;
        sample.values[0] = v;
        output.filter(sample);
    }
}
