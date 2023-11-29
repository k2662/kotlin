/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator.util

fun <E> depthFirstSearch(root: E, getAdjacent: (E) -> Iterable<E>) = sequence<E> {
    val visited = hashSetOf<E>(root)
    val toYield = ArrayDeque<E>()
    toYield += root
    while (true) {
        val element = toYield.removeFirstOrNull()
            ?: break
        yield(element)
        for (next in getAdjacent(element)) {
            if (visited.add(next)) {
                toYield += next
            }
        }
    }
}