// IStepsService.aidl
package com.hiit.steps;

import com.hiit.steps.IStepsCallback;

interface IStepsService {

    void addStepsCallback(IStepsCallback cb);
    void removeStepsCallback(IStepsCallback cb);

    void stop();
    int getSamples();
    int getSteps();
    boolean isRunning();

}
