package com.hiit.steps;

import android.content.Context;
import android.hardware.Sensor;

import com.hiit.steps.filter.DelayFilter;
import com.hiit.steps.filter.Filter;
import com.hiit.steps.filter.MovingAverageFilter;
import com.hiit.steps.filter.NormFilter;
import com.hiit.steps.filter.PeakFilter;
import com.hiit.steps.filter.QueueFilter;
import com.hiit.steps.filter.RelayFilter;
import com.hiit.steps.filter.ResamplingFilter;
import com.hiit.steps.filter.SlidingStandardDeviationFilter;
import com.hiit.steps.filter.TeeFilter;
import com.hiit.steps.filter.ThresholdFilter;
import com.hiit.steps.filter.WindowedPeakFilter;

import java.util.concurrent.atomic.AtomicInteger;

public class AILoop {

    private Thread thread;

    private CachedIntArrayBufferQueue sensorQueue;
    private CachedIntArrayBufferQueue ioQueue;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (;;) {
                CachedIntArrayBufferQueue.Message message = sensorQueue.take();
                filter(message.data);
                if (message.getCommand() == CachedIntArrayBufferQueue.Command.Quit) {
                    ioQueue.quit();
                    return;
                }
            }
        }

    };

    private ResamplingFilter accResamplingFilter;
    private Filter ioFilter;
    private Filter magnFilter;
    private Filter ioMagnFilter;
    private Filter teeMagnFilter;
    private SlidingStandardDeviationFilter stdFilter;
    private Filter ioStdFilter;
    private ThresholdFilter stdThFilter; // these events are delayed by std_wnd / 2
    private Filter ioStdThFilter;
    private MovingAverageFilter maFilter; // these events are delayed by ma_wnd / 2 (< std_wnd)
    private DelayFilter delayedMaFilter; // delay by (std_wnd - ma_wnd) / 2
    private Filter ioDelayedMaFilter;
    private RelayFilter relayFilter;
    private WindowedPeakFilter peakFilter;

    private Sample sample;

    private void filter(IntArrayBuffer src) {
        for (int i = 0; i < src.getEnd(); ++i) {
            int offset = src.get(i);
            sample.copyFrom(src.buffer, offset);
            switch (sample.type) {
                case Sensor.TYPE_ACCELEROMETER:
                    accResamplingFilter.filter(sample);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    break;
                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    break;
            }
            ioFilter.filter(sample);
        }
    }

    private long resamplingRate = 20000000;     // 20 ms

    // parameter values from Brajdic, Harle, 2013

    private int stdWindow = 30;                 // 0.6 s
    private float stdThreshold = 1.0f;          // 1.0 m/s^2
    private int maWindow = 15;                  // 0.30 s
    private long peakMinDistance = 590000000L;  // 0.59 s

    private int peakWindow = 200;               // 4 s

    AILoop(Context context,
           CachedIntArrayBufferQueue sensorQueue,
           CachedIntArrayBufferQueue ioQueue,
           StepsCallback stepsCallback) {
        int width = Sample.intBufferValueCount(sensorQueue.getWidth());
        this.ioQueue = ioQueue;

        // filters created from end to beginning in pipeline
        this.ioFilter = new QueueFilter(ioQueue);
        this.peakFilter = new WindowedPeakFilter(ioFilter, peakMinDistance, peakWindow, stepsCallback);
        this.relayFilter = new RelayFilter(peakFilter, width);
        this.ioDelayedMaFilter = new TeeFilter(ioFilter, relayFilter);
        this.delayedMaFilter = new DelayFilter(ioDelayedMaFilter, width, (stdWindow - maWindow) / 2, resamplingRate);
        this.maFilter = new MovingAverageFilter(delayedMaFilter, width, maWindow);
        this.ioStdThFilter = new TeeFilter(ioFilter, relayFilter.getSwitchFilter());
        this.stdThFilter = new ThresholdFilter(ioStdThFilter, stdThreshold);
        this.ioStdFilter = new TeeFilter(ioFilter, stdThFilter);
        this.stdFilter = new SlidingStandardDeviationFilter(ioStdFilter, width, stdWindow);
        this.teeMagnFilter = new TeeFilter(maFilter, stdFilter); // maFilter MUST be first!
        this.ioMagnFilter = new TeeFilter(ioFilter, teeMagnFilter);
        this.magnFilter = new NormFilter(ioMagnFilter);
        this.accResamplingFilter = new ResamplingFilter(magnFilter, width, resamplingRate);

        this.sample = new Sample(width);
        this.thread = new Thread(runnable);
        this.sensorQueue = sensorQueue;
    }

    public void start() {
        thread.start();
    }

    public int getSteps() {
        return peakFilter.getSteps();
    }

    public void setMaWindow(int window) {
        this.maWindow = window;
        delayedMaFilter.setSampleDelay((stdWindow - maWindow) / 2);
        maFilter.setWindowLength(maWindow);
    }

    public void setStdWindow(int window) {
        this.stdWindow = window;
        delayedMaFilter.setSampleDelay((stdWindow - maWindow) / 2);
        stdFilter.setWindowLength(window);
    }

    public void setResampleRate(long resampleRate) {
        this.resamplingRate = resampleRate;
        delayedMaFilter.setSampleRate(resampleRate);
        accResamplingFilter.setResampleRate(resampleRate);
    }

    public void setStdThreshold(float threshold) {
        this.stdThreshold = threshold;
        this.stdThFilter.setThreshold(threshold);
    }
}
