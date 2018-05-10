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
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;


public class EstimoteBeaconDetector {

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

    public EstimoteBeaconDetector(Context context) {
        this.context = context;
        this.isUseLegacySDK = (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
    }

    public boolean isUseLegacySDK() {
        return this.isUseLegacySDK;
    }

    public void init(String appId, String appToken, String[] detectDistances) {
        if (this.isUseLegacySDK) {
            this._initBeaconManager(appId, appToken);
        } else {
            this._initProximity(appId, appToken, detectDistances);
        }
    }

    public void start() {
        if (this.isUseLegacySDK) {
            this._startByBeaconManager();
        } else {
            this._startByProximity();
        }
    }

    public void stop() {
        if (this.isUseLegacySDK) {
            this._stopByBeaconManager();
        } else {
            this._stopByProximity();
        }
    }

    public void setBeaconDetectionDistance(double detectionDistance) {
        this.detectionDistance = detectionDistance;
    }

    public static void startBackendDetect(String appId, String appToken, String[] detectDistances, final Context context) {
        if(EstimoteBeaconDetector.backgroundObservationHandler != null)
            return;


        EstimoteCloudCredentials estimoteCloudCredentials = new EstimoteCloudCredentials(appId, appToken);
        EstimoteBeaconDetector.backgroundProximityObserver =
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
                        .withEstimoteSecureMonitoringDisabled()
                        .withTelemetryReportingDisabled()
                        .build();
        //add build option withAnalyticsReportingDisabled to avoid app crash if not network
        //ref : https://github.com/Estimote/Android-Proximity-SDK/issues/45
        //add build option withEstimoteSecureMonitoringDisabled / withTelemetryReportingDisabled to avoid init error: Bluetooth Low Energy scan failed with error code: 2
        //ref: https://github.com/Estimote/Android-Proximity-SDK/issues/48


        List<ProximityZone> proximityZones = new ArrayList();
        for (String stringRange : detectDistances) {
            ProximityZone proximityZone =
                    EstimoteBeaconDetector.backgroundProximityObserver.zoneBuilder()
                            .forAttachmentKeyAndValue("range", stringRange)
                            .inCustomRange(BACKGROUND_BEACON_DETECT_RANGE)
                            .withOnEnterAction(new Function1<ProximityAttachment, Unit>() {
                                @Override
                                public Unit invoke(ProximityAttachment proximityAttachment) {
                                    PreferenceHelper preferenceHelper = new PreferenceHelper(context);
                                    String beaconCode = proximityAttachment.getPayload().get("uid");
                                    preferenceHelper.setBeaconData(beaconCode, BeaconEventTypeEnum.ONENTER.toString());
                                    return null;
                                }
                            })
                            .withOnExitAction(new Function1<ProximityAttachment, Unit>() {
                                @Override
                                public Unit invoke(ProximityAttachment proximityAttachment) {
                                    PreferenceHelper preferenceHelper = new PreferenceHelper(context);
                                    String beaconCode = proximityAttachment.getPayload().get("uid");
                                    preferenceHelper.setBeaconData(beaconCode, BeaconEventTypeEnum.ONLEAVE.toString());
                                    return null;
                                }
                            })
                            .withOnChangeAction(new Function1<List<? extends ProximityAttachment>, Unit>() {
                                @Override
                                public Unit invoke(List<? extends ProximityAttachment> attachments) {
                                    PreferenceHelper preferenceHelper = new PreferenceHelper(context);
                                    for (ProximityAttachment attachment : attachments) {
                                        String beaconCode = attachment.getPayload().get("uid");
                                        preferenceHelper.setBeaconData(beaconCode, BeaconEventTypeEnum.ONCHANGE.toString());
                                    }
                                    return null;
                                }
                            })
                            .create();

            proximityZones.add(proximityZone);
        }
        EstimoteBeaconDetector.backgroundObservationHandler = EstimoteBeaconDetector.backgroundProximityObserver.addProximityZones(proximityZones).start();
    }

    private void _initProximity(String appId, String appToken, String[] detectDistances) {
        final Context context = this.context;
        EstimoteCloudCredentials estimoteCloudCredentials = new EstimoteCloudCredentials(appId, appToken);
        EstimoteBeaconDetector.foregroundProximityObserver =
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
                        .withEstimoteSecureMonitoringDisabled()
                        .withTelemetryReportingDisabled()
                        .build();
        //add build option withAnalyticsReportingDisabled to avoid app crash if not network
        //ref : https://github.com/Estimote/Android-Proximity-SDK/issues/45
        //add build option withEstimoteSecureMonitoringDisabled / withTelemetryReportingDisabled to avoid init error: Bluetooth Low Energy scan failed with error code: 2
        //ref: https://github.com/Estimote/Android-Proximity-SDK/issues/48

        List<ProximityZone> proximityZones = new ArrayList();
        for (String stringRange : detectDistances) {
            try {
                Double.parseDouble(stringRange);
            } catch (Exception ex) {
                stringRange = "10";
            }

            ProximityZone proximityZone =
                    EstimoteBeaconDetector.foregroundProximityObserver.zoneBuilder()
                            .forAttachmentKeyAndValue("range", stringRange)
                            .inCustomRange(Double.parseDouble(stringRange))
                            .withOnExitAction(new Function1<ProximityAttachment, Unit>() {
                                @Override
                                public Unit invoke(ProximityAttachment proximityAttachment) {
                                    WritableMap map = MapUtil.toWritableMap(proximityAttachment.getPayload());
                                    RCTNativeAppEventEmitter eventEmitter = ((ReactContext) context).getJSModule(RCTNativeAppEventEmitter.class);
                                    eventEmitter.emit(EMITTED_ONLEAVE_EVENT_NAME, map);
                                    return null;
                                }
                            })
                            .withOnChangeAction(new Function1<List<? extends ProximityAttachment>, Unit>() {
                                @Override
                                public Unit invoke(List<? extends ProximityAttachment> attachments) {
                                    WritableArray writableArray = Arguments.createArray();
                                    for (ProximityAttachment attachment : attachments) {
                                        WritableMap map = MapUtil.toWritableMap(attachment.getPayload());
                                        writableArray.pushMap(map);
                                    }
                                    RCTNativeAppEventEmitter eventEmitter = ((ReactContext) context).getJSModule(RCTNativeAppEventEmitter.class);
                                    eventEmitter.emit(EMITTED_ONENTER_EVENT_NAME, writableArray);
                                    return null;
                                }
                            })
                            .create();

            proximityZones.add(proximityZone);
        }
        EstimoteBeaconDetector.foregroundObservationHandler = EstimoteBeaconDetector.foregroundProximityObserver.addProximityZones(proximityZones).start();
    }

    private void _startByProximity() {
        if (EstimoteBeaconDetector.foregroundObservationHandler == null) {
            EstimoteBeaconDetector.foregroundObservationHandler = EstimoteBeaconDetector.foregroundProximityObserver.start();
        }
    }

    private void _stopByProximity() {
        if (EstimoteBeaconDetector.foregroundObservationHandler != null) {
            EstimoteBeaconDetector.foregroundObservationHandler.stop();
            EstimoteBeaconDetector.foregroundObservationHandler = null;
        }
    }

    private void _initBeaconManager(String appId, String appToken) {
        final Context context = this.context.getApplicationContext();

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

    private void _startByBeaconManager() {
        this.beaconManager.startLocationDiscovery();
    }

    private void _stopByBeaconManager() {
        this.beaconManager.stopLocationDiscovery();
    }

}
