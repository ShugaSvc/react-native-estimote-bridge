import { NativeModules, NativeEventEmitter } from 'react-native';
import codeHeardQueue from './src/codeHeardQueue';

const { RNEstimote } = NativeModules;

const Estimote = {
	addEventListener: function(callback) {
        new NativeEventEmitter(RNEstimote).addListener('RNEstimoteEvent', (data) => {
            let beaconCode = data.beaconCode;
            if (codeHeardQueue.shouldCodeInvokeCallback(beaconCode)) {
                callback(data);
            }
            codeHeardQueue.heardCode(beaconCode);
        });
	},

    /**
     * Start RNEstimote service to start monitoring service
     * @param {string} appId
     * @param {string} appToken
     */
    start: function(appId, appToken) {
        RNEstimote.start(appId, appToken);
    },

    stop: function() {
        RNEstimote.stop();
    },
};

export default Estimote;
