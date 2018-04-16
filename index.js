import { NativeModules, NativeEventEmitter } from 'react-native';

const { RNEstimote } = NativeModules;

const Estimote = {
    addOnEnterEventListener: function(callback) {
        new NativeEventEmitter(RNEstimote).addListener('RNEstimoteEventOnEnter', (data) => {
            callback(data);
    });
    },

    addOnLeaveEventListener: function(callback) {
        new NativeEventEmitter(RNEstimote).addListener('RNEstimoteEventOnLeave', (data) => {
            callback(data);
    });
    },

    start: function(appId, appToken, beaconZones, attachmentKey) {
        RNEstimote.start(appId, appToken, beaconZones, attachmentKey);
    },

    stop: function() {
        RNEstimote.stop();
    },
};

export default Estimote;
