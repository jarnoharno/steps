package com.hiit.steps;

public class AlgoLoop {

    private SensorBuffer sensorBuffer;
    private Thread thread;

    private IOBuffer ioBuffer;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (;;) {
                SensorBuffer.Message message = sensorBuffer.take();
                transfer(message);
                if (message.type == SensorBuffer.Message.Type.Quit) {
                    ioBuffer.put(IOBuffer.EntryType.Quit);
                    return;
                }
            }
        }

        public void transfer(SensorBuffer.Message message) {
            if (message.end >= message.begin) {
                for (int i = message.begin; i < message.end; ++i) {
                    transferEntry(i);
                }
                return;
            }
            for (int i = message.begin; i < sensorBuffer.bufferSize; ++i) {
                transferEntry(i);
            }
            for (int i = 0; i < message.end; ++i) {
                transferEntry(i);
            }

        }

        public void transferEntry(int index) {
            ioBuffer.put(IOBuffer.EntryType.Accelerator,
                    sensorBuffer.timestampEntries[index],
                    sensorBuffer.floatEntries[index]);
        }
    };

    AlgoLoop(SensorBuffer sensorBuffer, IOBuffer ioBuffer) {
        this.thread = new Thread(runnable);
        this.sensorBuffer = sensorBuffer;
        this.ioBuffer = ioBuffer;
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        // just wait and hope the source quits the loop
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
