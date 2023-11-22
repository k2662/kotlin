/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ObjCFrameworkCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.ExecutionTimeout
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.LLDB
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.konan.test.blackbox.support.util.LLDBSessionSpec
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File


@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
@EnforcedHostTarget
class ObjCToKotlinSteppingInLLDBTest : AbstractNativeSimpleTest() {

    @Test
    fun stepInFromObjCToKotlin___WithDisabledStopHook___StopsAtABridgingRoutine() {
        testSteppingFromObjcToKotlin(
            """
            > b ${CLANG_FILE_NAME}:3
            > env KONAN_LLDB_DONT_SKIP_BRIDGING_FUNCTIONS=1
            > run
            > thread step-in
            > frame select
            [..]`objc2kotlin_kfun:#bar(){} at <compiler-generated>:1
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepInFromObjCToKotlin___WithDisabledStopHook___StopsAtABridgingRoutine.name}"
        )
    }

    @Test
    fun stepInFromObjCToKotlin___WithStopHook___StepsThroughToKotlinCode() {
        testSteppingFromObjcToKotlin(
            """
            > b ${CLANG_FILE_NAME}:3
            > run
            > thread step-in
            > frame select
            [..]`kfun:#bar(){} at lib.kt:1:1
            -> 1   	fun bar() {
                    ^
               2   	    print("")
               3   	}
            > c
            """.trimIndent(),
            CLANG_FILE_NAME,
            "${ObjCToKotlinSteppingInLLDBTest::class.qualifiedName}.${::stepInFromObjCToKotlin___WithStopHook___StepsThroughToKotlinCode.name}"
        )
    }

    private fun testSteppingFromObjcToKotlin(
        lldbSpec: String,
        clangFileName: String,
        testName: String,
    ) {
        if (!targets.testTarget.family.isAppleFamily) { Assumptions.abort<Nothing>("This test is supported only on Apple targets") }

        val kotlinFrameworkName = "Kotlin"
        val clangMainSources = """
            @import ${kotlinFrameworkName};
            int main() {
                [${kotlinFrameworkName}LibKt bar];
            }
        """.trimIndent()

        val kotlinFileName = "lib.kt"
        val kotlinLibrarySources = """
            fun bar() {
                print("")
            }
        """.trimIndent()

        runTestWithLLDB(
            kotlinLibrarySources = kotlinLibrarySources,
            kotlinFileName = kotlinFileName,
            kotlinFrameworkName = kotlinFrameworkName,
            clangMainSources = clangMainSources,
            clangFileName = clangFileName,
            lldbSpec = lldbSpec,
            testName = testName,
        )
    }

    private fun runTestWithLLDB(
        kotlinLibrarySources: String,
        kotlinFileName: String,
        kotlinFrameworkName: String,
        clangMainSources: String,
        clangFileName: String,
        lldbSpec: String,
        testName: String,
    ) {
        // 1. Create sources
        val sourceDirectory = buildDir.resolve("sources")
        sourceDirectory.createDirectory()
        val clangFile = sourceDirectory.resolve(clangFileName)
        clangFile.writeText(clangMainSources)
        sourceDirectory.resolve(kotlinFileName).writeText(kotlinLibrarySources)

        // 2. Build Kotlin framework
        val freeCompilerArgs = TestCompilerArgs(
            testRunSettings.get<PipelineType>().compilerFlags + listOf(
                "-Xstatic-framework",
                "-Xbinary=bundleId=stub",
            )
        )
        val module = generateTestCaseWithSingleModule(sourceDirectory, freeCompilerArgs)
        ObjCFrameworkCompilation(
            testRunSettings,
            freeCompilerArgs = freeCompilerArgs,
            sourceModules = module.modules,
            dependencies = emptyList(),
            expectedArtifact = TestCompilationArtifact.ObjCFramework(
                buildDir,
                kotlinFrameworkName,
            )
        ).result.assertSuccess()

        // 3. Compile the executable
        val clangExecutableName = "clangMain"
        val executableFile = File(buildDir, clangExecutableName)

        assert(
            ProcessBuilder(
                "${testRunSettings.configurables.absoluteTargetToolchain}/bin/clang",
                clangFile.absolutePath,
                "-isysroot", testRunSettings.configurables.absoluteTargetSysRoot,
                "-target", testRunSettings.configurables.targetTriple.toString(),
                "-g", "-fmodules",
                "-F", buildDir.absolutePath,
                "-o", executableFile.absolutePath
            ).inheritIO().start().waitFor() == 0
        )

        // 4. Generate the test case
        val testExecutable = TestExecutable(
            TestCompilationArtifact.Executable(executableFile),
            loggedCompilationToolCall = LoggedData.NoopCompilerCall(buildDir),
            testNames = listOf(TestName(testName)),
        )
        val spec = LLDBSessionSpec.parse(lldbSpec)
        val moduleForTestCase = TestModule.Exclusive(testName, emptySet(), emptySet(), emptySet())
        val testCase = TestCase(
            id = TestCaseId.Named(testName),
            kind = TestKind.STANDALONE_LLDB,
            modules = setOf(moduleForTestCase),
            freeCompilerArgs = freeCompilerArgs,
            nominalPackageName = PackageName.EMPTY,
            checks = TestRunChecks(
                executionTimeoutCheck = ExecutionTimeout.ShouldNotExceed(testRunSettings.get<Timeouts>().executionTimeout),
                exitCodeCheck = TestRunCheck.ExitCode.Expected(0),
                expectedFailureCheck = null,
                outputDataFile = null,
                outputMatcher = spec.let { TestRunCheck.OutputMatcher { output -> spec.checkLLDBOutput(output, testRunSettings.get()) } },
                fileCheckMatcher = null,
            ),
            extras = TestCase.NoTestRunnerExtras(
                "main",
                arguments = spec.generateCLIArguments(testRunSettings.get<LLDB>().prettyPrinters)
            )
        ).apply { initialize(null, null) }

        // 5. Run the test
        runExecutableAndVerify(testCase, testExecutable)
    }

    companion object {
        val CLANG_FILE_NAME = "main.m"
    }

}
