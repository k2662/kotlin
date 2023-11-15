// FIR_IDENTICAL
// !RENDER_IR_DIAGNOSTICS_FULL_TEXT
// TARGET_BACKEND: JVM_IR

// FILE: A.java

public class A {
    public static final int X = 1;
    public static final int Y;

    public final int z = 3;

    static {
        Y = 2;
    }
}

// FILE: main.kt

annotation class Ann(val x: Int)

@Ann(A.X)
fun main1() {}

@Ann(A.<!EVALUATION_ERROR!>Y<!>)
fun main2() {}

val q = A()
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>q.<!EVALUATION_ERROR!>z<!><!>)
fun main3() {}

