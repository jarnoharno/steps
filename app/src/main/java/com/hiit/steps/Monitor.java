package com.hiit.steps;

public class Monitor {
    private boolean hold = true;
    public synchronized void release() {
        hold = false;
        notifyAll();
    }
    public synchronized void block() {
        while (hold) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
