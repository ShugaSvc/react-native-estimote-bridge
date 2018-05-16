
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#import "RCTEventEmitter.h"
#import "RCTLog.h"
#else
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTLog.h>
#endif

#import <EstimoteProximitySDK/EstimoteProximitySDK.h>

@interface RNEstimote : RCTEventEmitter <RCTBridgeModule>

@property (nonatomic) EPXProximityObserver *proximityObserver;
@property (nonatomic) NSArray *zones;
@end

