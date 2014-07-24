package com.hiit.steps;

public class Sample {

    public int type;
    public long timestamp;
    public int valueCount;
    public float[] values;
    public Sample(int bufferWidth) {
        values = new float[bufferWidth];
    }

    public static int intBufferValueCount(int intBufferWidth) {
        return intBufferWidth - 4;
    }

    public void copyFrom(int[] buffer, int offset) {
        type = buffer[offset];
        timestamp = Conversion.intArrayToLong(buffer, offset + 1);
        valueCount = buffer[offset + 3];
        for (int i = 0; i < valueCount; ++i) {
            values[i] = Float.intBitsToFloat(buffer[offset + 4 + i]);
        }
    }

    public void copyTo(int[] buffer, int offset) {
        buffer[offset] = type;
        Conversion.longToIntArray(timestamp, buffer, offset + 1);
        buffer[offset + 3] = valueCount;
        for (int i = 0; i < valueCount; ++i) {
            buffer[offset + 4 + i] = Float.floatToRawIntBits(values[i]);
        }
    }

    public void copyFrom(Sample sample) {
        type = sample.type;
        timestamp = sample.timestamp;
        valueCount = sample.valueCount;
        for (int i = 0; i < valueCount; ++i) {
            values[i] = sample.values[i];
        }
    }
}
