package com.hiit.steps;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class StepsActivity extends Activity {

    private static final String TAG = "Steps";
    private static final String STEPS = "steps";

    private StepsService service = null;
    private int steps = 0;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            log("onServiceConnected");
            StepsService.LocalBinder binder = (StepsService.LocalBinder) service;
            StepsActivity.this.service = binder.getService();
            StepsActivity.this.service.setListener(listener);
            ToggleButton btn = (ToggleButton)findViewById(R.id.service_button);
            btn.setEnabled(true);
            btn.setChecked(StepsActivity.this.service.isRunning());
            StepsActivity.this.updateSteps(StepsActivity.this.service.getSteps());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            log("onServiceDisconnected");
            service = null;
            ToggleButton btn = (ToggleButton)findViewById(R.id.service_button);
            btn.setEnabled(false);
        }
    };

    void updateSteps(int steps) {
        this.steps = steps;
        TextView view = (TextView)findViewById(R.id.steps);
        view.setText(Integer.toString(this.steps));
    }

    private static final int STEP = 1;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STEP:
                    if (service == null)
                        return;
                    updateSteps(service.getSteps());
                default:
                    super.handleMessage(msg);
            }
        }
    };

    // thread-safe
    private StepsListener listener = new StepsListener() {
        @Override
        public void onStep() {
            Message msg = handler.obtainMessage(STEP);
            handler.sendMessage(msg);
        }
    };

    private void log(String msg) {
        Log.d(TAG, "Activity(" + System.identityHashCode(this) + "/" + Thread.currentThread().getId() + "): " + msg);
    }

    public StepsActivity() {
        super();
        log("constructor");
    }

    public void onServiceButtonClicked(View view) {
        log("onServiceButtonClicked");
        Intent intent = new Intent(this, StepsService.class);
        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
            startService(intent);
            service.start();
        } else {
            service.stop();
            stopService(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifecycle_test_activity);
        if (savedInstanceState != null) {
            log(savedInstanceState.toString());
            int steps = savedInstanceState.getInt(STEPS, -1);
            log("steps in bundle: " + steps);
            updateSteps(steps);
        }
        ToggleButton btn = (ToggleButton)findViewById(R.id.service_button);
        btn.setEnabled(false);
    }

    @Override
    protected void onStart() {
        log("onStart");
        super.onStart();
        Intent intent = new Intent(this, StepsService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    protected void onResume() {
        log("onResume");
        super.onResume();
    }
    @Override
    protected void onPause() {
        log("onPause");
        super.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        log("onSaveInstanceState");
        outState.putInt(STEPS, steps);
        super.onSaveInstanceState(outState);
    }
    @Override
    protected void onStop() {
        log("onStop");
        super.onStop();
        // Unbind from the service
        if (service == null)
            return;
        service.setListener(null);
        service = null;
        unbindService(mConnection);
        ToggleButton btn = (ToggleButton)findViewById(R.id.service_button);
        btn.setEnabled(false);
    }
    @Override
    protected void onDestroy() {
        log("onDestroy");
        super.onDestroy();
    }
    
    public void logDirs() {
        log("Environment.getDataDirectory(): " + Environment.getDataDirectory().toString());
        log("Environment.getDownloadCacheDirectory(): " + Environment.getDownloadCacheDirectory().toString());
        log("Environment.getExternalStorageDirectory(): " + Environment.getExternalStorageDirectory().toString());
        log("Environment.getExternalStorageDirectory(Environment.DIRECTORY_DCIM): " +
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());
        log("Context.getCacheDir(): " + getCacheDir().toString());
        log("Context.getExternalCacheDir(): " + getExternalCacheDir().toString());
        log("Context.getExternalFilesDir(null): " + getExternalFilesDir(null).toString());
        log("Context.getExternalFilesDir(Environment.DIRECTORY_DCIM): " +
                getExternalFilesDir(Environment.DIRECTORY_DCIM).toString());
        log("Context.getFilesDir(): " + getFilesDir().toString());

    }
}
