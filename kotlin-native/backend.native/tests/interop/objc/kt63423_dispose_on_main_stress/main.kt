/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import objclib.*

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.native.internal.test.testLauncherEntryPoint
import kotlin.native.internal.MemoryUsageInfo
import kotlin.system.exitProcess
import kotlin.test.*
import kotlinx.cinterop.*

class PeakRSSChecker(private val rssDiffLimitBytes: Long) {
    // On Linux, the child process might immediately commit the same amount of memory as the parent.
    // So, measure difference between peak RSS measurements.
    private val initialBytes = MemoryUsageInfo.peakResidentSetSizeBytes.also {
        check(it != 0L) { "Error trying to obtain peak RSS. Check if current platform is supported" }
    }

    fun check(): Long {
        val diffBytes = MemoryUsageInfo.peakResidentSetSizeBytes - initialBytes
        check(diffBytes <= rssDiffLimitBytes) { "Increased peak RSS by $diffBytes bytes which is more than $rssDiffLimitBytes" }
        return diffBytes
    }
}

class LockedSet<T> {
    private val lock = AtomicInt(0)
    private val impl = mutableSetOf<T>()

    private inline fun <R> locked(f: () -> R): R {
        while (!lock.compareAndSet(0, 1)) {}
        try {
            return f()
        } finally {
            lock.value = 0
        }
    }

    fun add(id: T) = locked {
        assertFalse(id in impl)
        impl.add(id)
    }

    fun remove(id: T) = locked {
        assertTrue(id in impl)
        impl.remove(id)
    }

    operator fun contains(id: T) = locked {
        id in impl
    }
}

val aliveObjectIds = LockedSet<ULong>()

fun alloc(dtor: () -> Unit): ULong = autoreleasepool {
    val id = OnDestroyHook {
        dtor()
        aliveObjectIds.remove(it)
    }.identity()
    aliveObjectIds.add(id)
    id
}

fun waitDestruction(ids: List<ULong>) {
    assertTrue(isMainThread())
    // Make sure the finalizers are not run on the main thread even for STMS.
    withWorker {
        execute(TransferMode.SAFE, {}) {
            kotlin.native.internal.GC.collect()
        }.result
    }
    while (true) {
        spin()
        if (ids.all { it !in aliveObjectIds })
            break
    }
}

fun main() = startApp {
    val ids = Array(500000) {
        alloc {
            assertTrue(isMainThread())
        }
    }
    val peakRSSChecker = PeakRSSChecker(10_000_000L) // ~10MiB allowed difference for running finalizers
    waitDestruction(ids.toList())
    peakRSSChecker.check()
}