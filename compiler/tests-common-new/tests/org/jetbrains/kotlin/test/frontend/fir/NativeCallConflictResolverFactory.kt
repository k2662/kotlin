/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.jvm.ConeEquivalentCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

object NativeCallConflictResolverFactory : ConeCallConflictResolverFactory() {
    override fun create(
        typeSpecificityComparator: TypeSpecificityComparator,
        components: InferenceComponents,
        transformerComponents: BodyResolveComponents
    ): ConeCompositeConflictResolver {
        val specificityComparator = TypeSpecificityComparator.NONE
        return ConeCompositeConflictResolver(
            ConeOverloadConflictResolver(specificityComparator, components, transformerComponents),
            FilteringOutOriginalInPresenceOfSmartCastConeCallConflictResolver,
            ConeEquivalentCallConflictResolver(specificityComparator, components, transformerComponents),
            ConeIntegerOperatorConflictResolver(specificityComparator, components, transformerComponents)
        )
    }
}
