package com.hiit.steps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import com.hiit.steps.filter.DelayFilter;
import com.hiit.steps.filter.MovingAverageFilter;
import com.hiit.steps.filter.NormFilter;
import com.hiit.steps.filter.PeakFilter;
import com.hiit.steps.filter.RelayFilter;
import com.hiit.steps.filter.ResamplingFilter;
import com.hiit.steps.filter.SlidingStandardDeviationFilter;
import com.hiit.steps.filter.SquareFilter;
import com.hiit.steps.filter.ThresholdFilter;
import com.hiit.steps.filter.TimeShiftFilter;
import com.hiit.steps.filter.WindowedPeakFilter;

import java.sql.Time;
import java.util.Formatter;

public class SensorEventSerializer {

    public static void toIntArray(SensorEvent event, IntArrayBuffer buffer) {
        toIntArray(event, buffer.buffer, buffer.obtain());
    }

    public static void toIntArray(SensorEvent event, int[] buffer, int offset) {
        buffer[offset] = event.sensor.getType();
        Conversion.longToIntArray(event.timestamp, buffer, offset + 1);
        buffer[offset + 3] = event.values.length;
        for (int i = 0; i < event.values.length; ++i) {
            buffer[offset + 4 + i] = Float.floatToRawIntBits(event.values[i]);
        }
    }

    public static void formatIntArray(int[] buffer, int offset, Formatter formatter) {
        int type = buffer[offset];
        long timestamp = Conversion.intArrayToLong(buffer, offset + 1);
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                formatter.format("acc  ");
                break;
            case Sensor.TYPE_GYROSCOPE:
                formatter.format("gyr  ");
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                formatter.format("mag  ");
                break;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                formatter.format("gyu  ");
                break;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                formatter.format("mau  ");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE:
                formatter.format("accr ");
                break;
            case Sensor.TYPE_GYROSCOPE + ResamplingFilter.TYPE_RESAMPLE:
                formatter.format("gyrr ");
                break;
            case Sensor.TYPE_MAGNETIC_FIELD + ResamplingFilter.TYPE_RESAMPLE:
                formatter.format("magr ");
                break;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED + ResamplingFilter.TYPE_RESAMPLE:
                formatter.format("gyur ");
                break;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED + ResamplingFilter.TYPE_RESAMPLE:
                formatter.format("maur ");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE + NormFilter.TYPE_NORM:
                formatter.format("magn ");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE +
                    NormFilter.TYPE_NORM + SquareFilter.TYPE_SQUARE:
                formatter.format("pow  ");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE +
                    NormFilter.TYPE_NORM + SquareFilter.TYPE_SQUARE +
                    MovingAverageFilter.TYPE_MOVING_AVERAGE:
                formatter.format("powa ");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE +
                    NormFilter.TYPE_NORM + SquareFilter.TYPE_SQUARE +
                    MovingAverageFilter.TYPE_MOVING_AVERAGE +
                    DelayFilter.TYPE_DELAY:
                formatter.format("powad");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE +
                    NormFilter.TYPE_NORM +
                    SlidingStandardDeviationFilter.TYPE_STANDARD_DEVIATION:
                formatter.format("std  ");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE +
                    NormFilter.TYPE_NORM +
                    SlidingStandardDeviationFilter.TYPE_STANDARD_DEVIATION +
                    DelayFilter.TYPE_DELAY:
                formatter.format("stdd ");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE +
                    NormFilter.TYPE_NORM +
                    SlidingStandardDeviationFilter.TYPE_STANDARD_DEVIATION +
                    TimeShiftFilter.TYPE_TIME_SHIFT: // :/
                formatter.format("stdt ");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE +
                    NormFilter.TYPE_NORM +
                    DelayFilter.TYPE_DELAY:
                formatter.format("magnd");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE +
                    NormFilter.TYPE_NORM +
                    SlidingStandardDeviationFilter.TYPE_STANDARD_DEVIATION +
                    ThresholdFilter.TYPE_THRESHOLD:
                formatter.format("stdth");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE +
                    NormFilter.TYPE_NORM +
                    MovingAverageFilter.TYPE_MOVING_AVERAGE +
                    DelayFilter.TYPE_DELAY:
                formatter.format("mad  ");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE +
                    NormFilter.TYPE_NORM +
                    MovingAverageFilter.TYPE_MOVING_AVERAGE +
                    DelayFilter.TYPE_DELAY + RelayFilter.TYPE_RELAY +
                    PeakFilter.TYPE_PEAK:
                formatter.format("map  ");
                break;
            case Sensor.TYPE_ACCELEROMETER + ResamplingFilter.TYPE_RESAMPLE +
                    NormFilter.TYPE_NORM +
                    MovingAverageFilter.TYPE_MOVING_AVERAGE +
                    DelayFilter.TYPE_DELAY + RelayFilter.TYPE_RELAY +
                    WindowedPeakFilter.TYPE_WINDOWED_PEAK:
                formatter.format("map  ");
                break;
            default:
                formatter.format("x%d", type);
                break;
        }
        formatter.format(" %d", timestamp);
        writeFloats(buffer, offset + 4, buffer[offset + 3], formatter);
        formatter.format("\n");
    }

    private static void writeFloats(int[] buffer, int offset, int length, Formatter formatter) {
        for (int i = 0; i < length; ++i) {
            formatter.format(" %.8g", Float.intBitsToFloat(buffer[offset + i]));
        }
    }

}
