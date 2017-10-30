
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#import "RCTEventEmitter.h"
#import "RCTLog.h"
#else
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTLog.h>
#endif

#import <EstimoteSDK/EstimoteSDK.h>

@interface RNEstimote : RCTEventEmitter <RCTBridgeModule, ESTMonitoringV2ManagerDelegate>

@property (nonatomic) ESTMonitoringV2Manager *monitoringManager;
@property (nonatomic) NSArray *devices;
@property (assign) float detectionDistance;

@end
  
