package vk.core.api;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * A simple set of tests for the compiler. In cases where we expect erorrors, we
 * only care that the compiler actually reports that there are errors. We do not
 * care about the right number or kind of messages.
 *
 */
public class CompilerTests {

    CompilationUnit validClassFoo = new CompilationUnit("Foo", "public class Foo {}", false);
    CompilationUnit anotherValidClassFoo = new CompilationUnit("Foo",
            "public class Foo { \n           \\a comment \n                  int x = 0; }",
            false);
    CompilationUnit validClassBar = new CompilationUnit("Bar", "public class Bar { \n"
            + " public static int fourtyTwo() { \n"
            + "    return 41 + 1; \n"
            + " }\n"
            + "}", false);

    CompilationUnit validClassFooUsingBar = new CompilationUnit("Foo", "public class Foo { \n"
            + " public static void print() { \n"
            + "    System.out.println(Bar.fourtyTwo()); \n"
            + " }\n"
            + "}", false);

    CompilationUnit inValidClassFooNameNotMatching = new CompilationUnit("FooX", "public class FooY {}", false);
    CompilationUnit inValidClassFooSyntaxError = new CompilationUnit("Foo", "public class Foo }", false);
    CompilationUnit inValidClassFooUsingNonExistingClass = new CompilationUnit("Foo", "public class Foo { \n"
            + " public static void print() { \n"
            + "    System.out.println(Baz.one()); \n"
            + " }\n"
            + "}", false);

    CompilationUnit testClassForBar = new CompilationUnit("BarTest",
            "import static org.junit.Assert.*;\n"
                    + "import org.junit.Test;\n"
                    + "public class BarTest { \n"
                    + "@Test\n"
                    + "public void leTest() { \n "
                    + "assertEquals(42, Bar.fourtyTwo()); \n"
                    + "   }\n "
                    + "}",
            true);

    CompilationUnit ticket5Class = new CompilationUnit("Foo", "public class Foo {  public static void foo(int bar){} }",
            false);
    CompilationUnit ticket5Test = new CompilationUnit("FooTest",
            "import org.junit.Test; \n public class FooTest { \n  @Test\n    public void testFoo(){ Foo.foo(\"bar\"); }}",
            true);

    @Test
    public void callingAWrongSignature_shouldProduceCompileErrors_Ticket5() {
        JavaStringCompiler compiler = CompilerFactory.getCompiler(ticket5Class, ticket5Test);
        compiler.compileAndRunTests();
        CompilerResult compilerResult = compiler.getCompilerResult();
        assertTrue(compilerResult.hasCompileErrors());
    }

    @Test
    public void compilingAValidClass_shouldNotProduceCompileErrors1() {
        JavaStringCompiler compiler = CompilerFactory.getCompiler(validClassFoo);
        compiler.compileAndRunTests();
        CompilerResult compilerResult = compiler.getCompilerResult();
        assertFalse(TestHelpers.getErrorMessages(compiler, compilerResult), compilerResult.hasCompileErrors());
    }

    @Test
    public void compilingAValidClass_shouldNotProduceCompileErrors2() {
        JavaStringCompiler compiler = CompilerFactory.getCompiler(validClassBar);
        compiler.compileAndRunTests();
        CompilerResult compilerResult = compiler.getCompilerResult();
        assertFalse(TestHelpers.getErrorMessages(compiler, compilerResult), compilerResult.hasCompileErrors());
    }

    @Test
    public void compilingValidClassesWithDependency_shouldNotProduceCompileErrors() {
        JavaStringCompiler compiler = CompilerFactory.getCompiler(validClassFooUsingBar, validClassBar);
        compiler.compileAndRunTests();
        CompilerResult compilerResult = compiler.getCompilerResult();
        assertFalse(TestHelpers.getErrorMessages(compiler, compilerResult), compilerResult.hasCompileErrors());
    }

    @Test
    public void compilingAClassWhereNamesDoNotMatch_shouldProduceCompileErrors() {
        JavaStringCompiler compiler = CompilerFactory.getCompiler(inValidClassFooNameNotMatching);
        compiler.compileAndRunTests();
        CompilerResult compilerResult = compiler.getCompilerResult();
        assertTrue(compilerResult.hasCompileErrors());
    }

    @Test
    public void compilingAClassWithSyntaxError_shouldProduceCompileErrors() {
        JavaStringCompiler compiler = CompilerFactory.getCompiler(inValidClassFooSyntaxError);
        compiler.compileAndRunTests();
        CompilerResult compilerResult = compiler.getCompilerResult();
        assertTrue(compilerResult.hasCompileErrors());
    }

    @Test
    public void compilingAClassWithMissingDependency_shouldProduceCompileErrors() {
        JavaStringCompiler compiler = CompilerFactory.getCompiler(inValidClassFooSyntaxError);
        compiler.compileAndRunTests();
        CompilerResult compilerResult = compiler.getCompilerResult();
        assertTrue(compilerResult.hasCompileErrors());
    }

    @Test
    public void compilingATestClassWithMissingDependency_shouldProduceCompileErrors() {
        JavaStringCompiler compiler = CompilerFactory.getCompiler(testClassForBar);
        compiler.compileAndRunTests();
        CompilerResult compilerResult = compiler.getCompilerResult();
        assertTrue(compilerResult.hasCompileErrors());
    }

    @Test
    public void compilingATestClassWithDependency_shouldNotProduceCompileErrors() {
        JavaStringCompiler compiler = CompilerFactory.getCompiler(testClassForBar, validClassBar);
        compiler.compileAndRunTests();
        CompilerResult compilerResult = compiler.getCompilerResult();
        assertFalse(TestHelpers.getErrorMessages(compiler, compilerResult), compilerResult.hasCompileErrors());
    }

    @Test(expected = IllegalArgumentException.class)
    public void compilingAClassesWithNameConflicts_shouldThrowAnExeption() {
        JavaStringCompiler compiler = CompilerFactory.getCompiler(validClassFoo, validClassBar, anotherValidClassFoo);
        compiler.compileAndRunTests();
        fail("We should never reach this statement");
    }

}
