/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.backend.common.actualizer.IrActualizedResult
import org.jetbrains.kotlin.backend.common.actualizer.IrActualizer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.KotlinMangler

data class FirResult(val outputs: List<ModuleCompilerAnalyzedOutput>)

data class ModuleCompilerAnalyzedOutput(
    val session: FirSession,
    val scopeSession: ScopeSession,
    val fir: List<FirFile>
)

data class Fir2IrActualizedResult(
    val irModuleFragment: IrModuleFragment,
    val components: Fir2IrComponents,
    val pluginContext: Fir2IrPluginContext,
    val irActualizedResult: IrActualizedResult?,
)

fun signatureComposerForJvmFir2Ir(generateSignatures: Boolean): IdSignatureComposer {
    val mangler = JvmDescriptorMangler(null)
    return if (generateSignatures) {
        JvmIdSignatureDescriptor(mangler)
    } else {
        DescriptorSignatureComposerStub(mangler)
    }
}

fun FirResult.convertToIrAndActualize(
    fir2IrExtensions: Fir2IrExtensions,
    fir2IrConfiguration: Fir2IrConfiguration,
    irGeneratorExtensions: Collection<IrGenerationExtension>,
    signatureComposer: IdSignatureComposer,
    irMangler: KotlinMangler.IrMangler,
    firMangler: FirMangler,
    visibilityConverter: Fir2IrVisibilityConverter,
    kotlinBuiltIns: KotlinBuiltIns,
    actualizerTypeContextProvider: (IrBuiltIns) -> IrTypeSystemContext,
    fir2IrResultPostCompute: (ModuleCompilerAnalyzedOutput, Fir2IrResult) -> Unit = { _, _ -> },
): Fir2IrActualizedResult {
    val fir2IrResult: Fir2IrResult
    val actualizationResult: IrActualizedResult?

    val commonMemberStorage = Fir2IrCommonMemberStorage(signatureComposer, firMangler)

    when (outputs.size) {
        0 -> error("No modules found")
        1 -> {
            val output = outputs.single()
            fir2IrResult = convertToIr(
                output,
                fir2IrExtensions,
                fir2IrConfiguration,
                commonMemberStorage = commonMemberStorage,
                irBuiltIns = null,
                irMangler,
                visibilityConverter,
                kotlinBuiltIns,
                actualizerTypeContextProvider,
            ).also { result ->
                fir2IrResultPostCompute(output, result)
            }
            actualizationResult = null
        }
        else -> {
            val platformOutput = outputs.last()
            val commonOutputs = outputs.dropLast(1)
            var irBuiltIns: IrBuiltInsOverFir? = null
            val commonIrOutputs = commonOutputs.map {
                convertToIr(
                    it,
                    // We need to build all modules before rebuilding fake overrides
                    // to avoid fixing declaration storages
                    fir2IrExtensions,
                    fir2IrConfiguration.copy(useIrFakeOverrideBuilder = false),
                    commonMemberStorage = commonMemberStorage,
                    irBuiltIns = irBuiltIns,
                    irMangler,
                    visibilityConverter,
                    kotlinBuiltIns,
                    actualizerTypeContextProvider,
                ).also { result ->
                    fir2IrResultPostCompute(it, result)
                    if (irBuiltIns == null) {
                        irBuiltIns = result.components.irBuiltIns
                    }
                }
            }
            fir2IrResult = convertToIr(
                platformOutput,
                fir2IrExtensions,
                fir2IrConfiguration,
                commonMemberStorage = commonMemberStorage,
                irBuiltIns = irBuiltIns!!,
                irMangler,
                visibilityConverter,
                kotlinBuiltIns,
                actualizerTypeContextProvider,
            ).also {
                fir2IrResultPostCompute(platformOutput, it)
            }

            actualizationResult = IrActualizer.actualize(
                fir2IrResult.irModuleFragment,
                commonIrOutputs.map { it.irModuleFragment },
                KtDiagnosticReporterWithImplicitIrBasedContext(
                    fir2IrConfiguration.diagnosticReporter,
                    fir2IrConfiguration.languageVersionSettings
                ),
                actualizerTypeContextProvider(fir2IrResult.irModuleFragment.irBuiltins),
                commonMemberStorage.symbolTable,
                fir2IrResult.components.fakeOverrideBuilder,
                fir2IrConfiguration.useIrFakeOverrideBuilder,
                fir2IrConfiguration.expectActualTracker,
            )
        }
    }

    val (irModuleFragment, components, pluginContext) = fir2IrResult
    pluginContext.applyIrGenerationExtensions(irModuleFragment, irGeneratorExtensions)
    return Fir2IrActualizedResult(irModuleFragment, components, pluginContext, actualizationResult)
}

private fun convertToIr(
    firOutput: ModuleCompilerAnalyzedOutput,
    fir2IrExtensions: Fir2IrExtensions,
    fir2IrConfiguration: Fir2IrConfiguration,
    commonMemberStorage: Fir2IrCommonMemberStorage,
    irBuiltIns: IrBuiltInsOverFir?,
    irMangler: KotlinMangler.IrMangler,
    visibilityConverter: Fir2IrVisibilityConverter,
    kotlinBuiltIns: KotlinBuiltIns,
    typeContextProvider: (IrBuiltIns) -> IrTypeSystemContext,
): Fir2IrResult {
    return Fir2IrConverter.createIrModuleFragment(
        firOutput.session, firOutput.scopeSession, firOutput.fir,
        fir2IrExtensions, fir2IrConfiguration,
        irMangler, IrFactoryImpl, visibilityConverter,
        Fir2IrJvmSpecialAnnotationSymbolProvider(), // TODO KT-60526: replace with appropriate (probably empty) implementation for other backends.
        kotlinBuiltIns = kotlinBuiltIns,
        commonMemberStorage = commonMemberStorage,
        initializedIrBuiltIns = irBuiltIns,
        typeContextProvider = typeContextProvider
    )
}

fun IrPluginContext.applyIrGenerationExtensions(irModuleFragment: IrModuleFragment, irGenerationExtensions: Collection<IrGenerationExtension>) {
    if (irGenerationExtensions.isEmpty()) return
    for (extension in irGenerationExtensions) {
        extension.generate(irModuleFragment, this)
    }
}
