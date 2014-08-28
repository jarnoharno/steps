package com.hiit.mock;

import android.app.Activity;
import android.os.Bundle;

public class MockActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mock_activity);
    }
}
