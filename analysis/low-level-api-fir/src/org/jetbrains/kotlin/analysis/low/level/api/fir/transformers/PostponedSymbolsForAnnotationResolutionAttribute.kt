/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

private object PostponedSymbolsForAnnotationResolutionKey : FirDeclarationDataKey()

internal var FirDeclaration.postponedSymbolsForAnnotationResolution: Collection<FirBasedSymbol<*>>?
        by FirDeclarationDataRegistry.data(PostponedSymbolsForAnnotationResolutionKey)
