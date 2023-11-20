#include "objclib.h"

#include <dispatch/dispatch.h>
#import <AppKit/NSApplication.h>
#import <Foundation/NSRunLoop.h>
#import <Foundation/NSThread.h>

@implementation OnDestroyHook {
    void (^onDestroy_)(uintptr_t);
}

- (uintptr_t)identity {
    return (uintptr_t)self;
}

- (instancetype)init:(void (^)(uintptr_t))onDestroy {
    if (self = [super init]) {
        onDestroy_ = onDestroy;
    }
    return self;
}

- (void)dealloc {
    onDestroy_([self identity]);
}

@end

void startApp(void (^task)()) {
    dispatch_async(dispatch_get_main_queue(), ^{
        // At this point all other scheduled main queue tasks were already executed.
        // Executing via performBlock to allow a recursive run loop in `spin()`.
        [[NSRunLoop currentRunLoop] performBlock:^{
            task();
            [NSApp terminate:NULL];
        }];
    });
    [[NSApplication sharedApplication] run];
}

BOOL isMainThread() {
    return [NSThread isMainThread];
}

void spin() {
    if ([NSRunLoop currentRunLoop] != [NSRunLoop mainRunLoop]) {
        fprintf(stderr, "Must spin main run loop\n");
        exit(1);
    }
    [[NSRunLoop currentRunLoop]
           runMode:NSDefaultRunLoopMode
        beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
}
