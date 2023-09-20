/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.cli.jvm.compiler.intellijCore


import com.intellij.ide.plugins.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.util.plugins.DataLoader
import com.intellij.platform.util.plugins.LocalFsDataLoader
import com.intellij.util.lang.ZipFilePool
import java.io.Closeable
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipFile
import javax.xml.stream.XMLStreamException

private val LOG: Logger
    get() = PluginManagerCore.getLogger()

internal fun loadForCoreEnv(pluginRoot: Path, fileName: String): IdeaPluginDescriptorImpl? {
    val pathResolver = PluginXmlPathResolver.DEFAULT_PATH_RESOLVER
    val parentContext = DescriptorListLoadingContext(disabledPlugins = emptySet())
    if (Files.isDirectory(pluginRoot)) {
        return loadDescriptorFromDir(file = pluginRoot,
                                     descriptorRelativePath = "${PluginManagerCore.META_INF}$fileName",
                                     pluginPath = null,
                                     context = parentContext,
                                     isBundled = true,
                                     isEssential = true,
                                     pathResolver = pathResolver,
                                     useCoreClassLoader = false)
    }
    else {
        return loadDescriptorFromJar(file = pluginRoot,
                                     fileName = fileName,
                                     pathResolver = pathResolver,
                                     parentContext = parentContext,
                                     isBundled = true,
                                     isEssential = true,
                                     pluginPath = null,
                                     useCoreClassLoader = false)
    }
}

private fun loadDescriptorFromDir(file: Path,
                                  descriptorRelativePath: String,
                                  pluginPath: Path?,
                                  context: DescriptorListLoadingContext,
                                  isBundled: Boolean,
                                  isEssential: Boolean,
                                  useCoreClassLoader: Boolean,
                                  pathResolver: PathResolver
): IdeaPluginDescriptorImpl? {
    try {
        val input = Files.readAllBytes(file.resolve(descriptorRelativePath))
        val dataLoader = LocalFsDataLoader(file)
        val raw = readModuleDescriptor(input = input,
                                       readContext = context,
                                       pathResolver = pathResolver,
                                       dataLoader = dataLoader,
                                       includeBase = null,
                                       readInto = null,
                                       locationSource = file.toString())
        val descriptor = IdeaPluginDescriptorImpl(raw = raw, path = pluginPath ?: file, isBundled = isBundled, id = null, moduleName = null,
                                                  useCoreClassLoader = useCoreClassLoader)
        descriptor.readExternal(raw = raw, pathResolver = pathResolver, context = context, isSub = false, dataLoader = dataLoader)
        descriptor.jarFiles = Collections.singletonList(file)
        return descriptor
    }
    catch (e: NoSuchFileException) {
        return null
    }
    catch (e: Throwable) {
        if (isEssential) {
            throw e
        }
        LOG.warn("Cannot load ${file.resolve(descriptorRelativePath)}", e)
        return null
    }
}

private fun loadDescriptorFromJar(file: Path,
                                  fileName: String,
                                  pathResolver: PathResolver,
                                  parentContext: DescriptorListLoadingContext,
                                  isBundled: Boolean,
                                  isEssential: Boolean,
                                  useCoreClassLoader: Boolean,
                                  pluginPath: Path?): IdeaPluginDescriptorImpl? {
    var closeable: Closeable? = null
    try {
        val dataLoader: DataLoader
        val pool = ZipFilePool.POOL
        if (pool == null || parentContext.transient) {
            val zipFile = ZipFile(file.toFile(), StandardCharsets.UTF_8)
            closeable = zipFile
            dataLoader = JavaZipFileDataLoader(zipFile)
        }
        else {
            @Suppress("UnstableApiUsage")
            dataLoader = ImmutableZipFileDataLoader(pool.load(file), file, pool)
        }

        val raw = readModuleDescriptor(input = dataLoader.load("META-INF/$fileName") ?: return null,
                                       readContext = parentContext,
                                       pathResolver = pathResolver,
                                       dataLoader = dataLoader,
                                       includeBase = null,
                                       readInto = null,
                                       locationSource = file.toString())

        val descriptor = IdeaPluginDescriptorImpl(raw = raw, path = pluginPath ?: file, isBundled = isBundled, id = null, moduleName = null,
                                                  useCoreClassLoader = useCoreClassLoader)
        descriptor.readExternal(raw = raw, pathResolver = pathResolver, context = parentContext, isSub = false, dataLoader = dataLoader)
        descriptor.jarFiles = Collections.singletonList(descriptor.pluginPath)
        return descriptor
    }
    catch (e: Throwable) {
        if (isEssential) {
            throw if (e is XMLStreamException) RuntimeException("Cannot read $file", e) else e
        }
        parentContext.result.reportCannotLoad(file, e)
    }
    finally {
        closeable?.close()
    }
    return null
}

private class JavaZipFileDataLoader(private val file: ZipFile) : DataLoader {
    override val pool: ZipFilePool?
        get() = null

    override fun load(path: String): InputStream? {
        val entry = file.getEntry(if (path[0] == '/') path.substring(1) else path) ?: return null
        return file.getInputStream(entry)
    }

    override fun toString() = file.toString()
}

private class ImmutableZipFileDataLoader(private val resolver: ZipFilePool.EntryResolver,
                                         private val zipPath: Path,
                                         override val pool: ZipFilePool) : DataLoader {
    override fun load(path: String): InputStream? {
        // well, path maybe specified as `/META-INF/*` in plugin descriptor, and
        // it is our responsibility to normalize path for ImmutableZipFile API
        // do not use kotlin stdlib here
        return resolver.loadZipEntry(if (path[0] == '/') path.substring(1) else path)
    }

    // yes, by identity - ImmutableZipFileDataLoader is created for the same Path object from plugin JARs list
    override fun isExcludedFromSubSearch(jarFile: Path) = jarFile === zipPath

    override fun toString() = resolver.toString()
}
