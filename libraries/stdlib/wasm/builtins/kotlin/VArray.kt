/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package kotlin

class VArray<T> {
    public operator fun get(index: Int): T = TODO()
    public operator fun set(index: Int, value: T): Unit {}

    public constructor(size: Int, init: (Int) -> T)

    public val size: Int get() = TODO()
}
