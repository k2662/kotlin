/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.name.WasmStandardClassIds

object FirWasmWasiExternalDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.symbol.isEffectivelyExternal(context.session)) return

        if (declaration is FirSimpleFunction) {
            if (!context.isTopLevel) {
                reporter.reportOn(declaration.source, FirWasmErrors.WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION, context)
            } else {
                if (!declaration.hasAnnotation(WasmStandardClassIds.Annotations.WasmImport, context.session)) {
                    reporter.reportOn(declaration.source, FirWasmErrors.WASI_EXTERNAL_FUNCTION_WITHOUT_IMPORT, context)
                }
            }
        }
    }
}