// WITH_STDLIB
// IGNORE_BACKEND_K2: NATIVE

import kotlin.experimental.ExperimentalTypeInference

operator fun <T> SequenceScope<String>.plusAssign(x: SequenceScope<T>) {}

@OptIn(ExperimentalTypeInference::class)

fun <T> mySequence(block: suspend SequenceScope<T>.() -> Unit): Sequence<T> = Sequence { iterator(block) }

fun main() {
    val y: Sequence<String> = mySequence {
        yield("result")
        this += this
    }
}

fun box(): String {
    main()
    return "OK"
}
