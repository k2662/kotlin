// ISSUE: KT-43710
// WITH_STDLIB

class Buildee<T> {
    fun yield(arg: T) {}
}

fun <T> Buildee<T>.any(predicate: (T) -> Boolean) {}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

fun box(): String {
    build {
        yield("")
        // K1&K2: OVERLOAD_RESOLUTION_AMBIGUITY
        // K1: OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES, STUB_TYPE_IN_RECEIVER_CAUSES_AMBIGUITY
        any { it.isEmpty() } // K1: TYPE_MISMATCH (expected Boolean, actual Unit)
    }
    return "OK"
}
