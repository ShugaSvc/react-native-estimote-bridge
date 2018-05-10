
#import "RNEstimote.h"

@implementation RNEstimote

static EPXProximityObserver *backgroundProximityObserver = nil;
const double BACKGROUND_BEACON_DETECT_RANGE = 20;

- (id)init {
    self = [super init];
    return self;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(init:(NSString *)appId withAppToken: (NSString *) appToken withBeaconZones:(NSArray *) detectDistances) {
    [self _init:appId withAppToken:appToken withBeaconZones:detectDistances];
}

RCT_EXPORT_METHOD(start) {
    [self _start];
}

RCT_EXPORT_METHOD(stop) {
    [self _stop];
}

- (void)_init:(NSString *)appId withAppToken: (NSString *) appToken withBeaconZones:(NSArray *) detectDistances{
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

            RCTLogWarn(@"on leave: %@", attachment.payload);
        };
        zone.onChangeAction = ^(NSSet<EPXDeviceAttachment *> * _Nonnull attachmentsCurrentlyInside) {
            NSArray *_attachment = [[attachmentsCurrentlyInside valueForKey:@"payload"] allObjects];
            [self sendEventWithName: @"RNEstimoteEventOnEnter"
                               body:_attachment];
            RCTLogWarn(@"%@ beacon: %@", @(attachmentsCurrentlyInside.count), attachmentsCurrentlyInside);
        };
        [_zones addObject:zone];
    }
    self.zones = [[NSArray alloc] init];
    self.zones = [self.zones arrayByAddingObjectsFromArray:_zones];
    if(self.proximityObserver != nil) {
        [self.proximityObserver startObservingZones: self.zones];
    }
}



- (void)_start {
    if(self.proximityObserver != nil) {
        [self.proximityObserver startObservingZones: self.zones];
    }
}

- (void)_stop {
    if(self.proximityObserver != nil) {
        [self.proximityObserver stopObservingZones];
        self.proximityObserver = nil;
    }
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"RNEstimoteEventOnEnter", @"RNEstimoteEventOnLeave"];
}

+ (void)initBackendDetect:(NSString *)appId withAppToken: (NSString *) appToken withBeaconZones:(NSArray *) detectDistances {
    if(backgroundProximityObserver != nil)
        return;

    EPXCloudCredentials *cloudCredentials =
    [[EPXCloudCredentials alloc] initWithAppID:appId
                                      appToken:appToken];

    backgroundProximityObserver = [[EPXProximityObserver alloc]
                                   initWithCredentials:cloudCredentials
                                   errorBlock:^(NSError * _Nonnull error) {
                                       NSLog(@"proximity observer error = %@" ,error);
                                   }];

    NSMutableArray * _zones = [[NSMutableArray alloc] init];

    for (NSString* distance in detectDistances) {
        EPXProximityZone *zone = [[EPXProximityZone alloc]
                                  initWithRange:[EPXProximityRange customRangeWithDesiredMeanTriggerDistance: BACKGROUND_BEACON_DETECT_RANGE]
                                  attachmentKey: @"range"
                                  attachmentValue: distance];

        zone.onEnterAction = ^(EPXDeviceAttachment * _Nonnull attachment) {
            NSDictionary* payload =attachment.payload;
            NSString* beaconCode = [payload valueForKey:@"uid"];
            [RNEstimote setBeaconData:beaconCode withEventType:@"ONENTER"];
        };
        zone.onExitAction = ^(EPXDeviceAttachment * _Nonnull attachment) {
            NSDictionary* payload =attachment.payload;
            NSString* beaconCode = [payload valueForKey:@"uid"];
            [RNEstimote setBeaconData:beaconCode withEventType:@"ONLEAVE"];
        };
        zone.onChangeAction = ^(NSSet<EPXDeviceAttachment *> * _Nonnull attachmentsCurrentlyInside) {
            NSArray *attachments = [[attachmentsCurrentlyInside valueForKey:@"payload"] allObjects];
            for (id attachment in attachments) {
                NSDictionary* payload = attachment;
                NSString* beaconCode = [payload valueForKey:@"uid"];
                [RNEstimote setBeaconData:beaconCode withEventType:@"ONCHANGE"];
            }
        };
        [_zones addObject:zone];
    }
    NSArray* zones = [[NSArray alloc] init];
    zones = [zones arrayByAddingObjectsFromArray:_zones];
    [backgroundProximityObserver startObservingZones: zones];
}

+ (void)setBeaconData:(NSString *)beaconCode withEventType: (NSString *) eventType {
    NSTimeInterval timeStamp = [[NSDate date] timeIntervalSince1970];  // NSTimeInterval is defined as double
    NSNumber *eventTime = [NSNumber numberWithInteger: timeStamp];
    NSString* platform = @"ios";
    NSString* key = [NSString stringWithFormat:@"beaconlog-%@-%@-%@",eventTime,beaconCode,eventType];
    NSString* value = [NSString stringWithFormat:@"%@",platform];
    [[NSUserDefaults standardUserDefaults] setValue:value forKey:key];
}

@end
