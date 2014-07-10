package com.hiit.steps;

public class CachedBufferQueue<T> implements CachedQueue<CachedBufferQueue.Message<T>> {

    public static class Message<T> {
        public static enum Command {
            Continue,
            Quit
        }
        private Command command = Command.Continue;
        public Buffer<T> buffer;
        Message(int size) {
            buffer = new Buffer<T>(size);
        }
        public Command getCommand() {
            return command;
        }
    }

    private CachedSynchronousQueue<Message<T>> queue;

    public CachedBufferQueue(final int size) {
        queue = new CachedSynchronousQueue<Message<T>>(
                new AbstractTypeFactory<Message<T>>() {

            @Override
            public Message<T> create() {
                return new Message<T>(size);
            }
        });
    }

    @Override
    public Message<T> obtain() {
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
    public Message<T> take() {
        return queue.take();
    }

    public void quit() {
        queue.obtain().command = Message.Command.Quit;
        queue.put();
    }
}
