package com.hiit.steps;

public class SensorBuffer {

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
    private int begin;
    private int sinceLastWindow;
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

    SensorBuffer(int maxMinWindow, int minMaxLag) {
        this.bufferSize = maxMinWindow + 2 * minMaxLag;
        this.typeEntries = new EntryType[bufferSize];
        this.timestampEntries = new long[bufferSize];
        this.floatEntries = new float[bufferSize][3];
        this.index = 0;
        this.minMaxLag = minMaxLag;
        this.begin = 2 * minMaxLag;
        this.sinceLastWindow = 0;
    }

    private static float[] dummy = new float[3];

    public void put(EntryType type) {
        put(type, 0, dummy);
    }

    public void put(EntryType type, long timestamp, float[] floats) {
        if (type == EntryType.Quit) {
            syncPairPut(Message.Type.Quit, begin, index);
            return;
        }

        // fastest way?
        typeEntries[index] = type;
        timestampEntries[index] = timestamp;
        floatEntries[index][0] = floats[0];
        floatEntries[index][1] = floats[1];
        floatEntries[index][2] = floats[2];
        ++index;
        ++sinceLastWindow;
        if (sinceLastWindow == minMaxLag) {
            syncPairPut(Message.Type.Window, begin, index);
            sinceLastWindow = 0;
            begin += minMaxLag;
            if (begin >= bufferSize) {
                begin -= bufferSize;
            }
        }
        if (index == bufferSize) {
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
