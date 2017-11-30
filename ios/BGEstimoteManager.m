//TODO: remove all non-error nslogs

#import "BGEstimoteManager.h"
#import <UIKit/UIKit.h>
#import <UserNotifications/UserNotifications.h>

@interface BGEstimoteManager ()
@property (nonatomic) ESTMonitoringV2Manager *monitoringManager;

@property (strong) NSString *appId;
@property (strong) NSString *appToken;
@property (strong) NSArray<NSString *> *deviceIdentifiers;
@property (strong) id<ESTMonitoringV2ManagerDelegate> delegate;

@property (assign) BOOL isPaused;
@end

@implementation BGEstimoteManager

+ (BGEstimoteManager *)sharedInstance {
    static BGEstimoteManager *sharedInstance = nil;
    
    if (sharedInstance == nil) {
        sharedInstance = [BGEstimoteManager new];
    }
    
    return sharedInstance;
}

- (void)startWithAppId:(NSString *)appId andAppToken:(NSString *)appToken delegate:(id<ESTMonitoringV2ManagerDelegate>)delegate {
    self.isPaused = false;
    self.appId = appId;
    self.appToken = appToken;
    self.delegate = delegate;
    
    [ESTConfig setupAppID:self.appId andAppToken:self.appToken];
    
    [self retrieveAllBeaconDevicesWithCompletionHandler:^(NSArray *deviceIdentifiers) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (!self.isPaused) { //<- might be paused halfway upon retrieving
                self.deviceIdentifiers = deviceIdentifiers;
                self.monitoringManager = [[ESTMonitoringV2Manager alloc]
                                          initWithDesiredMeanTriggerDistance:100 delegate:self.delegate];
                [self.monitoringManager startMonitoringForIdentifiers:self.deviceIdentifiers];
            }
        });
    }];
}

- (void)stop {
    if (nil != self.monitoringManager) {
        [self.monitoringManager stopMonitoring];
    }
}

- (void)pause {
    if (nil != self.monitoringManager) {
        [self.monitoringManager stopMonitoring];
        self.isPaused = true;
    }
}

- (void)resume {
    if ((nil != self.monitoringManager) && self.isPaused) {
        [self.monitoringManager startMonitoringForIdentifiers:self.deviceIdentifiers];
    }
}

- (void)retrieveAllBeaconDevicesWithCompletionHandler:(void (^)(NSArray *deviceIdentifiers))callbackBlock {
    NSString *urlString = [NSString stringWithFormat:@"https://cloud.estimote.com/v2/devices"];
    NSURL *url = [NSURL URLWithString:urlString];
    
    //Set basic auth
    NSString *authStr = [NSString stringWithFormat:@"%@:%@", self.appId, self.appToken];
    NSData *authData = [authStr dataUsingEncoding:NSASCIIStringEncoding];
    NSString* authStrData = [[NSString alloc] initWithData:[authData base64EncodedDataWithOptions:NSDataBase64EncodingEndLineWithLineFeed] encoding:NSASCIIStringEncoding];
    NSString *authValue = [NSString stringWithFormat:@"Basic %@", authStrData];
    
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    [request setURL:url];
    [request setHTTPMethod:@"GET"];
    [request setValue:authValue forHTTPHeaderField:@"Authorization"];
    
    NSURLSession *session = [NSURLSession sessionWithConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];
    [[session dataTaskWithRequest:request completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        // handle basic connectivity issues here
        if (error) {
            NSLog(@"retrieveAllBeaconDevicesWithCompletionHandler failed with error: %@", error);
            return;
        }
        
        // handle HTTP errors here
        if ([response isKindOfClass:[NSHTTPURLResponse class]]) {
            NSInteger statusCode = [(NSHTTPURLResponse *)response statusCode];
            if (statusCode != 200) {
                NSLog(@"retrieveAllBeaconDevicesWithCompletionHandler HTTP status code: %ld", (long)statusCode);
                return;
            }
        }
        
        //Parse JSON
        NSArray *array = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:&error];
        if (error) {
            NSLog(@"retrieveAllBeaconDevicesWithCompletionHandler failed with error: %@", error);
            return;
        }
        
        //Return identifiers as NSArray
        NSMutableArray *identifiers = [NSMutableArray new];
        for (NSDictionary *device in array) {
            NSString *identifier = [device objectForKey:@"identifier"];
            [identifiers addObject:identifier];
        }
        callbackBlock([identifiers mutableCopy]);
    }] resume];
}

@end

