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
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class StepsActivity extends Activity {

    private static final String SAMPLES_KEY_NAME = "samples";
    private static final String STEPS_KEY_NAME = "steps";

    private IStepsService service = null;

    private int samples = 0;
    private int steps = 0;

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = IStepsService.Stub.asInterface(binder);
            boolean running;
            try {
                service.addStepsCallback(callback);
                running = service.isRunning();
            } catch (RemoteException e) {
                // connection is dead
                service = null;
                return;
            }
            ToggleButton btn = (ToggleButton)findViewById(R.id.service_button);
            btn.setEnabled(true);
            btn.setChecked(running);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            ToggleButton btn = (ToggleButton)findViewById(R.id.service_button);
            btn.setEnabled(false);
        }
    };

    void updateSamples(int samples) {
        this.samples = samples;
        TextView view = (TextView)findViewById(R.id.samples);
        view.setText(Integer.toString(samples));
    }

    void updateSteps(int steps) {
        this.steps = steps;
        TextView view = (TextView)findViewById(R.id.steps);
        view.setText(Integer.toString(steps));
    }

    private static final int MSG_SAMPLE_EVENT = 1;
    private static final int MSG_STEP_EVENT = 2;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SAMPLE_EVENT:
                    if (msg.arg1 > 0) {
                        updateSamples(msg.arg1);
                    }
                    break;
                case MSG_STEP_EVENT:
                    if (msg.arg1 > 0) {
                        updateSteps(msg.arg1);
                    }
                    break;
            }
        }
    };

    private IStepsCallback callback = new IStepsCallback.Stub() {

        @Override
        public void onSampleEvent(int samples) {
            Message msg = handler.obtainMessage(MSG_SAMPLE_EVENT, samples, 0, null);
            handler.sendMessage(msg);
        }

        @Override
        public void onStepEvent(int steps) {
            Message msg = handler.obtainMessage(MSG_STEP_EVENT, steps, 0, null);
            handler.sendMessage(msg);
        }
    };

    public void onServiceButtonClicked(View view) {
        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
            Intent intent = new Intent(this, StepsService.class);
            startService(intent);
        } else {
            try {
                service.stop();
            } catch (RemoteException e) {
                // service is dead
                service = null;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifecycle_test_activity);
        if (savedInstanceState != null) {
            int samples = savedInstanceState.getInt(SAMPLES_KEY_NAME, 0);
            int steps = savedInstanceState.getInt(STEPS_KEY_NAME, 0);
            updateSamples(samples);
            updateSteps(steps);
        }
        ToggleButton btn = (ToggleButton)findViewById(R.id.service_button);
        btn.setEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        StepsService.bind(this, connection);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAMPLES_KEY_NAME, samples);
        outState.putInt(STEPS_KEY_NAME, steps);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from service
        if (service == null)
            return;
        try {
            service.removeStepsCallback(callback);
        } catch (RemoteException e) {
            service = null;
            return;
        }
        unbindService(connection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
