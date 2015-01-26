package com.hiit.steps;

public class CachedMessageQueue<T> implements CachedQueue<CachedMessageQueue.Message> {

    public enum Command {
        Continue,
        Quit
    }

    public class Message {

        private Command command = Command.Continue;
        public T data;
        Message(TypeFactory<T> factory) {
            data = factory.create();
        }
        public Command getCommand() {
            return command;
        }
    }

    private CachedSynchronousQueue<Message> queue;

    public CachedMessageQueue(final TypeFactory<T> factory) {
        queue = new CachedSynchronousQueue<Message>(
                new AbstractTypeFactory<Message>() {

                    @Override
                    public Message create() {
                        return new Message(factory);
                    }
                });
    }

    public Message obtain() {
        return queue.obtain();
    }

    public void put() {
        queue.put();
    }

    public Message take() {
        return queue.take();
    }

    public void quit() {
        queue.obtain().command = Command.Quit;
        queue.put();
    }
}
