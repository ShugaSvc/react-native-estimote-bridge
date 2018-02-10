
#import "RNEstimote.h"

@implementation RNEstimote

- (id)init {
    self = [super init];
    self.detectionDistance = 6.0; //Default detection distance, in case we needed to change it from js someday
    return self;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(setBeaconDetectionDistance:(NSNumber *)distance) {
    self.detectionDistance = distance.floatValue;
}

RCT_EXPORT_METHOD(start:(NSString *)appId withAppToken: (NSString *) appToken) {
    [ESTConfig setupAppID:appId andAppToken:appToken];
    
    self.monitoringManager = [[ESTMonitoringV2Manager alloc]
                              initWithDesiredMeanTriggerDistance:self.detectionDistance
                              delegate:self];
    
    if (self.devices.count > 0) { //devices had been set
        [self startMonitorDevice];
    }
}

RCT_EXPORT_METHOD(stop) {
    [self.monitoringManager stopMonitoring];
}

RCT_EXPORT_METHOD(setBeaconDevices:(NSArray *)beaconDevices) {
    [self setDevices:beaconDevices];
    
    if (self.monitoringManager != nil) { //: is initialized
        [self stop];
        [self startMonitorDevice];
    }
}

- (NSString *)getEventName {
    return @"RNEstimoteEvent";
}

- (NSArray<NSString *> *)supportedEvents {
    return @[[self getEventName]];
}

- (void)startMonitorDevice {
    [self.monitoringManager startMonitoringForIdentifiers:self.devices];
}

- (void)emitBeaconEvent:(NSString *)identifier {
    [self sendEventWithName:[self getEventName]
                       body:@{@"beaconCode": identifier}];
}

#pragma mark > Delegate: Monitoring
- (void)monitoringManager:(ESTMonitoringV2Manager *)manager didDetermineInitialState:(ESTMonitoringState)state forBeaconWithIdentifier:(NSString *)identifier {
    // state codes: 0 = unknown, 1 = inside, 2 = outside
    if (state == 1) {
        [self emitBeaconEvent:identifier];
    }
}

- (void)monitoringManager:(nonnull ESTMonitoringV2Manager *)manager didEnterDesiredRangeOfBeaconWithIdentifier:(nonnull NSString *)identifier {
    [self emitBeaconEvent:identifier];
}

- (void)monitoringManager:(nonnull ESTMonitoringV2Manager *)manager
         didFailWithError:(nonnull NSError *)error {
    RCTLogWarn(@"Estimote Monitoring Manager failed with error: %@", error);
}

@end
