package com.hiit.steps;

import com.hiit.steps.Connection.ConnectionClient;
import com.hiit.steps.Trace.SamplerClient;
import com.hiit.steps.StepsProtos.Sample;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class StepsService extends Service {

    public enum State {
        STARTING,
        STARTED,
        STOPPING,
        STOPPED;

        public boolean hasStarted() {
            return this == STARTING || this == STARTED;
        }

        public boolean hasStopped() {
            return this == STOPPING || this == STOPPED;
        }
    }

    // public interface

    public static interface Client {
        public void print(String msg);
        public void serviceStateChanged(State state);
        public void traceStateChanged(State state);
    }

    public void addClient(Client client) {
        this.client = client;
    }

    public void removeClient(Client client) {
        this.client = null;
    }

    public State getServiceState() {
        return serviceState;
    }

    public State getTraceState() {
        return traceState;
    }

    public Deque<String> getOutputBuffer() {
        return outputBuffer;
    }

    public void startService() {
        Intent intent = new Intent(this, StepsService.class);
        serviceStateChanged(State.STARTING);
        startService(intent);
    }

    public void stopService() {
        Intent intent = new Intent(this, StepsService.class);
        serviceStateChanged(State.STOPPING);
        stopService(intent);
        connection.disconnect();
        serviceStateChanged(State.STOPPED);
    }

    public void startTrace() {
        trace.start();
        traceStateChanged(State.STARTED);
    }

    public void stopTrace() {
        traceStateChanged(State.STOPPED);
        trace.stop();
    }

    public static boolean bind(Context context, ServiceConnection serviceConnection) {
        Intent intent = new Intent(context, StepsService.class);
        return context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public class StepsBinder extends Binder {
        public StepsService getService() {
            return StepsService.this;
        }
    }

    // private parts

    private Deque<String> outputBuffer = new ArrayDeque<String>();

    void serviceStateChanged(State state) {
        serviceState = state;
        if (client != null) {
            client.serviceStateChanged(state);
        }
    }

    void traceStateChanged(State state) {
        traceState = state;
        if (client != null) {
            client.traceStateChanged(state);
        }
    }
    
    private State serviceState = State.STOPPED;
    private State traceState = State.STOPPED;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceStateChanged(State.STARTED);
        connection.connect();
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        print("Service created");
    }

    @Override
    public void onDestroy() {
        connection.disconnect();
        print("Service destroyed");
        super.onDestroy();
    }

    private final int MAX_OUTPUT_BUFFER_LINES = 1024;

    //private TimeZone timeZone = TimeZone.getTimeZone("UTC");
    //private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private void print(String s) {
        Log.i("Steps", s);
        String line = "[" + dateFormat.format(new Date()) + "] Service: " + s;
        if (outputBuffer.size() >= MAX_OUTPUT_BUFFER_LINES) {
            outputBuffer.pop();
        }
        outputBuffer.push(line);
        if (client == null) {
            return;
        }
        client.print(line);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private Handler handler = new Handler();
    private Client client;
    private final IBinder binder = new StepsBinder();
    private Connection connection = new Connection(new ConnectionClient() {

        @Override
        public void print(String s) {
            StepsService.this.print(s);
        }

        @Override
        public Context getContext() {
            return StepsService.this;
        }
    });

    private Trace trace = new Trace(new SamplerClient() {
        @Override
        public void SensorEventReceived(SensorEvent sensorEvent) {
            Sample.Builder sample = Sample.newBuilder()
                    .setType(Sample.Type.SENSOR_EVENT)
                    .setTimestamp(sensorEvent.timestamp);
            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    sample.setName("acc");
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    sample.setName("mag");
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    sample.setName("gyr");
                    break;
                default:
                    sample.setName("unk");
                    break;
            }
            StepsProtos.SensorEvent.Builder builder =
                    StepsProtos.SensorEvent.newBuilder();
            for (float value: sensorEvent.values) {
                builder.addValue(value);
            }
            sample.setSensorEvent(builder);
            connection.send(sample.build().toByteArray());
        }

        @Override
        public void LocationReceived(Location location) {
            Sample sample = Sample.newBuilder()
                    .setType(Sample.Type.LOCATION)
                    .setTimestamp(location.getElapsedRealtimeNanos())
                    .setName(location.getProvider() ==
                            LocationManager.GPS_PROVIDER ?
                            "gps" :
                            "net")
                    .setLocation(StepsProtos.Location.newBuilder()
                            .setUtctime(location.getTime())
                            .setLatitude(location.getLatitude())
                            .setLongitude(location.getLongitude())
                            .setAccuracy(location.getAccuracy())
                            .setAltitude(location.getAltitude())
                            .setBearing(location.getBearing())
                            .setSpeed(location.getSpeed())
                    )
                    .build();
            connection.send(sample.toByteArray());
        }

        @Override
        public Context getContext() {
            return StepsService.this;
        }

        @Override
        public void print(String s) {
            StepsService.this.print(s);
        }
    });

}
