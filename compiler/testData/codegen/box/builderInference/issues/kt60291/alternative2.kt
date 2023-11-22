// ISSUE: KT-60291
// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

class Buildee<T> {
    fun yield(value: T) {}
}

@OptIn(ExperimentalTypeInference::class)
fun <T> build(@BuilderInference block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class TargetType

fun box(): String {
    // K2/JVM: java.lang.IllegalStateException: Cannot serialize error type: ERROR CLASS: Cannot infer argument for type parameter T
    // K2/Native & K2/WASM & K2/JS: org.jetbrains.kotlin.backend.common.linkage.issues.IrDisallowedErrorNode: Class found but error nodes are not allowed.
    when ("") {
        "true" -> build { yield(TargetType()) }
        "false" -> build {}
        else -> Buildee()
    }
    return "OK"
}
