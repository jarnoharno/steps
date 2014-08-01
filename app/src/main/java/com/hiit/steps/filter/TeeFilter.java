package com.hiit.steps.filter;

import com.hiit.steps.Sample;
import com.hiit.steps.filter.Filter;

public class TeeFilter implements Filter {
    private Filter output1;
    private Filter output2;
    public TeeFilter(Filter output1, Filter output2) {
        this.output1 = output1;
        this.output2 = output2;
    }

    @Override
    public void filter(Sample sample) {
        output1.filter(sample);
        output2.filter(sample);
    }
}
