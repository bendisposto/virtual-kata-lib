package vk.core.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import vk.core.api.CompilationUnit;
import vk.core.api.CompileError;
import vk.core.api.CompilerResult;
import vk.core.api.TestFailure;
import vk.core.api.TestResult;

public class InternalResult implements CompilerResult, TestResult {

    private final Collection<CompileError> errors = new ArrayList<>();
    private final Collection<CompileError> otherProblems = new ArrayList<>();
    private final Collection<CompileError> checkStyleProblems = new ArrayList<>();
    private InternalStatistics stats;
    private long compiletime;
    private List<TestFailure> failures;

    private String output;

    void addStyleError(CompilationUnit cu, CheckStyleError problem) {
        checkStyleProblems.add(problem);
    }

    void addProblem(CompilationUnit cu, Diagnostic<? extends JavaFileObject> r) {
        if (r.getKind() == Kind.ERROR) {
            errors.add(new InternalCompileProblem(r, cu));
        } else {
            otherProblems.add(new InternalCompileProblem(r, cu));
        }
    }

    public void setStatistics(InternalStatistics internalStatistics) {
        this.stats = internalStatistics;
    }

    @Override
    public int getNumberOfSuccessfulTests() {
        return stats.runCount - stats.failureCount;
    }

    @Override
    public int getNumberOfFailedTests() {
        return stats.failureCount;
    }

    @Override
    public int getNumberOfIgnoredTests() {
        return stats.ignoreCount;
    }

    @Override
    public Duration getTestDuration() {
        return Duration.ofMillis(stats.runtime);
    }

    @Override
    public Duration getCompileDuration() {
        return Duration.ofMillis(compiletime);
    }

    public void setCompileTime(long start, long end) {
        compiletime = end - start;
    }

    public void setFailures(List<TestFailure> failures) {
        this.failures = failures;
    }

    @Override
    public Collection<TestFailure> getTestFailures() {
        return Collections.unmodifiableCollection(this.failures);
    }

    @Override
    public boolean hasCompileErrors() {
        return errors.size() + checkStyleProblems.size() > 0;
    }

    @Override
    public Collection<CompileError> getCompilerErrors() {
        return Collections.unmodifiableCollection(errors);
    }

    @Override
    public Collection<CompileError> getStyleErrors() {
        return Collections.unmodifiableCollection(checkStyleProblems);
    }

    public void setOutput(String output) {
        this.output = output;
    }

    @Override
    public String getOutput() {
        return output;
    }

}
