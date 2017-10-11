package com.tudan.smartlamp;

import android.app.Application;

import com.akaita.java.rxjava2debug.RxJava2Debug;

/**
 * Created by mario on 10.10.2017..
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable RxJava assembly stack collection, to make RxJava crash reports clear and unique
        RxJava2Debug.enableRxJava2AssemblyTracking(new String[]{"com.tudan.smartlamp", "com.tudan.smartlamp"});
    }
}