package com.hiit.steps;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Deque;
import java.util.List;

import javax.net.ssl.SSLContext;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import com.hiit.steps.StepsProtos.Sample;
import com.hiit.steps.StepsProtos.SensorEvent;

public class StepsService extends Service {

    public enum State {
        STARTING,
        STARTED,
        STOPPING,
        STOPPED
    }

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
        client.serviceStateChanged(State.STARTING);
        context.startService(intent);
    }

    public void stop(Context context) {
        Intent intent = new Intent(context, StepsService.class);
        client.serviceStateChanged(State.STOPPING);
        context.stopService(intent);
        disconnect();
        client.serviceStateChanged(State.STOPPED);
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

    private State serviceState = State.STOPPED;
    private State traceState = State.STOPPED;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connect("wss://whoop.pw/steps/ws");
        client.serviceStateChanged(State.STARTED);
        return START_REDELIVER_INTENT;
    }

    private void disconnect() {
        webSocketClient.close();
    }

    private void connect(String addr) {
        URI uri;
        try {
            uri = new URI(addr);
        } catch (URISyntaxException e) {
            print(e.toString());
            e.printStackTrace();
            return;
        }

        //WebSocketImpl.DEBUG = true;

        webSocketClient = new WebSocketClient(uri, new Draft_17()) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                print("websocket opened");

                // Send sample

                /*
                Sample sample = Sample.newBuilder()
                        .setName("acc")
                        .setTimestamp(System.currentTimeMillis() * 1000000)
                        .setType(Sample.Type.SENSOR_EVENT)
                        .setSensorEvent(SensorEvent.newBuilder()
                                        .addValue(0.0f)
                                        .addValue(0.0f)
                                        .addValue(9.8f)
                        )
                        .build();

                byte[] data = sample.toByteArray();
                webSocketClient.send(data);

                print("sent sample: " + sample.toString());
                print("sent data: " + Arrays.toString(data));
                */
            }

            @Override
            public void onMessage(String s) {
                print("received message: " + s);
            }

            @Override
            public void onMessage(ByteBuffer buffer) {
                byte[] data = buffer.array();
                print("received data: " + Arrays.toString(data));
                StepsProtos.Sample sample;
                try {
                    sample = StepsProtos.Sample.parseFrom(data);
                } catch (InvalidProtocolBufferException ex) {
                    print("unable to parse data");
                    return;
                }
                print("received sample: " + sample.toString());
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                print("websocket closed");
                webSocketClient = null;
            }

            @Override
            public void onError(Exception e) {
                print("websocket error: " + e.getMessage());
            }
        };
        print("opening websocket");
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException ex) {
            print("Unrecognized algorithm");
            return;
        }
        try {
            // for self-signed keys
            //sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(),
            //      null);
            sslContext.init(null, null, null); // default
        } catch (KeyManagementException ex) {
            print("Error initializing SSL context");
            return;
        }
        webSocketClient.setWebSocketFactory(
                new DefaultSSLWebSocketClientFactory(sslContext));
        webSocketClient.connect();
    }

    @Override
    public void onDestroy() {
        if (webSocketClient != null &&
                webSocketClient.getReadyState() != WebSocket.READYSTATE.CLOSING &&
                webSocketClient.getReadyState() != WebSocket.READYSTATE.CLOSED) {
            webSocketClient.close();
        }
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

    private Client client;
    private WebSocketClient webSocketClient;
    private final IBinder binder = new StepsBinder();

}
