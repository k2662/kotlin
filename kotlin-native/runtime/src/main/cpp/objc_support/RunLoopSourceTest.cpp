/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include "RunLoopSource.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ScopedThread.hpp"

using namespace kotlin;

namespace {

class RunLoopInScopedThread : private Pinned {
public:
    explicit RunLoopInScopedThread(objc_support::RunLoopSource& source) noexcept :
        thread_([&]() noexcept {
            auto runLoop = CFRunLoopGetCurrent();
            runLoop_.store(runLoop, std::memory_order_release);
            auto subscription = source.subscribe(runLoop);
            CFRunLoopRun();
        }) {
        while (runLoop_.load(std::memory_order_acquire) == nullptr) {
        }
    }

    ~RunLoopInScopedThread() {
        CFRunLoopStop(runLoop_.load(std::memory_order_relaxed));
        thread_.join();
    }

    void wakeUp() noexcept { CFRunLoopWakeUp(runLoop_.load(std::memory_order_relaxed)); }

private:
    std::atomic<CFRunLoopRef> runLoop_ = nullptr;
    ScopedThread thread_;
};

class Checkpoint : private Pinned {
public:
    Checkpoint() noexcept = default;

    void checkpoint() noexcept { value_.fetch_add(1, std::memory_order_release); }

    template <typename F>
    int64_t performAndWaitChange(F&& f) noexcept {
        auto initial = value_.load(std::memory_order_acquire);
        std::invoke(std::forward<F>(f));
        while (value_.load(std::memory_order_relaxed) <= initial) {
            std::this_thread::yield();
        }
        auto final = value_.load(std::memory_order_acquire);
        return final - initial;
    }

private:
    std::atomic<int64_t> value_ = 0;
};

} // namespace

TEST(RunLoopSourceTest, SignalRunsOnce) {
    Checkpoint checkpoint;
    objc_support::RunLoopSource source([&]() noexcept { checkpoint.checkpoint(); });
    RunLoopInScopedThread runLoop(source);
    ASSERT_THAT(checkpoint.performAndWaitChange([&]() noexcept {
        source.signal();
        runLoop.wakeUp();
    }), 1);
    ASSERT_THAT(checkpoint.performAndWaitChange([&]() noexcept {
        source.signal();
        runLoop.wakeUp();
    }), 1);
}

TEST(RunLoopSourceTest, ConnectToDifferentLoop) {
    Checkpoint checkpoint;
    objc_support::RunLoopSource source([&]() noexcept { checkpoint.checkpoint(); });
    {
        RunLoopInScopedThread runLoop(source);
        ASSERT_THAT(checkpoint.performAndWaitChange([&]() noexcept {
            source.signal();
            runLoop.wakeUp();
        }), 1);
    }
    {
        RunLoopInScopedThread runLoop(source);
        ASSERT_THAT(checkpoint.performAndWaitChange([&]() noexcept {
            source.signal();
            runLoop.wakeUp();
        }), 1);
    }
}

#endif