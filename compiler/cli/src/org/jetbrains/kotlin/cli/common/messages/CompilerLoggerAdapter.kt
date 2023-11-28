/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.util.Logger

/**
 * An adapter for the [Logger] interface that reports all messages to compiler's [MessageCollector].
 *
 * @property messageCollector - The destination for the messages.
 * @property warningsAsErrors - Whether to treat warnings as errors. Can be used in tests.
 */
private class CompilerLoggerAdapter(
    private val messageCollector: MessageCollector,
    private val warningsAsErrors: Boolean
) : Logger {
    override fun log(message: String) = messageCollector.report(LOGGING, message, null)
    override fun warning(message: String) = messageCollector.report(if (warningsAsErrors) ERROR else STRONG_WARNING, message, null)
    override fun error(message: String) = messageCollector.report(ERROR, message, null)

    override fun fatal(message: String): Nothing {
        messageCollector.report(ERROR, message, null)
        (messageCollector as? GroupingMessageCollector)?.flush()
        throw CompilationErrorException()
    }
}

fun MessageCollector.toLogger(forTests: Boolean = false): Logger =
    CompilerLoggerAdapter(this, warningsAsErrors = forTests)

fun CompilerConfiguration.getLogger(forTests: Boolean = false): Logger =
    getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).toLogger(forTests)
