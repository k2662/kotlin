// ISSUE: KT-57834

class Buildee<T: Any> {
    private lateinit var data: T
    fun yield(arg: T) { data = arg }
    fun materialize(): T = data
}

fun <T: Any> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class TargetType
class DifferentType

fun consume(x: TargetType) {}
fun consume(x: DifferentType) {}

fun box(): String {
    build {
        yield(TargetType())
        // K1&K2: OVERLOAD_RESOLUTION_AMBIGUITY
        // K1: OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES, STUB_TYPE_IN_ARGUMENT_CAUSES_AMBIGUITY
        consume(materialize())
    }
    return "OK"
}
