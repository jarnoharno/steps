// IStepsService.aidl
package com.hiit.steps;

import com.hiit.steps.IStepsCallback;
import com.hiit.steps.ILifecycleCallback;

interface IStepsService {

    boolean addStepsCallback(IStepsCallback callback);
    boolean removeStepsCallback(IStepsCallback callback);

    boolean addLifecycleCallback(ILifecycleCallback callback);
    boolean removeLifecycleCallback(ILifecycleCallback callback);

    // false if already stopped
    boolean stop();
    int getSamples();
    int getSteps();
    boolean isRunning();

}
