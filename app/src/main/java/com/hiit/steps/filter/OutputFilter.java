package com.hiit.steps.filter;

import com.hiit.steps.Sample;
import com.hiit.steps.filter.Filter;

public class OutputFilter implements Filter {

    private Filter output;

    public OutputFilter(Filter output) {
        this.output = output;
    }

    @Override
    public void filter(Sample sample) {
        output.filter(sample);
    }
}
