// ILifecycleCallback.aidl
package com.hiit.steps;

oneway interface ILifecycleCallback {
    void stopped(int samples, String outputFile);
}
