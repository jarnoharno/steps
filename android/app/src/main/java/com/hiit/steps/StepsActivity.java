package com.hiit.steps;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class StepsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steps);
    }

    @Override
    protected void onStart() {
        super.onStart();
        StepsService.bind(this, serviceConnection);
    }

    @Override
    protected void onStop() {
        // Unbind from the service
        if (stepsService != null) {
            stepsService.removeLogger(logger);
            unbindService(serviceConnection);
            stepsService = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.steps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private StepsService stepsService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepsService.StepsBinder stepsBinder = (StepsService.StepsBinder) service;
            stepsService = stepsBinder.getService();
            stepsService.addLogger(logger);
            if (!stepsService.started) {
                stepsService.start(StepsActivity.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            stepsService = null;
        }
    };

    private StepsService.Logger logger = new StepsService.Logger() {
        @Override
        public void send(final String msg) {
            StepsActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) findViewById(R.id.text);
                    textView.append(msg);
                    textView.append("\n");
                }
            });
        }
    };
}
