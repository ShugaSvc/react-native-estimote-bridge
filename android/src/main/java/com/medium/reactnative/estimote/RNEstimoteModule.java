package com.medium.reactnative.estimote;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;

public class RNEstimoteModule extends ReactContextBaseJavaModule {
    private EstimoteBeaconDetector estimoteBeaconDetector;

    public RNEstimoteModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.estimoteBeaconDetector = new EstimoteBeaconDetector(reactContext);
    }

    @ReactMethod
    public void isUseLegacySDK(Promise promise) {
        promise.resolve(this.estimoteBeaconDetector.isUseLegacySDK());
    }

    @ReactMethod
    public void init(String appId, String appToken, ReadableArray detectDistances) {
        this.estimoteBeaconDetector.init(appId, appToken, MapUtil.toArray(detectDistances));
    }

    @ReactMethod
    public void start() {
        this.estimoteBeaconDetector.start();
    }

    @ReactMethod
    public void stop() {
        this.estimoteBeaconDetector.stop();
    }

    @ReactMethod
    public void setBeaconDetectionDistance(double detectionDistance) {
        this.estimoteBeaconDetector.setBeaconDetectionDistance(detectionDistance);
    }

    @Override
    public String getName() {
        return "RNEstimote";
    }
}