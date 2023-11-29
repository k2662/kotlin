/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.native.swift.sir

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.sir.analysisapi.AbstractKotlinSirContextTest
import org.jetbrains.kotlin.sir.bridge.AbstractKotlinSirBridgeTest


fun main() {
    System.setProperty("java.awt.headless", "true")
    generateTestGroupSuiteWithJUnit5 {
        testGroup(
            "native/swift/sir-analysis-api/tests-gen/",
            "native/swift/sir-analysis-api/testData"
        ) {
            testClass<AbstractKotlinSirContextTest>(
                suiteTestClassName = "SirAnalysisGeneratedTests"
            ) {
                model("", pattern = "^([^_](.+)).kt$", recursive = false)
            }
        }
        testGroup(
            "native/swift/sir-compiler-bridge/tests-gen/",
            "native/swift/sir-compiler-bridge/testData"
        ) {
            testClass<AbstractKotlinSirBridgeTest>(
                suiteTestClassName = "SirCompilerBridgeTestGenerated"
            ) {
                model("", extension = null, recursive = false)
            }
        }
    }
}
