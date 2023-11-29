// MODULE: m1-common
// FILE: common.kt

interface I

open class Base {
    open fun foo(): I = null!!
}

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect open class Foo<T : I> : Base {
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!><T : I> : Base() {
    override fun foo(): T = null!!
}
