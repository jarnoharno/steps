package com.hiit.steps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

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

    private Filter accResamplingFilter;
    private Filter ioFilter;
    private Filter ioAccrFilter;
    private Filter accrNormFilter;
    private Filter ioAccrNormFilter;
    private Filter PowerFilter;
    private Filter ioPowerFilter;
    private Filter averagePowerFilter;

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

    AILoop(Context context,
           CachedIntArrayBufferQueue sensorQueue,
           CachedIntArrayBufferQueue ioQueue,
           StepsCallback stepsCallback) {
        int width = Sample.intBufferValueCount(sensorQueue.getWidth());
        this.ioQueue = ioQueue;

        // filters created from end to beginning in pipeline
        this.ioFilter = new QueueFilter(ioQueue);
        this.averagePowerFilter = new MovingAverageFilter(ioFilter, 3);
        this.ioPowerFilter = new TeeFilter(ioFilter, averagePowerFilter);
        this.PowerFilter = new SquareFilter(ioPowerFilter, width);
        this.ioAccrNormFilter = new TeeFilter(ioFilter, PowerFilter);
        this.accrNormFilter = new NormFilter(ioAccrNormFilter, SensorManager.STANDARD_GRAVITY);
        this.ioAccrFilter = new TeeFilter(ioFilter, accrNormFilter);
        this.accResamplingFilter = new ResamplingFilter(ioAccrFilter, width);

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
}
