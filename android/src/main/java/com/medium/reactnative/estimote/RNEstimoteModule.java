
package com.medium.reactnative.estimote;

import android.content.Context;

import com.estimote.coresdk.common.config.EstimoteSDK;
import com.estimote.coresdk.recognition.packets.EstimoteLocation;
import com.estimote.coresdk.service.BeaconManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RNEstimoteModule extends ReactContextBaseJavaModule {
    private static final String EMITTED_EVENT_NAME = "RNEstimoteEvent"; //If you change this, remember to also change it in index.js

    private final ReactApplicationContext reactContext;
    private BeaconManager beaconManager;
    private LinkedList<String> allowedBeaconDevices = new LinkedList<>();
    private double detectionDistance = 6.0;

    public RNEstimoteModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @ReactMethod
    public void start(String appId, String appToken) {
        Context context = reactContext.getApplicationContext();

        EstimoteSDK.initialize(context, appId, appToken);
        beaconManager = new BeaconManager(context);

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startLocationDiscovery();
            }
        });

        //- location listener
        beaconManager.setLocationListener(new BeaconManager.LocationListener() {
            private double detectionDistance;

            public BeaconManager.LocationListener setDetectionDistance(double detectionDistance) {
                this.detectionDistance = detectionDistance;
                return this;
            }

            private Boolean isWithinDetectionDistance(double rssi, double txPower, String id) {
                return Math.pow(10, (txPower - rssi) / 20) <= this.detectionDistance;
            }

            @Override
            public void onLocationsFound(List<EstimoteLocation> beacons) {
                RCTNativeAppEventEmitter eventEmitter = reactContext.getJSModule(RCTNativeAppEventEmitter.class);
                for (EstimoteLocation beacon : beacons) {
                    String beaconId = beacon.id.toHexString();
                    if (allowedBeaconDevices.indexOf(beaconId) > -1) {
                        Boolean isWithinDetectionDistance = this.isWithinDetectionDistance(beacon.rssi, beacon.txPower, beaconId);
                        if (isWithinDetectionDistance) {
                            WritableMap map = Arguments.createMap();
                            map.putString("beaconCode", beacon.id.toHexString());
                            eventEmitter.emit(EMITTED_EVENT_NAME, map);
                        }
                    }
                }
            }
        }.setDetectionDistance(this.detectionDistance));
    }

    @ReactMethod
    public void stop() {
        beaconManager.stopLocationDiscovery();
    }

    @ReactMethod
    public void setBeaconDevices(ReadableArray beacons) {
        ArrayList allowedBeacons = beacons.toArrayList();
        allowedBeaconDevices = new LinkedList<>();
        allowedBeaconDevices.addAll(allowedBeacons);
    }

    @ReactMethod
    public void setBeaconDetectionDistance(double detectionDistance) {
        this.detectionDistance = detectionDistance;
    }

    @Override
    public String getName() {
        return "RNEstimote";
    }
}