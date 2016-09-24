package vk.core.api;

import java.time.Duration;
import java.util.Collection;

public interface CompilerResult {

    /**
     * @return true, if and only if the compilation process produced error
     *         messages
     */
    boolean hasCompileErrors();

    /**
     * @return Duration of the compilation process
     */
    Duration getCompileDuration();

    /**
     * @return List of all compiler errors for all compilation units
     */
    Collection<CompileError> getCompilerErrors();

    Collection<CompileError> getStyleErrors();

}
