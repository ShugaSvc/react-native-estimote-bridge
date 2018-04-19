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

    start: function(appId, appToken, detectDistances) {
        RNEstimote.start(appId, appToken, detectDistances);
    },

    stop: function() {
        RNEstimote.stop();
    },
};

export default Estimote;
