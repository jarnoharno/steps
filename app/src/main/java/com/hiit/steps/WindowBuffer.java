package com.hiit.steps;

public class WindowBuffer {

    public int bufferLength;
    public int bufferWidth;
    public int buffer[];
    public int index;

    private int minMaxLag;
    private int begin;
    private int sinceLastWindow;
    private CachedSynchronousQueue<Window> cachedSynchronousQueue =
            new CachedSynchronousQueue<Window>(windowFactory);

    public static class Window {
        public static enum Command {
            Continue,
            Quit
        }
        public Command command;
        public int begin;
        public int end;
    }

    private void syncPairPut(Window.Command command, int begin, int end) {
        Window window = cachedSynchronousQueue.obtain();
        window.command = command;
        window.begin = begin;
        window.end = end;
        cachedSynchronousQueue.put();
    }

    WindowBuffer(int width, int minMaxLag, int maxMinWindow) {
        this.bufferLength = maxMinWindow + 2 * minMaxLag;
        this.bufferWidth = width;
        this.buffer = new int[bufferLength * bufferWidth];
        this.index = 0;
        this.minMaxLag = minMaxLag;
        this.begin = 2 * minMaxLag;
        this.sinceLastWindow = 0;
    }

    public void quit() {
        syncPairPut(Window.Command.Quit, begin, index);
    }

    public void put(int[] element) {
        put(element, 0, element.length);
    }

    public void put(int[] element, int srcPos, int length) {
        System.arraycopy(element, srcPos, buffer, index * bufferWidth, length);
    }

    public void next() {
        ++index;
        ++sinceLastWindow;
        if (sinceLastWindow == minMaxLag) {
            syncPairPut(Window.Command.Continue, begin, index);
            sinceLastWindow = 0;
            begin += minMaxLag;
            if (begin >= bufferLength) {
                begin -= bufferLength;
            }
        }
        if (index == bufferLength) {
            index = 0;
        }
    }

    public Window take() {
        return cachedSynchronousQueue.take();
    }
    private static TypeFactory<Window> windowFactory =
            new AbstractTypeFactory<Window>() {
                @Override
                public Window create() {
                    return new Window();
                }
            };
}
