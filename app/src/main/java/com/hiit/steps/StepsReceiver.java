package com.hiit.steps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

// Use StepsService remotely

public class StepsReceiver extends BroadcastReceiver {

    public static final String ACTION_START = "com.hiit.steps.action.START";
    public static final String ACTION_STOP = "com.hiit.steps.action.STOP";
    public static final String ACTION_RUN = "com.hiit.steps.action.RUN";

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.d(TAG, "StepsReceiver(" + Thread.currentThread().getId() + "): " + msg);
    }

    private static final String THREAD_RECEIVER = "com.hiit.steps.thread.RECEIVER";

    @Override
    public void onReceive(final Context context, Intent intent) {
        log("onReceive, intent=" + intent);
        String action = intent.getAction();
        Intent stepsIntent = new Intent(context, StepsService.class);
        if (action.equals(ACTION_START) || action.equals(Intent.ACTION_MAIN)) {
            context.startService(stepsIntent);
        } else if (action.equals(ACTION_STOP)) {
            IBinder binder = peekService(context, stepsIntent);
            if (binder == null) {
                log("service not started!");
                return;
            }
            StepsService.Remote service = new StepsService.Remote(binder);
            service.stop();
        } else if (action.equals(ACTION_RUN)) {
            // Start service and wait synchronously for it to stop.
            //
            // This may fail after an unknown timeout (~60 seconds on Galaxy S3).
            // The service can't be stopped with another broadcast because
            // onReceive() blocks!
            final HandlerThread handlerThread = new HandlerThread(THREAD_RECEIVER);
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    log("MSG_CALLBACK_STOPPED");
                    switch (msg.what) {
                        case StepsService.MSG_CALLBACK_STOPPED:
                            handlerThread.quit();
                            break;
                    }
                }
            };
            stepsIntent.putExtra(StepsService.EXTRA_CALLBACK, new Messenger(handler));
            context.startService(stepsIntent);
            // wait for service to stop
            try {
                handlerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
