package com.hiit.steps;

public class AILoop {

    private WindowQueue windowQueue;
    private Thread thread;

    private IOBuffer ioBuffer;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (;;) {
                WindowQueue.Window window = windowQueue.take();
                transfer(window);
                if (window.command == WindowQueue.Window.Command.Quit) {
                    ioBuffer.put(IOBuffer.EntryType.Quit);
                    return;
                }
            }
        }

        public void transfer(WindowQueue.Window window) {
            if (window.end >= window.begin) {
                for (int i = window.begin; i < window.end; ++i) {
                    transferEntry(i);
                }
                return;
            }
            for (int i = window.begin; i < windowQueue.buffer.length; ++i) {
                transferEntry(i);
            }
            for (int i = 0; i < window.end; ++i) {
                transferEntry(i);
            }

        }

        public void transferEntry(int index) {
            //ioBuffer.put(windowBuffer.buffer[index]);
        }
    };

    AILoop(WindowQueue windowQueue, IOBuffer ioBuffer) {
        this.thread = new Thread(runnable);
        this.windowQueue = windowQueue;
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
