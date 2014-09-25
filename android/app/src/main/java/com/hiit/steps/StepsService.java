package com.hiit.steps;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class StepsService extends Service {

    // public interface

    public static interface Logger {
        public void send(String msg);
    }

    public void addLogger(Logger logger) {
        this.logger = logger;
    }

    public void removeLogger(Logger logger) {
        this.logger = null;
    }

    public void start(Context context) {
        Intent intent = new Intent(context, StepsService.class);
        context.startService(intent);
        started = true;
    }

    public void stop(Context context) {
        Intent intent = new Intent(context, StepsService.class);
        context.stopService(intent);
        started = false;
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

    boolean started = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connect("ws://whoop.pw:8080/steps/ws");
        return START_STICKY;
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

        print("opening websocket");
        webSocketClient = new WebSocketClient(uri, new Draft_17()) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                print("websocket opened");

                // Send sample

                StepsProtos.Sample sample = StepsProtos.Sample.newBuilder()
                    .setType("acc")
                    .setTimestamp(System.currentTimeMillis() * 1000000)
                    .addValue(0.0f)
                    .addValue(0.0f)
                    .addValue(9.8f)
                    .build();

                byte[] data = sample.toByteArray();
                webSocketClient.send(data);

                print("sent sample: " + sample.toString());
                print("sent data: " + Arrays.toString(data));
            }

            @Override
            public void onMessage(String s) {
                Log.i("Websocket", "Message " + s);
                print("received message: " + s);
            }

            @Override
            public void onMessage(ByteBuffer buffer) {
                byte[] data = data.array();
                print("received data: " + buffer.toString());
                StepsProtos.Sample sample;
                try {
                    sample = StepsProtos.Sample.parseFrom(data);
                } catch (InvalidProtocolBufferException ex) {
                    print("unable to parse data");
                    Log.e("Steps", ex.toString());
                    return;
                }
                print("received sample: " + sample.toString());
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
                print("websocket closed");
                webSocketClient = null;
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
                print("websocket error: " + e.getMessage());
            }
        };
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

    private void print(String s) {
        if (logger == null) {
            return;
        }
        logger.send(s);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private Logger logger;
    private WebSocketClient webSocketClient;
    private final IBinder binder = new StepsBinder();

}
