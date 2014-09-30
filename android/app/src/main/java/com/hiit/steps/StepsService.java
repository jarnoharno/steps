package com.hiit.steps;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.Deque;

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

    public static int RETRY_DELAY_MILLIS = 5000; // 5 s

    // public interface

    public static interface Client {
        public void print(String msg);
        public void serviceStateChanged(State state);
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

    public void start(Context context) {
        Intent intent = new Intent(context, StepsService.class);
        serviceStateChanged(State.STARTING);
        context.startService(intent);
    }

    public void stop(Context context) {
        Intent intent = new Intent(context, StepsService.class);
        serviceStateChanged(State.STOPPING);
        context.stopService(intent);
        disconnect();
        handler.removeCallbacks(retryConnectRunnable);
        serviceStateChanged(State.STOPPED);
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

    void connectionStateChanged(State state) {
        connectionState = state;
    }
    
    private State serviceState = State.STOPPED;
    private State connectionState = State.STOPPED;
    private State traceState = State.STOPPED;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceStateChanged(State.STARTED);
        tryConnect();
        return START_REDELIVER_INTENT;
    }

    private void disconnect() {
        if (connectionState.hasStarted()) {
            connectionState = State.STOPPING;
            if (webSocket != null) {
                webSocket.close();
                // throw client away in any case if initiating close
                webSocket = null;
            }
        }
    }

    private void tryConnect() {
        if (!hasConnectivity()) {
            // set connectivity receiver to try connect when network becomes
            // available
            enableConnectivityReceiver(true);
        } else {
            connect();
        }
    }

    private void retryConnect() {
        retryConnect(0);
    }

    private void retryConnect(int delayMillis) {
        handler.postDelayed(retryConnectRunnable, delayMillis);
    }

    private Runnable retryConnectRunnable = new Runnable() {

        @Override
        public void run() {
            if (serviceState.hasStarted()) {
                tryConnect();
            }
        }
    };

    private void connect() {
        enableConnectivityReceiver(false);

        print("Opening websocket");
        AsyncHttpClient.getDefaultInstance()
                .websocket("wss://whoop.pw/steps/ws", null,
                        new WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                if (ex != null) {
                    print(ex.toString());
                    retryConnect(RETRY_DELAY_MILLIS);
                    return;
                }
                if (webSocket == null) {
                    print("Failed to open websocket");
                    retryConnect(RETRY_DELAY_MILLIS);
                    return;
                }
                connectionStateChanged(State.STARTED);
                print("Websocket opened");

                StepsService.this.webSocket = webSocket;

                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        connectionStateChanged(State.STOPPED);
                        print("Websocket closed");
                        retryConnect(RETRY_DELAY_MILLIS);
                    }
                });
                webSocket.setStringCallback(new StringCallback() {
                    public void onStringAvailable(String s) {
                        System.out.println("I got a string: " + s);
                    }
                });
                webSocket.setDataCallback(new DataCallback() {
                    public void onDataAvailable(DataEmitter emitter,
                                                ByteBufferList byteBufferList) {
                        System.out.println("I got some bytes!");
                        // note that this data has been read
                        byteBufferList.recycle();
                    }
                });
            }
        });
    }

    private boolean hasConnectivity() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        enableConnectivityReceiver(false);
    }

    private void enableConnectivityReceiver(boolean enabled) {
        if (enabled && !connectivityReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(connectivityReceiver, intentFilter);
            connectivityReceiverRegistered = true;
            print("Registered connectivity receiver");
        } else if (!enabled && connectivityReceiverRegistered) {
            unregisterReceiver(connectivityReceiver);
            connectivityReceiverRegistered = false;
            print("Unregistered connectivity receiver");
        }
    }

    @Override
    public void onDestroy() {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.close();
        }
        enableConnectivityReceiver(false);
        super.onDestroy();
    }

    private final int MAX_OUTPUT_BUFFER_LINES = 1024;

    private void print(String s) {
        if (outputBuffer.size() >= MAX_OUTPUT_BUFFER_LINES) {
            outputBuffer.pop();
        }
        outputBuffer.push(s);
        if (client == null) {
            return;
        }
        client.print(s);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private Handler handler = new Handler();
    private Client client;
    private WebSocket webSocket;
    private final IBinder binder = new StepsBinder();
    private BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tryConnect();
        }
    };
    private boolean connectivityReceiverRegistered = false;

}
