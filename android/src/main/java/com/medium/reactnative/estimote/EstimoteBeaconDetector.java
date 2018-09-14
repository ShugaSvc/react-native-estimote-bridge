package com.medium.reactnative.estimote;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.estimote.coresdk.common.config.EstimoteSDK;
import com.estimote.coresdk.recognition.packets.EstimoteLocation;
import com.estimote.coresdk.service.BeaconManager;
import com.estimote.proximity_sdk.api.EstimoteCloudCredentials;
import com.estimote.proximity_sdk.api.ProximityZoneContext;
import com.estimote.proximity_sdk.api.ProximityObserver;
import com.estimote.proximity_sdk.api.ProximityObserverBuilder;
import com.estimote.proximity_sdk.api.ProximityZone;
import com.estimote.proximity_sdk.api.ProximityZoneBuilder;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;



public class EstimoteBeaconDetector {

    private static final String TAG_PREFIX = "range_android_";

    private static final String EMITTED_ONENTER_EVENT_NAME = "RNEstimoteEventOnEnter"; //If you change this, remember to also change it in index.js
    private static final String EMITTED_ONLEAVE_EVENT_NAME = "RNEstimoteEventOnLeave"; //If you change this, remember to also change it in index.js
    private static final double BACKGROUND_BEACON_DETECT_RANGE = 20;

    private Context context;
    private boolean isUseLegacySDK;

    public static ProximityObserver.Handler foregroundObservationHandler = null;
    public static ProximityObserver foregroundProximityObserver;

    public static ProximityObserver.Handler backgroundObservationHandler = null;
    public static ProximityObserver backgroundProximityObserver;

    private BeaconManager beaconManager;
    private double detectionDistance = 10.0;
    private static String TAG = "estimoteBeacon";

    private List<ProximityZone> proximityZones;

    public EstimoteBeaconDetector(Context context) {
        this.context = context;
        this.isUseLegacySDK = (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
    }

    public boolean isUseLegacySDK() {
        return this.isUseLegacySDK;
    }

    public void init(String appId, String appToken, ReadableArray detectDistances) {
        if (this.isUseLegacySDK) {
            this.initLegacyBeaconManager(appId, appToken);
        } else {
            this.initProximityObserver(appId, appToken, MapUtil.toArray(detectDistances));
        }
    }

    public void start() {
        if (this.isUseLegacySDK) {
            this.startLegacyBeaconManager();
        } else {
            this.startProximityObserver();
        }
    }

    public void stop() {
        if (this.isUseLegacySDK) {
            this.stopLegacyBeaconManager();
        } else {
            this.stopProximityObserver();
        }
    }

    public void setBeaconDetectionDistance(double detectionDistance) {
        this.detectionDistance = detectionDistance;
    }

    public static void startBackendDetect(String appId, String appToken, String[] detectDistances, final Context context) {
        Log.i(TAG, "into startBackendDetect()");
        if (EstimoteBeaconDetector.backgroundObservationHandler != null) {
            Log.i(TAG, "EstimoteBeaconDetector.backgroundObservationHandler still alive, return.");
            return;
        }


        EstimoteBeaconDetector.backgroundProximityObserver = createProximityObserver(appId, appToken, context);
        List<ProximityZone> proximityZones = createProximityZones(
                detectDistances,
                true,
                new Function1<ProximityZoneContext, Unit>() {
                    @Override
                    public Unit invoke(ProximityZoneContext proximityZoneContext) {
                        Log.i(TAG, "backend detector:: OnEnter event be triggered.");
                        PreferenceHelper preferenceHelper = new PreferenceHelper(context);
                        WritableMap map = MapUtil.contextToMap(proximityZoneContext);
                        String beaconCode = map.getString("uid");
                        String[] beaconCodes = {beaconCode};
                        preferenceHelper.setBeaconData(beaconCodes, BeaconEventTypeEnum.ONENTER.toString());
                        return null;
                    }
                },
                new Function1<ProximityZoneContext, Unit>() {
                    @Override
                    public Unit invoke(ProximityZoneContext proximityZoneContext) {
                        Log.i(TAG, "backend detector:: OnExit event be triggered.");
                        PreferenceHelper preferenceHelper = new PreferenceHelper(context);
                        WritableMap map = MapUtil.contextToMap(proximityZoneContext);
                        String beaconCode = map.getString("uid");
                        String[] beaconCodes = {beaconCode};
                        preferenceHelper.setBeaconData(beaconCodes, BeaconEventTypeEnum.ONLEAVE.toString());
                        return null;
                    }
                },
                new Function1<Set<? extends ProximityZoneContext>, Unit>() {
                    @Override
                    public Unit invoke(Set<? extends ProximityZoneContext> proximityZoneContexts) {
                        Log.i(TAG, "backend detector:: OnChange event be triggered.");
                        PreferenceHelper preferenceHelper = new PreferenceHelper(context);
                        List<String> beaconCodes = new ArrayList<String>();
                        for (ProximityZoneContext proximityZoneContext : proximityZoneContexts) {
                            WritableMap map = MapUtil.contextToMap(proximityZoneContext);
                            String beaconCode = map.getString("uid");
                            beaconCodes.add(beaconCode);
                        }
                        preferenceHelper.setBeaconData(beaconCodes.toArray(new String[beaconCodes.size()]),
                                BeaconEventTypeEnum.ONCHANGE.toString());
                        return null;
                    }
                });
        if(proximityZones != null) {
            EstimoteBeaconDetector.backgroundObservationHandler = EstimoteBeaconDetector.backgroundProximityObserver.startObserving(proximityZones);
            Log.i(TAG, "EstimoteBeaconDetector.backgroundObservationHandler start listener beacons...");
        } else {
            Log.i(TAG, "EstimoteBeaconDetector.backgroundObservationHandler start listener beacons failed, the proximityZones is null");
        }
    }


    //private methods
    private void initProximityObserver(String appId, String appToken, String[] detectDistances) {
        final Context context = this.context;
        EstimoteBeaconDetector.foregroundProximityObserver = createProximityObserver(appId, appToken, context);
        if (EstimoteBeaconDetector.foregroundObservationHandler == null) {
             this.proximityZones = createProximityZones(
                    detectDistances,
                    false,
                    new Function1<ProximityZoneContext, Unit>() {
                        @Override
                        public Unit invoke(ProximityZoneContext proximityAttachment) {
                            // onEnter event, do nothing
                            return null;
                        }
                    },
                    new Function1<ProximityZoneContext, Unit>() {
                        @Override
                        public Unit invoke(ProximityZoneContext context) {
                            WritableMap map = MapUtil.contextToMap(context);
                            RCTNativeAppEventEmitter eventEmitter = ((ReactContext) context).getJSModule(RCTNativeAppEventEmitter.class);
                            eventEmitter.emit(EMITTED_ONLEAVE_EVENT_NAME, map);
                            return null;
                        }
                    },
                    new Function1<Set<? extends ProximityZoneContext>, Unit>() {
                        @Override
                        public Unit invoke(Set<? extends ProximityZoneContext> contexts) {
                            WritableArray writableArray = Arguments.createArray();
                            for (ProximityZoneContext context : contexts) {
                                WritableMap map = MapUtil.contextToMap(context);
                                writableArray.pushMap(map);
                            }
                            RCTNativeAppEventEmitter eventEmitter = ((ReactContext) context).getJSModule(RCTNativeAppEventEmitter.class);
                            eventEmitter.emit(EMITTED_ONENTER_EVENT_NAME, writableArray);
                            return null;
                        }
                    });
            EstimoteBeaconDetector.foregroundObservationHandler = EstimoteBeaconDetector.foregroundProximityObserver.startObserving(proximityZones);
        }
    }



    private void startProximityObserver() {
        if (EstimoteBeaconDetector.foregroundObservationHandler == null && this.proximityZones != null) {
            EstimoteBeaconDetector.foregroundObservationHandler = EstimoteBeaconDetector.foregroundProximityObserver.startObserving(this.proximityZones);
        }
    }

    private void stopProximityObserver() {
        if (EstimoteBeaconDetector.foregroundObservationHandler != null) {
            EstimoteBeaconDetector.foregroundObservationHandler.stop();
            EstimoteBeaconDetector.foregroundObservationHandler = null;
        }
    }

    private static ProximityObserver createProximityObserver(String appId, String appToken, Context context) {
        EstimoteCloudCredentials estimoteCloudCredentials = new EstimoteCloudCredentials(appId, appToken);
        return new ProximityObserverBuilder(context, estimoteCloudCredentials)
                .onError(new Function1<Throwable, Unit>() {
                    @Override
                    public Unit invoke(Throwable throwable) {
                        Log.e("app", "proximity observer error: " + throwable);
                        return null;
                    }
                })
                .withBalancedPowerMode()
                .withAnalyticsReportingDisabled()
                .withEstimoteSecureMonitoringDisabled()
                .withTelemetryReportingDisabled()
                .build();

        //add build option withAnalyticsReportingDisabled to avoid app crash if not network
        //ref : https://github.com/Estimote/Android-Proximity-SDK/issues/45
        //add build option withEstimoteSecureMonitoringDisabled / withTelemetryReportingDisabled to avoid init error: Bluetooth Low Energy scan failed with error code: 2
        //ref: https://github.com/Estimote/Android-Proximity-SDK/issues/48
    }

    private static List<ProximityZone> createProximityZones(String[] detectDistances,
                                                            boolean isBackground,
                                                            Function1<ProximityZoneContext, Unit> onEnterAction,
                                                            Function1<ProximityZoneContext, Unit> onExitAction,
                                                            Function1<Set<? extends ProximityZoneContext>, Unit> onChangeAction) {
        double customRange;

        List<ProximityZone> proximityZones = new ArrayList();
        for (String stringRange : detectDistances) {
            if (isBackground) {
                customRange = BACKGROUND_BEACON_DETECT_RANGE;
            } else {
                try {
                    customRange = Double.parseDouble(stringRange);
                } catch (Exception ex) {
                    customRange = 10.0;
                }
            }
            ProximityZone proximityZone =
                    new ProximityZoneBuilder()
                            .forTag(TAG_PREFIX + stringRange)
                            .inCustomRange(customRange)
                            .onEnter(onEnterAction)
                            .onExit(onExitAction)
                            .onContextChange(onChangeAction)
                            .build();

            proximityZones.add(proximityZone);
        }
        return proximityZones;
    }

    //Legacy SDK methods
    private void initLegacyBeaconManager(String appId, String appToken) {
        final Context context = this.context;

        EstimoteSDK.initialize(context, appId, appToken);
        this.beaconManager = new BeaconManager(context);

        this.beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startLocationDiscovery();
            }
        });

        //- location listener
        this.beaconManager.setLocationListener(new BeaconManager.LocationListener() {
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
                RCTNativeAppEventEmitter eventEmitter = ((ReactContext) context).getJSModule(RCTNativeAppEventEmitter.class);
                for (EstimoteLocation beacon : beacons) {
                    String beaconId = beacon.id.toHexString();
                    Boolean isWithinDetectionDistance = this.isWithinDetectionDistance(beacon.rssi, beacon.txPower, beaconId);
                    if (isWithinDetectionDistance) {
                        WritableMap map = Arguments.createMap();
                        map.putString("uid", beacon.id.toHexString());
                        WritableArray writableArray = Arguments.createArray();
                        writableArray.pushMap(map);
                        eventEmitter.emit(EMITTED_ONENTER_EVENT_NAME, writableArray);
                    }
                }

            }
        }.setDetectionDistance(this.detectionDistance));
    }

    private void startLegacyBeaconManager() {
        this.beaconManager.startLocationDiscovery();
    }

    private void stopLegacyBeaconManager() {
        this.beaconManager.stopLocationDiscovery();
    }

}
