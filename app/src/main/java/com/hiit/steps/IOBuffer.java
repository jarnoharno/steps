package com.hiit.steps;

public class IOBuffer {

    public static enum EntryType {
        Accelerator,
        Quit
    }
    public int bufferSize;
    public EntryType typeEntries[];
    public long timestampEntries[];
    public float floatEntries[][];
    private int index;
    private int minMaxLag;
    private SyncPair<Message> syncPair = new SyncPair<Message>(messageFactory);

    public static class Message {
        public static enum Type {
            Window,
            Quit
        }
        public Type type;
        public int begin;
        public int end;
    }

    private void syncPairPut(Message.Type type, int begin, int end) {
        Message message = syncPair.obtain();
        message.type = type;
        message.begin = begin;
        message.end = end;
        syncPair.put();
    }

    IOBuffer(int minMaxLag) {
        this.minMaxLag = minMaxLag;
        this.bufferSize = 2 * minMaxLag;
        this.typeEntries = new EntryType[bufferSize];
        this.timestampEntries = new long[bufferSize];
        this.floatEntries = new float[bufferSize][3];
        this.index = 0;
    }

    private static float[] dummy = new float[3];

    public void put(EntryType type) {
        put(type, 0, dummy);
    }

    public void put(EntryType type, long timestamp, float[] floats) {
        if (type == EntryType.Quit) {
            if (index < minMaxLag) {
                syncPairPut(Message.Type.Quit, 0, index);
            } else {
                syncPairPut(Message.Type.Quit, minMaxLag, index);
            }
            return;
        }

        // fastest way?
        typeEntries[index] = type;
        timestampEntries[index] = timestamp;
        floatEntries[index][0] = floats[0];
        floatEntries[index][1] = floats[1];
        floatEntries[index][2] = floats[2];
        ++index;
        if (index == minMaxLag) {
            syncPairPut(Message.Type.Window, 0, minMaxLag);
        } else if (index == bufferSize) {
            syncPairPut(Message.Type.Window, minMaxLag, bufferSize);
            index = 0;
        }
    }

    public Message take() {
        return syncPair.take();
    }

    private static SyncPair.Factory<Message> messageFactory =
            new SyncPair.Factory<Message>() {
                @Override
                public Message create() {
                    return new Message();
                }
            };
}
