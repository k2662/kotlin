#include <inttypes.h>
#include <objc/NSObject.h>

@interface OnDestroyHook: NSObject
-(instancetype)init:(void(^)(uintptr_t))onDestroy;
-(uintptr_t)identity;
@end

void startApp(void(^task)());
BOOL isMainThread();
void spin();