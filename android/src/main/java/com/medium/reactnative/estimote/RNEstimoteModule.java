
package com.medium.reactnative.estimote;

import android.app.Activity;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class RNEstimoteModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;

    public RNEstimoteModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @ReactMethod
    public void start(String appId, String appToken) {

    }

    @ReactMethod
    public void stop() {

    }

    @Override
    public String getName() {
        return "RNEstimote";
    }
}