// IStepsCallback.aidl
package com.hiit.steps;

oneway interface IStepsCallback {
    void onStepEvent(int steps);
    void onSampleEvent(int samples);
}
