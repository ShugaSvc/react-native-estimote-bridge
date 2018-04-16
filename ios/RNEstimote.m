
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
    [self.proximityObserver stopObservingZones];
    self.proximityObserver = nil;
}

RCT_EXPORT_METHOD(start:(NSString *)appId withAppToken: (NSString *) appToken withBeaconZones:(NSArray *) beaconZones withAttachmentKey: (NSString *) attachmentKey) {
    EPXCloudCredentials *cloudCredentials =
    [[EPXCloudCredentials alloc] initWithAppID:appId
                                      appToken:appToken];

    self.proximityObserver = [[EPXProximityObserver alloc]
                              initWithCredentials:cloudCredentials
                              errorBlock:^(NSError * _Nonnull error) {
                                  RCTLogWarn(@"proximity observer error = %@", error);
                              }];

    NSMutableArray * _zones = [[NSMutableArray alloc] init];

    for (id zone in beaconZones) {
        double range = [[zone valueForKey:@"range"] floatValue];
        if(range == 0) {
            range = 10.0;
        }

        NSString *attachmentValue = [zone valueForKey:attachmentKey];

        EPXProximityZone *zone = [[EPXProximityZone alloc]
                                  initWithRange:[EPXProximityRange customRangeWithDesiredMeanTriggerDistance: range]
                                  attachmentKey: attachmentKey
                                  attachmentValue: attachmentValue];

        zone.onEnterAction = ^(EPXDeviceAttachment * _Nonnull attachment) {
            [self sendEventWithName: @"RNEstimoteEventOnEnter"
                               body:attachment.payload];

        };
        zone.onExitAction = ^(EPXDeviceAttachment * _Nonnull attachment) {
            [self sendEventWithName: @"RNEstimoteEventOnLeave"
                               body:attachment.payload];
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
