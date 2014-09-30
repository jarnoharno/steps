package com.hiit.steps;

import com.hiit.steps.Connection.ConnectionClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.TimeZone;
import java.util.concurrent.Future;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.WebSocket.StringCallback;

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
        public void connectionStateChanged(State state);
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
    }

    public void stopTrace() {
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

}
