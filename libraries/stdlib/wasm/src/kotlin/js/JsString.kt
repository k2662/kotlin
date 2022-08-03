/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.JsPrimitive
import kotlin.wasm.internal.reftypes.stringref

///** JavaScript primitive string */
@JsPrimitive("string")
public external class JsString internal constructor() : JsAny

private fun unsafeCastToJsString(@Suppress("UNUSED_PARAMETER") ref: stringref): JsString = js("ref")

public fun String.toJsString(): JsString =
    unsafeCastToJsString(reference)