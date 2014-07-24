package com.hiit.steps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.Objects;

// Use StepsService remotely

public class StepsReceiver extends BroadcastReceiver {

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.d(TAG, "StepsReceiver(" + Thread.currentThread().getId() + "): " + msg);
    }

    private static final String THREAD_RECEIVER = "com.hiit.steps.thread.RECEIVER";

    @Override
    public void onReceive(final Context context, Intent intent) {
        log("onReceive, intent=" + intent);
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value == null) {
                    log(key);
                } else {
                    log(key + "[" + value.getClass().getCanonicalName() + "]=" + value);
                }
            }
        }
        Intent stepsIntent = new Intent(context, StepsService.class);
        if (bundle != null) {
            stepsIntent.putExtras(bundle);
        }
        String action = intent.getAction();
        if (action.equals(Configuration.ACTION_START) || action.equals(Intent.ACTION_MAIN)) {
            context.startService(stepsIntent);
        } else if (action.equals(Configuration.ACTION_STOP)) {
            IBinder binder = peekService(context, stepsIntent);
            if (binder == null) {
                log("service not started!");
                return;
            }
            IStepsService service = IStepsService.Stub.asInterface(binder);
            try {
                service.stop();
            } catch (RemoteException e) {
                log("error connecting service");
            }
        } else if (action.equals(Configuration.ACTION_RUN)) {
            // Start service and wait synchronously for it to stop.
            //
            // This may fail after an unknown timeout (~60 seconds on Galaxy S3).
            // The service can't be stopped with another broadcast because
            // onReceive() blocks!

            final Monitor monitor = new Monitor();
            final ILifecycleCallback cb = new ILifecycleCallback.Stub() {
                @Override
                public void stopped(int samples, String outputFile) {
                    setResult(samples, outputFile, null);
                    monitor.release();
                }
            };

            stepsIntent.putExtra(Configuration.EXTRA_LIFECYCLE_CALLBACK, new LifecycleCallback(cb));
            context.startService(stepsIntent);
            monitor.block();
        }
    }

}
