package com.hiit.steps.filter;

import com.hiit.steps.Sample;

public class RelayFilter implements Filter {

    public static final int TYPE_RELAY = 18437; // added to original type

    private ResetFilter output;
    private boolean open;

    private Sample sample; // for now, only one sample is buffered

    @Override
    public void filter(Sample next) {
        sample.copyFrom(next);
        sample.type += TYPE_RELAY;
        if (open) {
            output.filter(sample);
        }
    }

    public RelayFilter(ResetFilter output, int width) {
        this.output = output;
        this.open = false;
        this.sample = new Sample(width);
        this.output.reset();
    }

    public class SwitchFilter implements Filter {

        @Override
        public void filter(Sample next) {
            RelayFilter.this.switchRelay(next);
        }
    }

    public Filter getSwitchFilter() {
        return new SwitchFilter();
    }

    public void switchRelay(Sample next) {
        if (!open && next.values[0] != 0) {
            open = true;
            // buffer is assumed to contain one sample with the same timestamp!
            output.filter(sample);
        } else if (open && next.values[0] == 0) {
            open = false;
            output.reset();
        }
    }
}
