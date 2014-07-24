// IStepsService.aidl
package com.hiit.steps;

import com.hiit.steps.IStepsCallback;

interface IStepsService {

    boolean addStepsCallback(IStepsCallback callback);
    boolean removeStepsCallback(IStepsCallback callback);

    void stop();
    int getSamples();
    int getSteps();
    boolean isRunning();

}
