/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.InterfaceAndAbstractClassConfigurator
import org.jetbrains.kotlin.generators.tree.addPureAbstractElement
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil.collectPreviouslyGeneratedFiles
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil.removeExtraFilesFromPreviousGeneration
import org.jetbrains.kotlin.ir.generator.model.markLeaves
import org.jetbrains.kotlin.ir.generator.print.*
import java.io.File

const val BASE_PACKAGE = "org.jetbrains.kotlin.ir"

internal const val TREE_GENERATOR_README = "compiler/ir/ir.tree/tree-generator/ReadMe.md"

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("compiler/ir/ir.tree/gen").canonicalFile

    val model = IrTree.build()

    InterfaceAndAbstractClassConfigurator(model.elements).configureInterfacesAndAbstractClasses()
    addPureAbstractElement(model.elements, elementBaseType)
    markLeaves(model.elements)

    val previouslyGeneratedFiles = collectPreviouslyGeneratedFiles(generationPath)
    val generatedFiles = sequence {
        yieldAll(printElements(generationPath, model))
        yield(printVisitor(generationPath, model))
        yield(printVisitorVoid(generationPath, model))
        yield(printTransformer(generationPath, model))
        yield(printTransformerVoid(generationPath, model))
        yield(printTypeVisitor(generationPath, model))
        yield(printFactory(generationPath, model))
    }.map {
        GeneratorsFileUtil.writeFileIfContentChanged(it.file, it.newText, logNotChanged = false)
        it.file
    }.toList()
    removeExtraFilesFromPreviousGeneration(previouslyGeneratedFiles, generatedFiles)
}
