
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTLog.h>

#import <EstimoteProximitySDK/EstimoteProximitySDK.h>

@interface RNEstimote : RCTEventEmitter <RCTBridgeModule>

@property (nonatomic) EPXProximityObserver *proximityObserver;
@property (nonatomic) NSArray *zones;
@end

