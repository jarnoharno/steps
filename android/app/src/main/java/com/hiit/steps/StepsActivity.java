package com.hiit.steps;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StepsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steps);
    }

    @Override
    protected void onStart() {
        super.onStart();
        printActivity("Binding service");
        StepsService.bind(this, serviceConnection);
    }

    @Override
    protected void onStop() {
        // Unbind from the service
        if (stepsService != null) {
            printActivity("Unbinding service");
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
            stepsService.startService();
        } else {
            serviceButton.setChecked(true);
            stepsService.stopService();
        }
    }

    public void onTraceButtonClicked(View view) {
        ToggleButton traceButton = (ToggleButton) view;
        if (traceButton.isChecked()) {
            stepsService.startTrace();
        } else {
            stepsService.stopTrace();
        }
    }

    private void print(String s) {
        TextView textView = (TextView) findViewById(R.id.text);
        textView.append(s + '\n');
    }

    private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private void printActivity(String s) {
        Log.i("Steps", s);
        String line = "[" + dateFormat.format(new Date()) + "] Activity: " + s;
        print(line);
    }

    private StepsService stepsService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepsService.StepsBinder stepsBinder =
                (StepsService.StepsBinder) service;
            stepsService = stepsBinder.getService();
            ToggleButton serviceButton = (ToggleButton) findViewById(R.id.serviceButton);
            ToggleButton traceButton = (ToggleButton) findViewById(R.id.traceButton);
            serviceButton.setEnabled(true);
            stepsService.addClient(client);
            printActivity("Service bound");
            for (String s: stepsService.getOutputBuffer()) {
                print(s);
            }
            if (stepsService.getServiceState().hasStarted()) {
                serviceButton.setChecked(true);
                traceButton.setEnabled(true);
                traceButton.setChecked(stepsService.getTraceState().hasStarted());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            stepsService = null;
            ToggleButton serviceButton = (ToggleButton) findViewById(R.id.serviceButton);
            serviceButton.setChecked(false);
            serviceButton.setEnabled(false);
            printActivity("Service unbound");
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
                    traceButton.setChecked(false);
                    traceButton.setEnabled(true);
                    break;
                case STOPPED:
                    serviceButton.setChecked(false);
                    serviceButton.setEnabled(true);
                    traceButton.setChecked(false);
                    traceButton.setEnabled(false);
                    break;
                default:
                    serviceButton.setEnabled(false);
                    traceButton.setChecked(false);
                    traceButton.setEnabled(false);
                    break;
            }
        }

        @Override
        public void traceStateChanged(StepsService.State state) {
            ToggleButton traceButton = (ToggleButton) findViewById(R.id.traceButton);
            if (state.hasStarted()) {
                traceButton.setChecked(true);
            } else {
                traceButton.setChecked(false);
            }
        }
    };
}
