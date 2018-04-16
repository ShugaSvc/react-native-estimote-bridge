package com.medium.reactnative.estimote;

import android.content.Context;
import android.util.Log;

import com.estimote.proximity_sdk.proximity.EstimoteCloudCredentials;
import com.estimote.proximity_sdk.proximity.ProximityAttachment;
import com.estimote.proximity_sdk.proximity.ProximityObserver;
import com.estimote.proximity_sdk.proximity.ProximityObserverBuilder;

import com.estimote.proximity_sdk.proximity.ProximityZone;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RNEstimoteModule extends ReactContextBaseJavaModule {
    private static final String EMITTED_ONENTER_EVENT_NAME = "RNEstimoteEventOnEnter"; //If you change this, remember to also change it in index.js
    private static final String EMITTED_ONLEAVE_EVENT_NAME = "RNEstimoteEventOnLeave"; //If you change this, remember to also change it in index.js

    private final ReactApplicationContext reactContext;
    private ProximityObserver.Handler observationHandler;


    public RNEstimoteModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    private WritableMap convertToWritableMap(Map<String, String> payload) {
        WritableMap map = Arguments.createMap();
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            map.putString(entry.getKey(), entry.getValue());
        }
        return map;
    }

    private double getRange(JSONObject jsonObject) {
        try {
            return jsonObject.getDouble("range");
        } catch (Exception ex) {
            Log.d("app", "getRange() error:" + ex.getStackTrace());
            return 10.0;
        }
    }

    @ReactMethod
    public void start(String appId, String appToken, ReadableArray beaconZones, String attachmentKey) {
        JSONArray zones = new JSONArray();
        try {
            zones = ReactNativeJson.convertArrayToJson(beaconZones);
        } catch (Exception ex) {
            Log.d("app", "parsing attachments error: " + ex.getStackTrace());
        }

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
                        .build();

        List<ProximityZone> proximityZones = new ArrayList();
        for (int i = 0; i < zones.length(); i++) {
            String attachmentValue;
            double beaconRange;
            try {
                JSONObject zone = zones.getJSONObject(i);
                attachmentValue = zone.getString(attachmentKey);
                beaconRange = this.getRange(zone);
            } catch (Exception ex) {
                Log.d("app", "parse beacon attachment error: " + ex.getStackTrace());
                break;
            }

            ProximityZone proximityZone =
                    proximityObserver.zoneBuilder()
                            .forAttachmentKeyAndValue(attachmentKey, attachmentValue)
                            .inCustomRange(beaconRange)
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

    @ReactMethod
    public void stop() {
        if (this.observationHandler != null) {
            observationHandler.stop();
            this.observationHandler = null;
        }
    }

    @Override
    public String getName() {
        return "RNEstimote";
    }
}