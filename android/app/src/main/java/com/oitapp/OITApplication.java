package com.oitapp;

import android.app.Application;

public class OITApplication extends Application {
    private static OITApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static OITApplication getInstance() {
        return instance;
    }
}
