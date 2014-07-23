package com.hiit.steps;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class StepsReceiver extends BroadcastReceiver {

    public static final String ACTION_START = "com.hiit.steps.action.START";
    public static final String ACTION_STOP = "com.hiit.steps.action.STOP";
    public static final String ACTION_RUN = "com.hiit.steps.action.RUN";

    private static final String TAG = "Steps";

    private void log(String msg) {
        Log.d(TAG, "StepsReceiver(" + Thread.currentThread().getId() + "): " + msg);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        log("onReceive, intent=" + intent);
        String action = intent.getAction();
        final Intent stepsIntent = new Intent(context, StepsService.class);
        if (action.equals(ACTION_START) || action.equals(Intent.ACTION_MAIN)) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    context.startService(stepsIntent);
                }
            });
            thread.start();
        } else if (action.equals(ACTION_STOP)) {
            IBinder binder = peekService(context, stepsIntent);
            if (binder == null) {
                log("service not started!");
                return;
            }
            StepsService service = ((StepsService.LocalBinder) binder).getService();
            service.stop();
        } else if (action.equals(ACTION_RUN)) {
            context.startService(stepsIntent);
            IBinder binder = peekService(context, stepsIntent);
            if (binder == null) {
                log("service not started!");
                return;
            }
            StepsService service = ((StepsService.LocalBinder) binder).getService();
            service.stop();
        }
    }

}
