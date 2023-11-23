// ISSUE: KT-55252

class Buildee<T> {
    fun yield(arg: () -> T) {}
}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

fun box(): String {
    build {
        // K1/Native: exception during psi2ir (java.lang.NullPointerException @ org.jetbrains.kotlin.library.metadata.KlibModuleOriginKt.getKlibModuleOrigin)
        yield {}
    }
    return "OK"
}
