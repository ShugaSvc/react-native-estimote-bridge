#import <Foundation/Foundation.h>
#import <EstimoteSDK/EstimoteSDK.h>

@interface BGEstimoteManager : NSObject

+ (BGEstimoteManager *)sharedInstance;
- (void)startWithAppId:(NSString *)appId andAppToken:(NSString *)appToken delegate:(id<ESTMonitoringV2ManagerDelegate>)delegate;
- (void)stop;
- (void)pause;
- (void)resume;

@end

