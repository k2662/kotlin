// ISSUE: KT-57709

class Buildee<T: Any> {
    private lateinit var data: T
    fun yield(arg: T) { data = arg }
    fun materialize(): T = data
}

fun <T: Any> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class DifferentType

fun Buildee<Any>.extension() {}
fun Buildee<DifferentType>.extension() {}

fun box(): String {
    build<Any> {
        yield(Any())
        extension()
    }
    build {
        yield(Any())
        // K1&K2: OVERLOAD_RESOLUTION_AMBIGUITY
        // K1: OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES, STUB_TYPE_IN_ARGUMENT_CAUSES_AMBIGUITY
        extension()
    }
    return "OK"
}
