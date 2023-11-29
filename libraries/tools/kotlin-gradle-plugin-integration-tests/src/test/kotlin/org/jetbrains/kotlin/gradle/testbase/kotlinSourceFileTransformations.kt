/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.testbase.TransformationsGeneratorState.changeCounter
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.appendText


fun Path.addPrivateVal(): Path {
    appendText("private val integerValue${changeCounter.incrementAndGet()} = 24")
    return this
}

/**
 * Ensures that generated names are unique.
 */
private object TransformationsGeneratorState {
    val changeCounter = AtomicInteger(0)
}
