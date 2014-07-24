package com.hiit.steps;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

public class LifecycleCallback implements Parcelable {

    private ILifecycleCallback target;

    public LifecycleCallback(ILifecycleCallback target) {
        this.target = target;
    }

    public LifecycleCallback(IBinder target) {
        this.target = ILifecycleCallback.Stub.asInterface(target);
    }

    public static final Parcelable.Creator<LifecycleCallback> CREATOR
            = new Parcelable.Creator<LifecycleCallback>() {
        public LifecycleCallback createFromParcel(Parcel in) {
            IBinder target = in.readStrongBinder();
            return target != null ? new LifecycleCallback(target) : null;
        }

        public LifecycleCallback[] newArray(int size) {
            return new LifecycleCallback[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeStrongBinder(target.asBinder());
    }

    void stopped(int samples, String outputFile) throws RemoteException {
        target.stopped(samples, outputFile);
    }

}
