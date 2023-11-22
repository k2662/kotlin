// ISSUE: KT-52838

open class Buildee<A, B> {
    fun materializeA(): A = null as A
    fun materializeB(): B = null as B
}

class DerivedBuildee<A, B>: Buildee<A, B>()

fun <A, B> build(block: Buildee<A, B>.() -> Unit): Buildee<A, B> {
    return DerivedBuildee<A, B>().apply(block)
}

fun box(): String {
    // K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    build {
        this as DerivedBuildee<*, *>
        materializeA()
        materializeB() // K1: RECEIVER_TYPE_MISMATCH (expected CapturedType(*), actual CapturedType(*))
    }
    return "OK"
}
