package com.hiit.steps;

public class CachedBufferQueue<T> implements CachedQueue<CachedBufferQueue.Message> {

    public enum Command {
        Continue,
        Quit
    }

    public class Message {

        private Command command = Command.Continue;
        public Buffer<T> buffer;
        Message(int size) {
            buffer = new Buffer<T>(size);
        }
        public Command getCommand() {
            return command;
        }
    }

    private CachedSynchronousQueue<Message> queue;

    public CachedBufferQueue(final int size) {
        queue = new CachedSynchronousQueue<Message>(
                new AbstractTypeFactory<Message>() {

            @Override
            public Message create() {
                return new Message(size);
            }
        });
    }

    @Override
    public Message obtain() {
        return queue.obtain();
    }

    @Override
    public void put() {
        if (queue.obtain().buffer.full()) {
            queue.put();
            queue.obtain().buffer.reset();
        }
    }

    @Override
    public Message take() {
        return queue.take();
    }

    public void quit() {
        queue.obtain().command = Command.Quit;
        queue.put();
    }
}
