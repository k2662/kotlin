/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include <CoreFoundation/CoreFoundation.h>
#include <CoreFoundation/CFRunLoop.h>

#include "RawPtr.hpp"
#include "Utils.hpp"

namespace kotlin::objc_support {

class RunLoopSource : private Pinned {
public:
    class [[nodiscard]] Subscription : private MoveOnly {
    public:
        ~Subscription() {
            if (!owner_) return;
            CFRunLoopRemoveSource(runLoop_, owner_->source_, mode_);
        }

    private:
        friend class RunLoopSource;

        Subscription(RunLoopSource& owner, CFRunLoopRef runLoop, CFRunLoopMode mode) noexcept : owner_(&owner), runLoop_(runLoop), mode_(mode) {
            CFRunLoopAddSource(runLoop_, owner_->source_, mode_);
        }

        raw_ptr<RunLoopSource> owner_;
        CFRunLoopRef runLoop_;
        CFRunLoopMode mode_;
    };

    template <typename Processor>
    explicit RunLoopSource(Processor&& processor) noexcept :
        processor_(std::forward<Processor>(processor)),
        sourceContext_{0, &processor_, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, &perform},
        source_(CFRunLoopSourceCreate(nullptr, 0, &sourceContext_)) {}

    ~RunLoopSource() { CFRelease(source_); }

    CFRunLoopSourceRef handle() noexcept { return source_; }

    void signal() noexcept { CFRunLoopSourceSignal(source_); }

    Subscription subscribe(CFRunLoopRef runLoop, CFRunLoopMode mode = kCFRunLoopDefaultMode) noexcept { return Subscription(*this, runLoop, mode); }

private:
    static void perform(void* processor) noexcept { static_cast<decltype(processor_)*>(processor)->operator()(); }

    std::function<void()> processor_; // TODO: std::function_ref?
    CFRunLoopSourceContext sourceContext_;
    CFRunLoopSourceRef source_;
};

} // namespace kotlin::objc_support

#endif