package com.medium.reactnative.estimote;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.estimote.coresdk.common.config.EstimoteSDK;
import com.estimote.coresdk.recognition.packets.EstimoteLocation;
import com.estimote.coresdk.service.BeaconManager;

import com.estimote.proximity_sdk.proximity.EstimoteCloudCredentials;
import com.estimote.proximity_sdk.proximity.ProximityAttachment;
import com.estimote.proximity_sdk.proximity.ProximityObserver;
import com.estimote.proximity_sdk.proximity.ProximityObserverBuilder;
import com.estimote.proximity_sdk.proximity.ProximityZone;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class RNEstimoteModule extends ReactContextBaseJavaModule {
    private static final String EMITTED_ONENTER_EVENT_NAME = "RNEstimoteEventOnEnter"; //If you change this, remember to also change it in index.js
    private static final String EMITTED_ONLEAVE_EVENT_NAME = "RNEstimoteEventOnLeave"; //If you change this, remember to also change it in index.js

    private final ReactApplicationContext reactContext;
    private ProximityObserver.Handler observationHandler;
    private boolean isUseLegacySDK;
    private BeaconManager beaconManager;
    private double detectionDistance = 10.0;

    public RNEstimoteModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.isUseLegacySDK = (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
    }

    @ReactMethod
    public void isUseLegacySDK(Promise promise) {
        promise.resolve(this.isUseLegacySDK);
    }

    @ReactMethod
    public void start(String appId, String appToken, ReadableArray detectDistances) {
        if (this.isUseLegacySDK) {
            this._startByBeaconManager(appId, appToken);
        } else {
           this._startByProximity(appId, appToken, detectDistances);
        }
    }

    @ReactMethod
    public void stop() {
        if (this.isUseLegacySDK) {
            this._stopByBeaconManager();
        } else {
            this._stopByProximity();
        }
    }

    private void _startByProximity(String appId, String appToken, ReadableArray detectDistances) {
        LinkedList<String> distances = new LinkedList<>();
        ArrayList _distances = detectDistances.toArrayList();
        distances.addAll(_distances);

        Context context = reactContext.getApplicationContext();
        EstimoteCloudCredentials estimoteCloudCredentials = new EstimoteCloudCredentials(appId, appToken);
        ProximityObserver proximityObserver =
                new ProximityObserverBuilder(context, estimoteCloudCredentials)
                        .withOnErrorAction(new Function1<Throwable, Unit>() {
                            @Override
                            public Unit invoke(Throwable throwable) {
                                Log.d("app", "proximity observer error: " + throwable);
                                return null;
                            }
                        })
                        .withBalancedPowerMode()
                        .withAnalyticsReportingDisabled()
                        .build();

        //add build option withAnalyticsReportingDisabled to avoid app crash if not network
        //ref : https://github.com/Estimote/Android-Proximity-SDK/issues/45

        List<ProximityZone> proximityZones = new ArrayList();
        for (int i = 0; i < distances.size(); i++) {
            String stringRange = distances.get(i);
            try{
                Double.parseDouble(stringRange);
            } catch (Exception ex) {
                stringRange = "10";
            }

            ProximityZone proximityZone =
                    proximityObserver.zoneBuilder()
                            .forAttachmentKeyAndValue("range", stringRange)
                            .inCustomRange(Double.parseDouble(stringRange))
                            .withOnEnterAction(new Function1<ProximityAttachment, Unit>() {
                                @Override
                                public Unit invoke(ProximityAttachment proximityAttachment) {
                                    WritableMap map = convertToWritableMap(proximityAttachment.getPayload());
                                    RCTNativeAppEventEmitter eventEmitter = reactContext.getJSModule(RCTNativeAppEventEmitter.class);
                                    eventEmitter.emit(EMITTED_ONENTER_EVENT_NAME, map);
                                    return null;
                                }
                            })
                            .withOnExitAction(new Function1<ProximityAttachment, Unit>() {
                                @Override
                                public Unit invoke(ProximityAttachment proximityAttachment) {
                                    WritableMap map = convertToWritableMap(proximityAttachment.getPayload());
                                    RCTNativeAppEventEmitter eventEmitter = reactContext.getJSModule(RCTNativeAppEventEmitter.class);
                                    eventEmitter.emit(EMITTED_ONLEAVE_EVENT_NAME, map);
                                    return null;
                                }
                            })
                            .create();

            proximityZones.add(proximityZone);
        }
        if (this.observationHandler == null) {
            observationHandler = proximityObserver.addProximityZones(proximityZones).start();
        }
    }

    private void _stopByProximity() {
        if (this.observationHandler != null) {
            observationHandler.stop();
            this.observationHandler = null;
        }
    }

    private void _startByBeaconManager(String appId, String appToken) {
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
                    Boolean isWithinDetectionDistance = this.isWithinDetectionDistance(beacon.rssi, beacon.txPower, beaconId);
                    if (isWithinDetectionDistance) {
                        WritableMap map = Arguments.createMap();
                        map.putString("beaconCode", beacon.id.toHexString());
                        eventEmitter.emit(EMITTED_ONENTER_EVENT_NAME, map);
                    }
                }
            }
        }.setDetectionDistance(this.detectionDistance));
    }

    public void _stopByBeaconManager() {
        beaconManager.stopLocationDiscovery();
    }

    private WritableMap convertToWritableMap(Map<String, String> payload) {
        WritableMap map = Arguments.createMap();
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            map.putString(entry.getKey(), entry.getValue());
        }
        return map;
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