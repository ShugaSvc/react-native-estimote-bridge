
#import "RNEstimote.h"

@implementation RNEstimote

- (id)init {
    self = [super init];
    return self;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(stop) {
    if(self.proximityObserver != nil) {
        [self.proximityObserver stopObservingZones];
         self.proximityObserver = nil;
     }
}

RCT_EXPORT_METHOD(start:(NSString *)appId withAppToken: (NSString *) appToken withBeaconZones:(NSArray *) detectDistances) {
    EPXCloudCredentials *cloudCredentials =
    [[EPXCloudCredentials alloc] initWithAppID:appId
                                      appToken:appToken];

    self.proximityObserver = [[EPXProximityObserver alloc]
                              initWithCredentials:cloudCredentials
                              errorBlock:^(NSError * _Nonnull error) {
                                  RCTLogWarn(@"proximity observer error = %@", error);
                              }];

    NSMutableArray * _zones = [[NSMutableArray alloc] init];

    for (NSString* distance in detectDistances) {
        double range = [distance doubleValue];
        if(range == 0) {
            range = 10.0;
        }
        EPXProximityZone *zone = [[EPXProximityZone alloc]
                                  initWithRange:[EPXProximityRange customRangeWithDesiredMeanTriggerDistance: range]
                                  attachmentKey: @"range"
                                  attachmentValue: distance];

        zone.onExitAction = ^(EPXDeviceAttachment * _Nonnull attachment) {
            [self sendEventWithName: @"RNEstimoteEventOnLeave"
                               body:attachment.payload];

            //RCTLogWarn(@"on leave: %@", attachment.payload);
        };
        zone.onChangeAction = ^(NSSet<EPXDeviceAttachment *> * _Nonnull attachmentsCurrentlyInside) {
            NSArray *_attachment = [[attachmentsCurrentlyInside valueForKey:@"payload"] allObjects];
            [self sendEventWithName: @"RNEstimoteEventOnEnter"
                                       body:_attachment];
            //RCTLogWarn(@"%@ beacon: %@", @(attachmentsCurrentlyInside.count), attachmentsCurrentlyInside);
        };
        [_zones addObject:zone];
    }
    self.zones = [[NSArray alloc] init];
    self.zones = [self.zones arrayByAddingObjectsFromArray:_zones];
    if(self.proximityObserver != nil) {
        [self.proximityObserver startObservingZones: self.zones];
    }
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"RNEstimoteEventOnEnter", @"RNEstimoteEventOnLeave"];
}
@end
