package com.hiit.steps;

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
