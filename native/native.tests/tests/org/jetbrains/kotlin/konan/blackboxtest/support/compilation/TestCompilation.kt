/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.compilation

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.ExecutableCompilation.Companion.applyTestRunnerSpecificArgs
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.ExecutableCompilation.Companion.assertTestDumpFileNotEmptyIfExists
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ArgsBuilder
import org.jetbrains.kotlin.konan.blackboxtest.support.util.buildArgs
import org.jetbrains.kotlin.konan.blackboxtest.support.util.flatMapToSet
import org.jetbrains.kotlin.konan.blackboxtest.support.util.mapToSet
import org.jetbrains.kotlin.konan.properties.resolvablePropertyList
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import java.io.File

internal abstract class TestCompilation<A : TestCompilationArtifact> {
    abstract val result: TestCompilationResult<out A>
}

internal abstract class BasicBasicCompilation<A : TestCompilationArtifact>(
    protected val targets: KotlinNativeTargets,
    protected val home: KotlinNativeHome,
    private val classLoader: KotlinNativeClassLoader,
    protected val freeCompilerArgs: TestCompilerArgs,
    protected val dependencies: CategorizedDependencies,
    protected val expectedArtifact: A
) : TestCompilation<A>() {
    protected abstract val sourceModules: Collection<TestModule>
    protected abstract val runner: CompilerToolRunner

    // Runs the compiler and memorizes the result on property access.
    final override val result: TestCompilationResult<out A> by lazy {
        val failures = dependencies.failures
        if (failures.isNotEmpty())
            TestCompilationResult.DependencyFailures(causes = failures)
        else
            doCompile()
    }

    protected abstract fun ArgsBuilder.applyCommonArgs()
    protected abstract fun applySpecificArgs(argsBuilder: ArgsBuilder)
    protected abstract fun applyDependencies(argsBuilder: ArgsBuilder)
    private fun ArgsBuilder.applyFreeArgs() {
        add(freeCompilerArgs.compilerArgs)
    }

    protected abstract fun ArgsBuilder.applySources()
    protected open fun postCompileCheck() = Unit
    private fun doCompile(): TestCompilationResult.ImmediateResult<out A> {
        val compilerArgs = buildArgs {
            applyCommonArgs()
            applySpecificArgs(this)
            applyDependencies(this)
            applyFreeArgs()
            applySources()
        }

        val loggedCompilerParameters = LoggedData.CompilerParameters(home, runner.toolName, compilerArgs, sourceModules)

        val (loggedCompilerCall: LoggedData, result: TestCompilationResult.ImmediateResult<out A>) = try {
            val (exitCode, compilerOutput, compilerOutputHasErrors, duration) = runner.run(
                compilerArgs = compilerArgs,
                kotlinNativeClassLoader = classLoader.classLoader
            )

            val loggedCompilerCall =
                LoggedData.CompilerCall(loggedCompilerParameters, exitCode, compilerOutput, compilerOutputHasErrors, duration)

            val result = if (exitCode != ExitCode.OK || compilerOutputHasErrors)
                TestCompilationResult.CompilerFailure(loggedCompilerCall)
            else
                TestCompilationResult.Success(expectedArtifact, loggedCompilerCall)

            loggedCompilerCall to result
        } catch (unexpectedThrowable: Throwable) {
            val loggedFailure = LoggedData.CompilerCallUnexpectedFailure(loggedCompilerParameters, unexpectedThrowable)
            val result = TestCompilationResult.UnexpectedFailure(loggedFailure)

            loggedFailure to result
        }

        expectedArtifact.logFile.writeText(loggedCompilerCall.toString())

        postCompileCheck()

        return result
    }

}

internal class CinteropCompilation(
    settings: Settings,
    freeCompilerArgs: TestCompilerArgs,
    dependencies: Iterable<TestCompilationDependency<*>>,
    expectedArtifact: KLIB,
    private val sourceModule: TestModule
) : BasicBasicCompilation<KLIB>(
    targets = settings.get(),
    home = settings.get(),
    classLoader = settings.get(),
    freeCompilerArgs = freeCompilerArgs,
    dependencies = CategorizedDependencies(dependencies),
    expectedArtifact = expectedArtifact
) {
    override val runner: CompilerToolRunner
        get() = CinteropRunner

    override val sourceModules: Collection<TestModule>
        get() = listOf(sourceModule)

    override fun ArgsBuilder.applyCommonArgs() {
        add("-target", targets.testTarget.name)
    }

    override fun applySpecificArgs(argsBuilder: ArgsBuilder) = with(argsBuilder) {
        add("-repo", home.librariesDir.path) // TODO: is this required?
        add("-output", expectedArtifact.path)
    }

    override fun applyDependencies(argsBuilder: ArgsBuilder) = with(argsBuilder) {
        addFlattened(dependencies.libraries) { library -> listOf("-l", library.path) }
    }

    override fun ArgsBuilder.applySources() {
        val defFile = sourceModule.files.single()
        check(defFile.location.name.endsWith(".def")) { defFile.location }
        add("-def", defFile.location.path)
    }
}

internal abstract class BasicCompilation<A : TestCompilationArtifact>(
    targets: KotlinNativeTargets,
    home: KotlinNativeHome,
    classLoader: KotlinNativeClassLoader,
    private val optimizationMode: OptimizationMode,
    freeCompilerArgs: TestCompilerArgs,
    dependencies: CategorizedDependencies,
    expectedArtifact: A
) : BasicBasicCompilation<A>(targets, home, classLoader, freeCompilerArgs, dependencies, expectedArtifact) {
    override val runner: CompilerToolRunner
        get() = CompilerRunner

    override fun ArgsBuilder.applyCommonArgs() {
        add("-target", targets.testTarget.name)
        optimizationMode.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        add(
            "-enable-assertions",
            "-Xskip-prerelease-check",
            "-Xverify-ir",
            "-Xbinary=runtimeAssertionsMode=panic"
        )
    }

    override fun ArgsBuilder.applySources() {
        // FIXME: cinterop modules probably shouldn't get there.
        addFlattenedTwice(sourceModules, { it.files }) { it.location.path }
    }

}

internal abstract class SourceBasedCompilation<A : TestCompilationArtifact>(
    targets: KotlinNativeTargets,
    home: KotlinNativeHome,
    classLoader: KotlinNativeClassLoader,
    optimizationMode: OptimizationMode,
    private val memoryModel: MemoryModel,
    private val threadStateChecker: ThreadStateChecker,
    private val gcType: GCType,
    private val gcScheduler: GCScheduler,
    freeCompilerArgs: TestCompilerArgs,
    override val sourceModules: Collection<TestModule>,
    dependencies: CategorizedDependencies,
    expectedArtifact: A
) : BasicCompilation<A>(
    targets = targets,
    home = home,
    classLoader = classLoader,
    optimizationMode = optimizationMode,
    freeCompilerArgs = freeCompilerArgs,
    dependencies = dependencies,
    expectedArtifact = expectedArtifact
) {
    override fun applySpecificArgs(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        add("-repo", home.librariesDir.path)
        memoryModel.compilerFlags?.let { compilerFlags -> add(compilerFlags) }
        threadStateChecker.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        gcType.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        gcScheduler.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
    }

    override fun applyDependencies(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        addFlattened(dependencies.libraries) { library -> listOf("-l", library.path) }
        dependencies.friends.takeIf(Collection<*>::isNotEmpty)?.let { friends ->
            add("-friend-modules", friends.joinToString(File.pathSeparator) { friend -> friend.path })
        }
        add(dependencies.includedLibraries) { include -> "-Xinclude=${include.path}" }
    }
}

internal class LibraryCompilation(
    settings: Settings,
    freeCompilerArgs: TestCompilerArgs,
    sourceModules: Collection<TestModule>,
    dependencies: Iterable<TestCompilationDependency<*>>,
    expectedArtifact: KLIB
) : SourceBasedCompilation<KLIB>(
    targets = settings.get(),
    home = settings.get(),
    classLoader = settings.get(),
    optimizationMode = settings.get(),
    memoryModel = settings.get(),
    threadStateChecker = settings.get(),
    gcType = settings.get(),
    gcScheduler = settings.get(),
    freeCompilerArgs = freeCompilerArgs,
    sourceModules = sourceModules,
    dependencies = CategorizedDependencies(dependencies),
    expectedArtifact = expectedArtifact
) {
    override fun applySpecificArgs(argsBuilder: ArgsBuilder) = with(argsBuilder) {
        add(
            "-produce", "library",
            "-output", expectedArtifact.path
        )
        super.applySpecificArgs(argsBuilder)
    }
}

internal class ExecutableCompilation(
    settings: Settings,
    freeCompilerArgs: TestCompilerArgs,
    sourceModules: Collection<TestModule>,
    private val extras: Extras,
    dependencies: Iterable<TestCompilationDependency<*>>,
    expectedArtifact: Executable
) : SourceBasedCompilation<Executable>(
    targets = settings.get(),
    home = settings.get(),
    classLoader = settings.get(),
    optimizationMode = settings.get(),
    memoryModel = settings.get(),
    threadStateChecker = settings.get(),
    gcType = settings.get(),
    gcScheduler = settings.get(),
    freeCompilerArgs = freeCompilerArgs,
    sourceModules = sourceModules,
    dependencies = CategorizedDependencies(dependencies),
    expectedArtifact = expectedArtifact
) {
    private val cacheMode: CacheMode = settings.get()

    override fun applySpecificArgs(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        add(
            "-produce", "program",
            "-output", expectedArtifact.path
        )
        when (extras) {
            is NoTestRunnerExtras -> add("-entry", extras.entryPoint)
            is WithTestRunnerExtras -> {
                val testDumpFile: File? = if (sourceModules.isEmpty()
                    && dependencies.includedLibraries.isNotEmpty()
                    && cacheMode.staticCacheRequiredForEveryLibrary
                ) {
                    // If there are no source modules passed to the compiler, but there is an included library with the static cache, then
                    // this should be two-stage test mode: Test functions are already stored in the included library, and they should
                    // already have been dumped during generation of library's static cache.
                    null // No, don't need to dump tests.
                } else {
                    expectedArtifact.testDumpFile // Yes, need to dump tests.

                }

                applyTestRunnerSpecificArgs(extras, testDumpFile)
            }
        }
        super.applySpecificArgs(argsBuilder)
    }

    override fun applyDependencies(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        super.applyDependencies(argsBuilder)
        cacheMode.staticCacheRootDir?.let { cacheRootDir -> add("-Xcache-directory=$cacheRootDir") }
        add(dependencies.uniqueCacheDirs) { libraryCacheDir -> "-Xcache-directory=${libraryCacheDir.path}" }
    }

    override fun postCompileCheck() {
        expectedArtifact.assertTestDumpFileNotEmptyIfExists()
    }

    companion object {
        internal fun ArgsBuilder.applyTestRunnerSpecificArgs(extras: WithTestRunnerExtras, testDumpFile: File?) {
            val testRunnerArg = when (extras.runnerType) {
                TestRunnerType.DEFAULT -> "-generate-test-runner"
                TestRunnerType.WORKER -> "-generate-worker-test-runner"
                TestRunnerType.NO_EXIT -> "-generate-no-exit-test-runner"
            }
            add(testRunnerArg)
            testDumpFile?.let { add("-Xdump-tests-to=$it") }
        }

        internal fun Executable.assertTestDumpFileNotEmptyIfExists() {
            if (testDumpFile.exists()) {
                testDumpFile.useLines { lines ->
                    assertTrue(lines.filter(String::isNotBlank).any()) { "Test dump file is empty: $testDumpFile" }
                }
            }
        }
    }
}

internal class StaticCacheCompilation(
    settings: Settings,
    freeCompilerArgs: TestCompilerArgs,
    private val options: Options,
    dependencies: Iterable<TestCompilationDependency<*>>,
    expectedArtifact: KLIBStaticCache
) : BasicCompilation<KLIBStaticCache>(
    targets = settings.get(),
    home = settings.get(),
    classLoader = settings.get(),
    optimizationMode = settings.get(),
    freeCompilerArgs = freeCompilerArgs,
    dependencies = CategorizedDependencies(dependencies),
    expectedArtifact = expectedArtifact
) {
    sealed interface Options {
        object Regular : Options
        class ForIncludedLibraryWithTests(val expectedExecutableArtifact: Executable, val extras: WithTestRunnerExtras) : Options
    }

    override val sourceModules get() = emptyList<TestModule>()

    private val cacheRootDir: File = run {
        val cacheMode = settings.get<CacheMode>()
        cacheMode.staticCacheRootDir ?: fail { "No cache root directory found for cache mode $cacheMode" }
    }

    override fun applySpecificArgs(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        add("-produce", "static_cache")

        when (options) {
            is Options.Regular -> Unit /* Nothing to do. */
            is Options.ForIncludedLibraryWithTests -> {
                applyTestRunnerSpecificArgs(options.extras, options.expectedExecutableArtifact.testDumpFile)
                add("-Xinclude=${dependencies.libraryToCache.path}")
            }
        }

        // The following line adds "-Xembed-bitcode-marker" for certain iOS device targets:
        add(home.properties.resolvablePropertyList("additionalCacheFlags", targets.testTarget.visibleName))
        add(
            "-Xadd-cache=${dependencies.libraryToCache.path}",
            "-Xcache-directory=${expectedArtifact.cacheDir.path}",
            "-Xcache-directory=$cacheRootDir"
        )
    }

    override fun applyDependencies(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        addFlattened(dependencies.cachedLibraries) { (_, library) -> listOf("-l", library.path) }
        add(dependencies.uniqueCacheDirs) { libraryCacheDir -> "-Xcache-directory=${libraryCacheDir.path}" }
    }

    override fun postCompileCheck() {
        (options as? Options.ForIncludedLibraryWithTests)?.expectedExecutableArtifact?.assertTestDumpFileNotEmptyIfExists()
    }
}

internal class CategorizedDependencies(uncategorizedDependencies: Iterable<TestCompilationDependency<*>>) {
    val failures: Set<TestCompilationResult.Failure> by lazy {
        uncategorizedDependencies.flatMapToSet { dependency ->
            when (val result = (dependency as? TestCompilation<*>)?.result) {
                is TestCompilationResult.Failure -> listOf(result)
                is TestCompilationResult.DependencyFailures -> result.causes
                is TestCompilationResult.Success -> emptyList()
                null -> emptyList()
            }
        }
    }

    val libraries: List<KLIB> by lazy { uncategorizedDependencies.collectArtifacts<KLIB, Library>() }
    val friends: List<KLIB> by lazy { uncategorizedDependencies.collectArtifacts<KLIB, FriendLibrary>() }
    val includedLibraries: List<KLIB> by lazy { uncategorizedDependencies.collectArtifacts<KLIB, IncludedLibrary>() }

    val cachedLibraries: List<KLIBStaticCache> by lazy { uncategorizedDependencies.collectArtifacts<KLIBStaticCache, LibraryStaticCache>() }

    val libraryToCache: KLIB by lazy {
        val libraries = uncategorizedDependencies.collectArtifacts<KLIB, TestCompilationDependencyType<KLIB>>()
        libraries.singleOrNull<KLIB>()
            ?: fail { "Only one library is expected as input for ${StaticCacheCompilation::class.java}, found: $libraries" }
    }

    val uniqueCacheDirs: Set<File> by lazy {
        cachedLibraries.mapToSet { (libraryCacheDir, _) -> libraryCacheDir } // Avoid repeating the same directory more than once.
    }

    private inline fun <reified A : TestCompilationArtifact, reified T : TestCompilationDependencyType<A>> Iterable<TestCompilationDependency<*>>.collectArtifacts(): List<A> {
        val concreteDependencyType = T::class.objectInstance
        val dependencyTypeMatcher: (TestCompilationDependencyType<*>) -> Boolean = if (concreteDependencyType != null) {
            { it == concreteDependencyType }
        } else {
            { it.canYield(A::class.java) }
        }

        return mapNotNull { dependency -> if (dependencyTypeMatcher(dependency.type)) dependency.artifact as A else null }
    }
}
