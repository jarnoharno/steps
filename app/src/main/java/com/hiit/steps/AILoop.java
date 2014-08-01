package com.hiit.steps;

import android.content.Context;
import android.hardware.Sensor;

import com.hiit.steps.filter.DelayFilter;
import com.hiit.steps.filter.Filter;
import com.hiit.steps.filter.NormFilter;
import com.hiit.steps.filter.QueueFilter;
import com.hiit.steps.filter.ResamplingFilter;
import com.hiit.steps.filter.SampleDelayFilter;
import com.hiit.steps.filter.SlidingStandardDeviationFilter;
import com.hiit.steps.filter.TeeFilter;
import com.hiit.steps.filter.TimeShiftFilter;

import java.util.concurrent.atomic.AtomicInteger;

public class AILoop {

    private Thread thread;

    private CachedIntArrayBufferQueue sensorQueue;
    private CachedIntArrayBufferQueue ioQueue;

    private AtomicInteger steps = new AtomicInteger();

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
    //private Filter ioAccrFilter;
    private Filter magnFilter;
    private Filter teeMagnFilter;
    //private Filter ioMagnFilter;
    private SlidingStandardDeviationFilter stdFilter;
    private DelayFilter delayedMagnFilter;

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
            //ioFilter.filter(sample);
        }
    }

    private int resamplingRate = 10000; // 10 ms
    private int meanPowerWindow = 100; // 1 s

    AILoop(Context context,
           CachedIntArrayBufferQueue sensorQueue,
           CachedIntArrayBufferQueue ioQueue,
           StepsCallback stepsCallback) {
        int width = Sample.intBufferValueCount(sensorQueue.getWidth());
        this.ioQueue = ioQueue;

        // filters created from end to beginning in pipeline
        this.ioFilter = new QueueFilter(ioQueue);
        this.delayedMagnFilter = new DelayFilter(ioFilter, width, meanPowerWindow / 2, resamplingRate);
        //this.delayedStdFilter = new DelayFilter(ioFilter, width, meanPowerWindow / 2, resamplingRate);
        //this.ioStdFilter = new TeeFilter(ioFilter, delayedStdFilter);
        this.stdFilter = new SlidingStandardDeviationFilter(ioFilter, width, meanPowerWindow);
        //this.ioMagnFilter = new TeeFilter(ioFilter, stdFilter);
        this.teeMagnFilter = new TeeFilter(delayedMagnFilter, stdFilter);
        this.magnFilter = new NormFilter(teeMagnFilter);
        //this.ioAccrFilter = new TeeFilter(ioFilter, magnFilter);
        this.accResamplingFilter = new ResamplingFilter(magnFilter, width, resamplingRate);

        this.sample = new Sample(width);
        this.thread = new Thread(runnable);
        this.sensorQueue = sensorQueue;
    }

    public void start() {
        thread.start();
    }

    public int getSteps() {
        return steps.get();
    }

    public void setMeanPowerWindow(int meanPowerWindow) {
        this.meanPowerWindow = meanPowerWindow;
        delayedMagnFilter.setSampleDelay(meanPowerWindow / 2);
        stdFilter.setWindowLength(meanPowerWindow);
    }

    public void setResampleRate(int resampleRate) {
        this.resamplingRate = resampleRate;
        delayedMagnFilter.setSampleRate(resampleRate);
        accResamplingFilter.setResampleRate(resampleRate);
    }
}
