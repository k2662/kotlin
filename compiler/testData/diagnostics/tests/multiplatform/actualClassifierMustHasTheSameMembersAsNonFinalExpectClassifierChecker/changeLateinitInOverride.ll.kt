// MODULE: m1-common
// FILE: common.kt

open class Base {
    open var red1: String = ""
    open lateinit var red2: String
    open lateinit var green: String
}

expect open class Foo : Base {
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> : Base() {
    override lateinit var red1: String
    override var red2: String = ""
    override lateinit var green: String
}
