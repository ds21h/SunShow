package jb.sunmoon.show;

import android.app.Application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SunMoonApp extends Application {
    private static SunMoonApp mSunMoonApp;
    ExecutorService xExecutor;

    static SunMoonApp getInstance(){
        return mSunMoonApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSunMoonApp = this;
        xExecutor = Executors.newSingleThreadExecutor();
    }
}
