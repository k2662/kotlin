// IGNORE_BACKEND_K2: WASM

interface A
open class B : A
open class C : A

abstract class X {
    fun <S : A> foo(s: S): String = when (s) {
        is B -> foo(s)
        is C -> foo(s)
        else -> throw AssertionError(s)
    }

    abstract fun <S : B> foo(s: S): String
    abstract fun <S : C> foo(s: S): String
}

class Y : X() {
    override fun <S : B> foo(s: S): String = "O"
    override fun <S : C> foo(s: S): String = "K"
}

fun box(): String = Y().foo(B()) + Y().foo(C())
