package com.hiit.steps;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class AbstractServiceConnection implements ServiceConnection {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
