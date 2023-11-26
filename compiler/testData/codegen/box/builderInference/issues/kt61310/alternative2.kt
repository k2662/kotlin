// ISSUE: KT-61310

class Buildee<CPTV> {
    fun yield(value: CPTV) {}
}

fun <FPTV> build(block: Buildee<FPTV>.() -> Unit): Buildee<FPTV> {
    return Buildee<FPTV>().apply(block)
}

class TargetType<T>(val value: T = Placeholder() as T)
class Placeholder
fun <T> materializeTargetType(): TargetType<T> = TargetType()

fun box(): String {
    val arg = TargetType<Placeholder>()
    // K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER (TypeVariable(FPTV))
    build {
        if (true) {
            yield(arg)
        } else {
            // K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER (TypeVariable(T))
            yield(TargetType())
        }
    }
    return "OK"
}
