/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.FrameworkCopy.Companion.dsymFile
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.mapToFile
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File
import java.io.IOException
import javax.inject.Inject

internal object AppleXcodeTasks {
    const val embedAndSignTaskPrefix = "embedAndSign"
    const val embedAndSignTaskPostfix = "AppleFrameworkForXcode"
    const val checkSandboxAndWriteProtection = "checkSandboxAndWriteProtection"
}

private object XcodeEnvironment {
    val buildType: NativeBuildType?
        get() {
            val configuration = System.getenv("CONFIGURATION") ?: return null

            fun String.toNativeBuildType() = when (this.toLowerCaseAsciiOnly()) {
                "debug" -> NativeBuildType.DEBUG
                "release" -> NativeBuildType.RELEASE
                else -> null
            }

            return configuration.toNativeBuildType()
                ?: System.getenv("KOTLIN_FRAMEWORK_BUILD_TYPE")?.toNativeBuildType()
        }


    val targets: List<KonanTarget>
        get() {
            val sdk = System.getenv("SDK_NAME") ?: return emptyList()
            val archs = System.getenv("ARCHS")?.split(" ") ?: return emptyList()
            return AppleSdk.defineNativeTargets(sdk, archs)
        }

    val frameworkSearchDir: File?
        get() {
            val configuration = System.getenv("CONFIGURATION") ?: return null
            val sdk = System.getenv("SDK_NAME") ?: return null
            return File(configuration, sdk)
        }

    val embeddedFrameworksDir: File?
        get() {
            val xcodeTargetBuildDir = System.getenv("TARGET_BUILD_DIR") ?: return null
            val xcodeFrameworksFolderPath = System.getenv("FRAMEWORKS_FOLDER_PATH") ?: return null
            return File(xcodeTargetBuildDir, xcodeFrameworksFolderPath).absoluteFile
        }

    val derivedFileDir: File?
        get() {
            val derivedFileDir = System.getenv("DERIVED_FILE_DIR") ?: return null
            return File(derivedFileDir).absoluteFile
        }

    val sign: String? get() = System.getenv("EXPANDED_CODE_SIGN_IDENTITY")

    val userScriptSandboxingEnabled: Boolean get() = System.getenv("ENABLE_USER_SCRIPT_SANDBOXING") == "YES"

    override fun toString() = """
        XcodeEnvironment:
          buildType=$buildType
          targets=$targets
          frameworkSearchDir=$frameworkSearchDir
          embeddedFrameworksDir=$embeddedFrameworksDir
          sign=$sign
    """.trimIndent()
}

private fun Project.registerAssembleAppleFrameworkTask(framework: Framework): TaskProvider<out Task>? {
    if (!framework.konanTarget.family.isAppleFamily || !framework.konanTarget.enabledOnCurrentHost) return null

    val envTargets = XcodeEnvironment.targets
    val needFatFramework = envTargets.size > 1

    val frameworkBuildType = framework.buildType
    val frameworkTarget = framework.target
    val isRequestedFramework = envTargets.contains(frameworkTarget.konanTarget)

    val frameworkTaskName = lowerCamelCaseName(
        "assemble",
        framework.namePrefix,
        frameworkBuildType.getName(),
        "AppleFrameworkForXcode",
        if (isRequestedFramework && needFatFramework) null else frameworkTarget.name //for fat framework we need common name
    )

    val envBuildType = XcodeEnvironment.buildType
    val envFrameworkSearchDir = XcodeEnvironment.frameworkSearchDir

    if (envBuildType == null || envTargets.isEmpty() || envFrameworkSearchDir == null) {
        val envConfiguration = System.getenv("CONFIGURATION")
        if (envTargets.isNotEmpty() && envConfiguration != null) {
            project.reportDiagnostic(KotlinToolingDiagnostics.UnknownAppleFrameworkBuildType(envConfiguration))
        } else {
            logger.debug("Not registering $frameworkTaskName, since not called from Xcode")
        }
        return null
    }

    return when {
        !isRequestedFramework -> locateOrRegisterTask<DefaultTask>(frameworkTaskName) { task ->
            task.description = "Packs $frameworkBuildType ${frameworkTarget.name} framework for Xcode"
            task.isEnabled = false
        }
        needFatFramework -> locateOrRegisterTask<FatFrameworkTask>(frameworkTaskName) { task ->
            task.description = "Packs $frameworkBuildType fat framework for Xcode"
            task.baseName = framework.baseName
            task.destinationDir = appleFrameworkDir(envFrameworkSearchDir)
            task.isEnabled = frameworkBuildType == envBuildType
        }.also {
            it.configure { task -> task.from(framework) }
        }
        else -> registerTask<FrameworkCopy>(frameworkTaskName) { task ->
            task.description = "Packs $frameworkBuildType ${frameworkTarget.name} framework for Xcode"
            task.isEnabled = frameworkBuildType == envBuildType
            task.sourceFramework.fileProvider(framework.linkTaskProvider.flatMap { it.outputFile })
            task.sourceDsym.fileProvider(dsymFile(task.sourceFramework.mapToFile()))
            task.dependsOn(framework.linkTaskProvider)
            task.destinationDirectory.set(appleFrameworkDir(envFrameworkSearchDir))
        }
    }
}

private fun fireEnvException(frameworkTaskName: String) {
    val envBuildType = XcodeEnvironment.buildType
    val envConfiguration = System.getenv("CONFIGURATION")
    if (envConfiguration != null && envBuildType == null) {
        throw IllegalStateException(
            "Unable to detect Kotlin framework build type for CONFIGURATION=$envConfiguration automatically. " +
                    "Specify 'KOTLIN_FRAMEWORK_BUILD_TYPE' to 'debug' or 'release'" +
                    "\n$XcodeEnvironment"
        )
    } else {
        throw IllegalStateException(
            "Please run the $frameworkTaskName task from Xcode " +
                    "('SDK_NAME', 'CONFIGURATION', 'TARGET_BUILD_DIR', 'ARCHS', 'DERIVED_FILE_DIR' and 'FRAMEWORKS_FOLDER_PATH' not provided)" +
                    "\n$XcodeEnvironment"
        )
    }
}

private fun fireSandboxException(frameworkTaskName: String) {
    val userScriptSandboxingEnabled = XcodeEnvironment.userScriptSandboxingEnabled
    val message = if (userScriptSandboxingEnabled) "You " else "DERIVED_FILE_DIR is not accessible, probably you "
    throw IllegalStateException(
        message +
                "have sandboxing for user scripts enabled." +
                "\nTo make the $frameworkTaskName task pass, disable this feature. " +
                "\nIn your Xcode project, navigate to \"Build Setting\", " +
                "and under \"Build Options\" set \"User script sandboxing\" (ENABLE_USER_SCRIPT_SANDBOXING) to \"NO\". " +
                "\nThen, run \"./gradlew --stop\" to stop the Gradle daemon" +
                "\nFor more information, see documentation: https://jb.gg/ltd9e6"
    )
}

private fun derivedFileDirAccessible(): Boolean {
    val userScriptSandboxingEnabled = XcodeEnvironment.userScriptSandboxingEnabled
    val derivedFileDir = XcodeEnvironment.derivedFileDir

    return if (derivedFileDir != null && userScriptSandboxingEnabled.not()) {
        try {
            val tempFile = File.createTempFile("sandbox", null, derivedFileDir)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            true
        } catch (e: IOException) {
            false
        }
    } else {
        false
    }
}

internal fun Project.registerEmbedAndSignAppleFrameworkTask(framework: Framework) {
    val envBuildType = XcodeEnvironment.buildType
    val envTargets = XcodeEnvironment.targets
    val envEmbeddedFrameworksDir = XcodeEnvironment.embeddedFrameworksDir
    val envFrameworkSearchDir = XcodeEnvironment.frameworkSearchDir
    val envSign = XcodeEnvironment.sign
    val userScriptSandboxingEnabled = XcodeEnvironment.userScriptSandboxingEnabled

    val frameworkTaskName = lowerCamelCaseName(
        AppleXcodeTasks.embedAndSignTaskPrefix,
        framework.namePrefix,
        AppleXcodeTasks.embedAndSignTaskPostfix
    )

    if (envBuildType == null || envTargets.isEmpty() || envEmbeddedFrameworksDir == null || envFrameworkSearchDir == null) {
        locateOrRegisterTask<DefaultTask>(frameworkTaskName) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Embed and sign ${framework.namePrefix} framework as requested by Xcode's environment variables"
            task.doFirst {
                fireEnvException(frameworkTaskName)
            }
        }
        return
    }

    val checkSandboxAndWriteProtectionTask = locateOrRegisterTask<DefaultTask>(AppleXcodeTasks.checkSandboxAndWriteProtection) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Check DERIVED_FILE_DIR accessible and ENABLE_USER_SCRIPT_SANDBOXING not enabled"
        task.doFirst {
            val dirAccessible = derivedFileDirAccessible()
            if (dirAccessible.not() || userScriptSandboxingEnabled) {
                fireSandboxException(frameworkTaskName)
            }
        }
    }

    val embedAndSignTask = locateOrRegisterTask<FrameworkCopy>(frameworkTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Embed and sign ${framework.namePrefix} framework as requested by Xcode's environment variables"
        task.isEnabled = !framework.isStatic
        task.inputs.apply {
            property("type", envBuildType)
            property("targets", envTargets)
            property("embeddedFrameworksDir", envEmbeddedFrameworksDir)
            property("userScriptSandboxingEnabled", userScriptSandboxingEnabled)
            if (envSign != null) {
                property("sign", envSign)
            }
        }
    }

    val assembleTask = registerAssembleAppleFrameworkTask(framework) ?: return
    if (framework.buildType != envBuildType || !envTargets.contains(framework.konanTarget)) return

    embedAndSignTask.configure { task ->
        val frameworkFile = framework.outputFile
        task.dependsOn(checkSandboxAndWriteProtectionTask)
        task.dependsOn(assembleTask)
        task.sourceFramework.set(File(appleFrameworkDir(envFrameworkSearchDir), frameworkFile.name))
        task.destinationDirectory.set(envEmbeddedFrameworksDir)
        if (envSign != null) {
            task.doLast {
                val binary = envEmbeddedFrameworksDir
                    .resolve(frameworkFile.name)
                    .resolve(frameworkFile.nameWithoutExtension)
                task.execOperations.exec {
                    it.commandLine("codesign", "--force", "--sign", envSign, "--", binary)
                }
            }
        }
    }
}

private val Framework.namePrefix: String
    get() = KotlinNativeBinaryContainer.extractPrefixFromBinaryName(
        name,
        buildType,
        outputKind.taskNameClassifier
    )

private fun Project.appleFrameworkDir(frameworkSearchDir: File) =
    buildDir.resolve("xcode-frameworks").resolve(frameworkSearchDir)

/**
 * macOS frameworks contain symlinks which are resolved/removed by the Gradle [Copy] task.
 * To preserve these symlinks we are using the `cp` command instead.
 * See https://youtrack.jetbrains.com/issue/KT-48594.
 */
@DisableCachingByDefault
internal abstract class FrameworkCopy : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    abstract val sourceFramework: DirectoryProperty

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFiles
    @get:Optional
    @get:IgnoreEmptyDirectories
    abstract val sourceDsym: DirectoryProperty

    @get:OutputDirectory
    abstract val destinationDirectory: DirectoryProperty

    @TaskAction
    open fun copy() {
        copy(sourceFramework)
        if (sourceDsym.isPresent && sourceDsym.getFile().exists()) {
            copy(sourceDsym)
        }
    }

    private fun copy(sourceProperty: DirectoryProperty) {
        val source = sourceProperty.getFile()
        val destination = destinationDirectory.getFile()

        val destinationFile = File(destination, source.name)
        if (destinationFile.exists()) {
            execOperations.exec { it.commandLine("rm", "-r", destinationFile.absolutePath) }
        }

        execOperations.exec { it.commandLine("cp", "-R", source.absolutePath, destination.absolutePath) }
    }

    companion object {
        fun dsymFile(framework: Provider<File>): Provider<File> = framework.map { File(it.path + ".dSYM") }
    }
}
