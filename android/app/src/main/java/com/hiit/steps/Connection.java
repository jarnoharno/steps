package com.hiit.steps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Future;

public class Connection {

    private static String NAME = "android";

    public interface ConnectionClient {
        void print(String s);
        Context getContext();
    }

    public Connection(ConnectionClient connectionClient) {
        this.connectionClient = connectionClient;
    }

    public void connect() {
        if (!enabled) {
            enabled = true;
            tryConnect();
        }
    }

    public void disconnect() {
        enabled = false;
        if (webSocketFuture != null) {
            webSocketFuture.cancel(true);
            webSocketFuture = null;
        }
        if (webSocket != null) {
            webSocket.close();
            webSocket = null;
        }
        handler.removeCallbacks(retryConnectRunnable);
        enableConnectivityReceiver(false);
    }

    public void send(byte[] data) {
        if (!tracing) {
            return;
        }
        if (webSocket == null) {
            if (buffer.size() >= MAX_QUEUE_SIZE) {
                buffer.pop();
            }
            buffer.push(data);
        } else {
            webSocket.send(data);
        }
    }

    public void startTrace() {
        tracing = true;
        send(startMessage().toByteArray());
    }

    public void stopTrace() {
        if (traceId != null) {
            send(stopMessage().toByteArray());
        }
        tracing = false;
        traceId = null;
    }

    // private

    private StepsProtos.Sample startMessage() {
        return StepsProtos.Sample.newBuilder()
                .setType(StepsProtos.Sample.Type.CONTROL)
                .setName("ctrl")
                .setTimestamp(System.currentTimeMillis() * 1000000)
                .setControl(StepsProtos.Control.newBuilder()
                                .setType(StepsProtos.Control.Type.START)
                                .setStart(StepsProtos.Start.newBuilder()
                                                .setName("android")
                                )
                )
                .build();
    }

    private StepsProtos.Sample stopMessage() {
        return StepsProtos.Sample.newBuilder()
                .setType(StepsProtos.Sample.Type.CONTROL)
                .setName("ctrl")
                .setTimestamp(System.currentTimeMillis() * 1000000)
                .setControl(StepsProtos.Control.newBuilder()
                                .setType(StepsProtos.Control.Type.STOP)
                                .setStop(StepsProtos.Stop.newBuilder()
                                                .setId(traceId)
                                )
                )
                .build();
    }

    private StepsProtos.Sample resumeMessage() {
        return StepsProtos.Sample.newBuilder()
                .setType(StepsProtos.Sample.Type.CONTROL)
                .setName("ctrl")
                .setTimestamp(System.currentTimeMillis() * 1000000)
                .setControl(StepsProtos.Control.newBuilder()
                                .setType(StepsProtos.Control.Type.RESUME)
                                .setResume(StepsProtos.Resume.newBuilder()
                                                .setId(traceId)
                                )
                )
                .build();
    }

    private static final int RETRY_DELAY_MILLIS = 5000;

    private ConnectionClient connectionClient;

    private void print(String s) {
        connectionClient.print(s);
    }

    private Handler handler = new Handler();

    private void retryConnect() {
        retryConnect(0);
    }

    private void retryConnect(int delayMillis) {
        if (enabled) {
            handler.postDelayed(retryConnectRunnable, delayMillis);
        }
    }

    private Runnable retryConnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (enabled) {
                tryConnect();
            }
        }
    };

    private void tryConnect() {
        if (!hasConnectivity()) {
            // set connectivity receiver to try connect when network becomes
            // available
            enableConnectivityReceiver(true);
        } else {
            connectNow();
        }
    }

    private boolean hasConnectivity() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                connectionClient.getContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private Future<WebSocket> webSocketFuture;
    private WebSocket webSocket;
    private boolean enabled;

    private boolean tracing;
    private String traceId;

    private void connectNow() {
        enableConnectivityReceiver(false);
        webSocketFuture = AsyncHttpClient.getDefaultInstance()
                .websocket("wss://whoop.pw/steps/ws", null,
                        webSocketConnectCallback);
    }

    private WebSocketConnectCallback webSocketConnectCallback =
            new WebSocketConnectCallback() {
        @Override
        public void onCompleted(Exception ex, WebSocket webSocket) {
            webSocketFuture = null;
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
            print("Websocket opened");
            if (!enabled) {
                webSocket.close();
                return;
            }

            Connection.this.webSocket = webSocket;

            webSocket.setClosedCallback(new CompletedCallback() {
                @Override
                public void onCompleted(Exception ex) {
                    print("Websocket closed");
                    retryConnect();
                    Connection.this.webSocket = null;
                    if (!tracing) {
                        return;
                    }
                }
            });
            webSocket.setStringCallback(new WebSocket.StringCallback() {
                public void onStringAvailable(String s) {
                    System.out.println("I got a string: " + s);
                }
            });
            webSocket.setDataCallback(new DataCallback() {
                public void onDataAvailable(DataEmitter emitter,
                                            ByteBufferList byteBufferList) {
                    byte[] data = byteBufferList.getAllByteArray();
                    try {
                        StepsProtos.Sample sample = StepsProtos.Sample
                                .parseFrom(data);
                        traceId = sample.getControl().getStartAck().getId();
                        print("received id: " + traceId);
                    } catch (InvalidProtocolBufferException e) {
                        print(e.toString());
                    } finally {
                        byteBufferList.recycle();
                    }
                }
            });

            // send a resume message if tracing
            if (traceId != null) {
                send(resumeMessage().toByteArray());
            }
            // send full buffer
            sendAll();
        }
    };

    // 1e5 samples ~ 5.5 min ~ 2MB (300 samples/s, 20B/sample)
    private static int MAX_QUEUE_SIZE = 100000;
    private Deque<byte[]> buffer = new ArrayDeque<byte[]>();

    private void sendAll() {
        for (byte[] data : buffer) {
            webSocket.send(data);
        }
        buffer.clear();
    }

    private void enableConnectivityReceiver(boolean enabled) {
        if (enabled && !connectivityReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            connectionClient.getContext()
                    .registerReceiver(connectivityReceiver, intentFilter);
            connectivityReceiverRegistered = true;
            print("Registered connectivity receiver");
        } else if (!enabled && connectivityReceiverRegistered) {
            connectionClient.getContext()
                    .unregisterReceiver(connectivityReceiver);
            connectivityReceiverRegistered = false;
            print("Unregistered connectivity receiver");
        }
    }

    private boolean connectivityReceiverRegistered = false;

    private BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tryConnect();
        }
    };
}
