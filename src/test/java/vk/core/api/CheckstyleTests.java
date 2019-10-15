package vk.core.api;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A simple set of tests for style checking. In cases where we expect errors, we
 * only care that the compiler actually reports that there are errors. We do not
 * care about the right number or kind of messages.
 *
 */
public class CheckstyleTests {

    CompilationUnit indentationErrorClass = new CompilationUnit("Bar", "public class Bar { \n"
            + "    public static int fourtyTwo() { \n"
            + "          System.out.println(-1);  "
            + "        return 41 + 1; \n"
            + "    }\n"
            + "}", false);


    @Test
    public void aClassWithWrongIndentation_shouldProduceAnError() {
        JavaStringCompiler compiler = CompilerFactory.getCompiler(indentationErrorClass);
        compiler.compileAndRunTests();
        CompilerResult compilerResult = compiler.getCompilerResult();
        assertTrue(compilerResult.hasCompileErrors());
    }


}
