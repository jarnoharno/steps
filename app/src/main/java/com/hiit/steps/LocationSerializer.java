package com.hiit.steps;

import android.location.Location;
import android.location.LocationManager;

import com.hiit.steps.Conversion;
import com.hiit.steps.IntArrayBuffer;

import java.util.Formatter;

public class LocationSerializer {

    public static final int UNKNOWN_PROVIDER = -1;
    public static final int GPS_PROVIDER = -2;
    public static final int NETWORK_PROVIDER = -3;

    public static void toIntArray(Location location, IntArrayBuffer buffer) {
        toIntArray(location, buffer.buffer, buffer.obtain());
    }

    public static void toIntArray(Location l, int[] buffer, int offset) {
        if (l.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            buffer[offset] = GPS_PROVIDER;
        } else if (l.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
            buffer[offset] = NETWORK_PROVIDER;
        } else {
            buffer[offset] = UNKNOWN_PROVIDER;
        }
        Conversion.longToIntArray(l.getElapsedRealtimeNanos(), buffer, offset + 1);
        Conversion.longToIntArray(l.getTime(), buffer, offset + 3);
        writeDouble(l.getLatitude(), buffer, offset + 5);
        writeDouble(l.getLongitude(), buffer, offset + 7);
        writeFloat(l.hasAccuracy() ? l.getAccuracy() : Float.NaN, buffer, offset + 9);
        writeDouble(l.hasAltitude() ? l.getAltitude() : Double.NaN, buffer, offset + 10);
        writeFloat(l.hasBearing() ? l.getBearing() : Float.NaN, buffer, offset + 12);
        writeFloat(l.hasSpeed() ? l.getSpeed() : Float.NaN, buffer, offset + 13);
    }

    private static void writeFloat(float f, int[] buffer, int offset) {
        buffer[offset] = Float.floatToRawIntBits(f);
    }

    private static void writeDouble(double d, int[] buffer, int offset) {
        long l = Double.doubleToRawLongBits(d);
        Conversion.longToIntArray(l, buffer, offset);
    }

    public static void formatIntArray(int[] buffer, int offset, Formatter formatter) {
        int type = buffer[offset];
        long timestamp = Conversion.intArrayToLong(buffer, offset + 1);
        long utctime = Conversion.intArrayToLong(buffer, offset + 3);
        double lat = readDouble(buffer, offset + 5);
        double lon = readDouble(buffer, offset + 7);
        float acc = readFloat(buffer, offset + 9);
        double alt = readDouble(buffer, offset + 10);
        float bea = readFloat(buffer, offset + 12);
        float spe = readFloat(buffer, offset + 13);
        switch (type) {
            case UNKNOWN_PROVIDER:
                formatter.format("unk  ");
                break;
            case GPS_PROVIDER:
                formatter.format("gps  ");
                break;
            case NETWORK_PROVIDER:
                formatter.format("net  ");
                break;
        }
        formatter.format(" %d", timestamp);
        formatter.format(" %d", utctime);
        formatter.format(" %.16g", lat);
        formatter.format(" %.16g", lon);
        formatter.format(" %.8g", acc);
        formatter.format(" %.16g", alt);
        formatter.format(" %.8g", bea);
        formatter.format(" %.8g", spe);
        formatter.format("\n");
    }

    private static double readDouble(int[] buffer, int offset) {
        long l = Conversion.intArrayToLong(buffer, offset);
        double d = Double.longBitsToDouble(l);
        return d;
    }

    private static float readFloat(int[] buffer, int offset) {
        return Float.intBitsToFloat(buffer[offset]);
    }
}
