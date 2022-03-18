#import "SignInWithTwitterPlugin.h"
#import <TwitterKit/TWTRKit.h>
#import <Twitter/Twitter.h>

@implementation SignInWithTwitterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"sign_in_with_twitter"
            binaryMessenger:[registrar messenger]];
  SignInWithTwitterPlugin* instance = [[SignInWithTwitterPlugin alloc] init];
  [registrar addApplicationDelegate:instance];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  [self initializeTwitterInstance:call];
  if ([@"authorize" isEqualToString:call.method]) {
     [self authorize:result];
  } else  if ([@"logOut" isEqualToString:call.method]) {
     [self logOut:result];
  } else  {
    result(FlutterMethodNotImplemented);
  }
}

- (void)initializeTwitterInstance:(FlutterMethodCall *)call {
  NSString *consumerKey = call.arguments[@"consumerKey"];
  NSString *consumerSecret = call.arguments[@"consumerSecret"];

  if (consumerKey != nil && consumerSecret != nil) {
    [[Twitter sharedInstance]
        startWithConsumerKey:call.arguments[@"consumerKey"]
              consumerSecret:call.arguments[@"consumerSecret"]];
  }
}

- (void)authorize:(FlutterResult)result {
  [[Twitter sharedInstance]
      logInWithCompletion:^(TWTRSession *session, NSError *error) {
        if (error == nil) {
          TWTRAPIClient *client = [TWTRAPIClient clientWithCurrentUser];
          [client requestEmailForCurrentUser:^(NSString *email, NSError *error) {
            result(@{
              @"status" : @"loggedIn",
              @"session" : [self sessionDataToMap:session email:email],
            });
          }];
          // TWTRAPIClient *client = [TWTRAPIClient clientWithCurrentUser];
          // [client loadUserWithID:userId completion:^(TWTRUser * _Nullable user, NSError * _Nullable error) {
          //     if (user) {
          //         NSLog(@"头像url:%@",user.profileImageURL);
          //     }else{
          //         NSLog(@"error:%@",error.localizedDescription);
          //     }
          // }];
        } else {
          result(@{
            @"status" : @"error",
            @"errorMessage" : error.description,
          });
        }
      }];
}

- (id)sessionDataToMap:(TWTRSession *)session email:(NSString *)email {
  if (session == nil) {
    return [NSNull null];
  }
  return @{
    @"secret" : session.authTokenSecret,
    @"token" : session.authToken,
    @"userId" : session.userID,
    @"username" : session.userName,
    @"email" : email
  };
}

- (void)logOut:(FlutterResult)result {
  NSString *signedInUserId = [TWTRAPIClient clientWithCurrentUser].userID;

  if (signedInUserId != nil) {
    [[Twitter sharedInstance].sessionStore logOutUserID:signedInUserId];
  }

  result(nil);
}


- (BOOL)application:(UIApplication *)app
            openURL:(NSURL *)url
            options:(NSDictionary<NSString *, id> *)options {
  return [[Twitter sharedInstance] application:app openURL:url options:options];
}
@end
