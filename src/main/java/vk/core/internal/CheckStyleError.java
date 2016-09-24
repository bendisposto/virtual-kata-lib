package vk.core.internal;

import vk.core.api.CompilationUnit;
import vk.core.api.CompileError;

public class CheckStyleError implements CompileError {

    private final int line;
    private final int column;
    private final String message;
    private final CompilationUnit compilationUnit;

    public CheckStyleError(int line, int column, String message, CompilationUnit compilationUnit) {
        this.line = line;
        this.column = column;
        this.message = message;
        this.compilationUnit = compilationUnit;
    }

    @Override
    public long getLineNumber() {
        return line;
    }

    @Override
    public long getColumnNumber() {
        return column;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getCodeLineContainingTheError() {
        return "";
    }

    @Override
    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

}
