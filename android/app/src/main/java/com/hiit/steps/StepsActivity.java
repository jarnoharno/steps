package com.hiit.steps;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class StepsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steps);
    }

    @Override
    protected void onStart() {
        super.onStart();
        print("Binding service");
        StepsService.bind(this, serviceConnection);
    }

    @Override
    protected void onStop() {
        // Unbind from the service
        if (stepsService != null) {
            print("Unbinding service");
            stepsService.removeClient(client);
            unbindService(serviceConnection);
            stepsService = null;
        }
        super.onStop();
    }

    public void onServiceButtonClicked(View view) {
        ToggleButton serviceButton = (ToggleButton) view;
        serviceButton.setEnabled(false);
        if (serviceButton.isChecked()) {
            serviceButton.setChecked(true);
            stepsService.start(this);
        } else {
            serviceButton.setChecked(true);
            stepsService.stop(this);
        }
    }

    private void print(String s) {
        TextView textView = (TextView) findViewById(R.id.text);
        textView.append(s + '\n');
    }

    private StepsService stepsService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepsService.StepsBinder stepsBinder =
                (StepsService.StepsBinder) service;
            stepsService = stepsBinder.getService();
            ToggleButton serviceButton = (ToggleButton) findViewById(R.id.serviceButton);
            serviceButton.setEnabled(true);
            stepsService.addClient(client);
            print("Service bound");
            for (String s: stepsService.getOutputBuffer()) {
                print(s);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            stepsService = null;
            ToggleButton serviceButton = (ToggleButton) findViewById(R.id.serviceButton);
            serviceButton.setChecked(false);
            serviceButton.setEnabled(false);
            print("Service unbound");
        }
    };

    private StepsService.Client client = new StepsService.Client() {
        @Override
        public void print(final String msg) {
            StepsActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    StepsActivity.this.print(msg);
                }
            });
        }

        @Override
        public void serviceStateChanged(StepsService.State state) {
            ToggleButton serviceButton = (ToggleButton) findViewById(R.id.serviceButton);
            ToggleButton traceButton = (ToggleButton) findViewById(R.id.traceButton);
            switch (state) {
                case STARTED:
                    serviceButton.setChecked(true);
                    serviceButton.setEnabled(true);
                    break;
                case STOPPED:
                    serviceButton.setChecked(false);
                    serviceButton.setEnabled(true);
                    break;
                default:
                    serviceButton.setEnabled(false);
                    break;
            }
        }
    };
}
