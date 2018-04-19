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

    @ReactMethod
    public void start(String appId, String appToken, ReadableArray detectDistances) {
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
                        .build();

        List<ProximityZone> proximityZones = new ArrayList();
        for (int i = 0; i < distances.size(); i++) {
            double doubleRange;
            String stringRange = distances.get(i);
            try{
                doubleRange = Double.parseDouble(distances.get(i));
            } catch (Exception ex) {
                doubleRange = 10.0;
                stringRange = "10";
            }

            ProximityZone proximityZone =
                    proximityObserver.zoneBuilder()
                            .forAttachmentKeyAndValue("range", stringRange)
                            .inCustomRange(doubleRange)
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