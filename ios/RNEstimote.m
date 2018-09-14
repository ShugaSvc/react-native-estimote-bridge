
#import "RNEstimote.h"

@implementation RNEstimote

const double BACKGROUND_BEACON_DETECT_RANGE = 20;


- (id)init {
    self = [super init];
    return self;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

NSDictionary * contextToJSON(EPXProximityZoneContext *context) {
    return @{@"tag": context.tag,
             @"uid": context.deviceIdentifier};
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(init:(NSString *)appId withAppToken: (NSString *) appToken withBeaconZones:(NSArray *) detectDistances) {
    EPXCloudCredentials *cloudCredentials =[[EPXCloudCredentials alloc] initWithAppID:appId appToken:appToken];
    self.proximityObserver = [[EPXProximityObserver alloc]
                                        initWithCredentials:cloudCredentials
                                        onError:^(NSError * _Nonnull error) {
                                            NSLog(@"proximity observer error = %@" ,error);
                                        }];
    NSMutableArray * _zones = [[NSMutableArray alloc] init];
    for (NSString* distance in detectDistances) {
        __weak __typeof(self) weakSelf = self;
        double range = [distance doubleValue];
        if(range == 0) {
            range = 10.0;
        }
        EPXProximityZone *zone = [[EPXProximityZone alloc]
                                  initWithTag:[NSString stringWithFormat:@"range_ios_%@", distance]
                                  range:[EPXProximityRange customRangeWithDesiredMeanTriggerDistance:range]];

        zone.onExit = ^(EPXProximityZoneContext *context) {
            [weakSelf sendEventWithName: @"RNEstimoteEventOnLeave" body:contextToJSON(context)];
            RCTLogWarn(@"on leave: %@", contextToJSON(context));
        };

        zone.onContextChange = ^(NSSet<EPXProximityZoneContext *> *contexts) {
            NSMutableArray *convertedContexts = [NSMutableArray arrayWithCapacity:contexts.count];
            for (EPXProximityZoneContext *context in contexts) {
                [convertedContexts addObject:contextToJSON(context)];
            }
            [weakSelf sendEventWithName:@"RNEstimoteEventOnEnter" body:convertedContexts];
            RCTLogWarn(@"%@ beacon: %@", @(contexts.count), convertedContexts);
        };
        [_zones addObject:zone];
    }
    self.zones = [[NSArray alloc] init];
    self.zones = [self.zones arrayByAddingObjectsFromArray:_zones];
    [self.proximityObserver startObservingZones: self.zones];
}

RCT_EXPORT_METHOD(start) {
    [self.proximityObserver startObservingZones: self.zones];
}

RCT_EXPORT_METHOD(stop) {
    [self.proximityObserver stopObservingZones];
}

RCT_REMAP_METHOD(isSupportIOSProximityEstimoteSDK,
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    if ([[NSTimer class] respondsToSelector:@selector(scheduledTimerWithTimeInterval:repeats:block:)]) {
        resolve([NSNumber numberWithBool:YES]);
    } else {
        resolve([NSNumber numberWithBool:NO]);
    }
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"RNEstimoteEventOnEnter", @"RNEstimoteEventOnLeave"];
}
@end
