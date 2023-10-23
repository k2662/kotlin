/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.bir.BirBuiltIns
import org.jetbrains.kotlin.bir.BirElementDynamicPropertyManager
import org.jetbrains.kotlin.bir.BirForest
import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.types.BirTypeSystemContext
import org.jetbrains.kotlin.bir.types.BirTypeSystemContextImpl
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.name.FqName

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JvmBirBackendContext @OptIn(ObsoleteDescriptorBasedAPI::class) constructor(
    irContext: JvmBackendContext,
    module: ModuleDescriptor,
    compiledBir: BirForest,
    ir2BirConverter: Ir2BirConverter,
    dynamicPropertyManager: BirElementDynamicPropertyManager,
    phaseConfig: List<(JvmBirBackendContext) -> BirLoweringPhase>,
) : BirBackendContext(compiledBir, dynamicPropertyManager) {
    override val configuration: CompilerConfiguration = irContext.configuration
    override val builtIns = irContext.builtIns

    override val birBuiltIns: BirBuiltIns = BirBuiltIns(irContext.irBuiltIns, ir2BirConverter)
    override val builtInSymbols: JvmBirBuiltInSymbols = JvmBirBuiltInSymbols(irContext.ir.symbols, ir2BirConverter)
    override val typeSystem: BirTypeSystemContext = BirTypeSystemContextImpl(birBuiltIns)

    val generationState: GenerationState = irContext.state

    val loweringPhases = phaseConfig.map { it(this) }

    override val internalPackageFqn = FqName("kotlin.jvm")
    override val sharedVariablesManager: SharedVariablesManager
        get() = TODO("Not yet implemented")
}