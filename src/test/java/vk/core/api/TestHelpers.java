package vk.core.api;

import java.util.Collection;

public class TestHelpers {

    /**
     * @param compiler
     * @param compilerResult
     * @return String containing all error messages, used if we do not expect
     *         Errors but the compiler found some. Very useful during when
     *         trying to identify reason for a failed test.
     */
    public static String getErrorMessages(JavaStringCompiler compiler, CompilerResult compilerResult) {

        Collection<CompileError> errors = compilerResult.getCompilerErrors();
        StringBuilder sb = new StringBuilder();

        for (CompileError error : errors) {
            sb.append(error.toString());
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

}
