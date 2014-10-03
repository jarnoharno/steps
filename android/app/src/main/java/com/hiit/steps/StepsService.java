package com.hiit.steps;

import com.hiit.steps.Trace.SamplerClient;
import com.hiit.steps.StepsProtos.Message;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
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
        connection.startTrace();
        write.startTrace();
        trace.start();
        traceStateChanged(State.STARTED);
    }

    public void stopTrace() {
        traceStateChanged(State.STOPPED);
        trace.stop();
        connection.stopTrace();
        write.stopTrace();
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

    private Client client;
    private final IBinder binder = new StepsBinder();

    public interface ContextClient {
        void print(String s);
        Context getContext();
    }

    private class StepsContextClient implements ContextClient {
        @Override
        public void print(String s) {
            StepsService.this.print(s);
        }

        @Override
        public Context getContext() {
            return StepsService.this;
        }
    }

    private class ConnectionClient extends StepsContextClient
            implements Connection.ConnectionClient {

        @Override
        public void idReceived(String id) {
            write.renameFile(id);
            print("received id: " + id);
        }
    }
    private ConnectionClient connectionClient = new ConnectionClient();
    private Connection connection = new Connection(connectionClient);

    private class TraceClient extends StepsContextClient
            implements SamplerClient {
        @Override
        public void SensorEventReceived(SensorEvent sensorEvent) {
            Message.Builder msg = Message.newBuilder()
                    .setType(Message.Type.SENSOR_EVENT)
                    .setTimestamp(sensorEvent.timestamp);
            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    msg.setId("acc");
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    msg.setId("mag");
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    msg.setId("gyr");
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    msg.setId("rot");
                    break;
                default:
                    msg.setId("unk");
                    break;
            }

            for (float value: sensorEvent.values) {
                msg.addValue(value);
            }
            byte[] data = msg.build().toByteArray();
            connection.send(data);
            write.send(data);
        }

        @Override
        public void LocationReceived(Location location) {
            byte[] data = Message.newBuilder()
                    .setType(Message.Type.LOCATION)
                    .setTimestamp(location.getElapsedRealtimeNanos())
                    .setId(location.getProvider() ==
                            LocationManager.GPS_PROVIDER ?
                            "gps" :
                            "net")
                    .setUtctime(location.getTime())
                    .setLatitude(location.getLatitude())
                    .setLongitude(location.getLongitude())
                    .setAccuracy(location.getAccuracy())
                    .setAltitude(location.getAltitude())
                    .setBearing(location.getBearing())
                    .setSpeed(location.getSpeed())
                    .build().toByteArray();
            connection.send(data);
            write.send(data);
        }
    }
    private TraceClient traceClient = new TraceClient();
    private Trace trace = new Trace(traceClient);

    private class WriteClient extends StepsContextClient
            implements Write.WriteClient {}
    private WriteClient writeClient = new WriteClient();
    private Write write = new Write(writeClient);

}
