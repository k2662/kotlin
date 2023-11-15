// FIR_IDENTICAL
// !RENDER_IR_DIAGNOSTICS_FULL_TEXT
// TARGET_BACKEND: JVM_IR

// FILE: Foo.java
public interface Foo {
    public static final long A = System.currentTimeMillis();
}

// FILE: test.kt
const val b = Foo.<!EVALUATION_ERROR!>A<!>
