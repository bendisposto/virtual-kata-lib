package vk.core.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.LocalizedMessage;

import vk.core.api.CompilationUnit;
import vk.core.api.CompileError;
import vk.core.api.CompilerResult;
import vk.core.api.JavaStringCompiler;
import vk.core.api.TestResult;

public class InternalCompiler implements JavaStringCompiler {

    Random rand = new Random();

    private HashMap<URI, CompilationUnit> backwardResolver = new HashMap<>();
    private HashMap<File, CompilationUnit> backwardFileResolver = new HashMap<>();
    private HashMap<CompilationUnit, File> forwardResolver = new HashMap<>();

    private HashMap<String, CompilationUnit> compilationUnits = new HashMap<>();

    private final JavaCompiler compiler;
    private final JUnitCore junit = new JUnitCore();
    InternalResult result = new InternalResult();
    private boolean compilerCalled = false;
    private CompilationUnit[] cus;

    public InternalCompiler(CompilationUnit[] cus) {
        compiler = ToolProvider.getSystemJavaCompiler();
        this.cus = cus;
        if (compiler == null) {
            throw new IllegalStateException("The compiler is only present in the JDK.");
        }
    }

    @Override
    public synchronized void compileAndRunTests() {
        prepareCompiler();
        try {
            Path tempDirectory = Files.createTempDirectory(null);
            saveAllCompilationUnits(tempDirectory);
            long start = System.currentTimeMillis();
            compileAllUnits();
            long end = System.currentTimeMillis();
            result.setCompileTime(start, end);
            compilerCalled = true;
            if (!result.hasCompileErrors()) {
                runAllTests(tempDirectory);
                runCheckStyle(tempDirectory);
            }
            recursivlyDeleteTempFolder(tempDirectory);
        } catch (IOException e) {
            throw new InternalCompilerException(
                    "Problem closing FileManager inside the compiler. This is most likely a bug in the compiler.", e);
        }
    }

    @Override
    public synchronized CompilerResult getCompilerResult() {
        if (!compilerCalled)
            throw new IllegalStateException("You need to call compileAndRunTests before you can get a result");
        return result;
    }

    @Override
    public synchronized TestResult getTestResult() {
        if (!compilerCalled)
            throw new IllegalStateException("You need to call compileAndRunTests before you can get a result");
        return result.hasCompileErrors() ? null : result;
    }

    @Override
    public Set<String> getAllCompilationUnitNames() {
        return compilationUnits.keySet();
    }

    @Override
    public CompilationUnit getCompilationUnitByName(String name) {
        return compilationUnits.get(name);
    }

    private void runAllTests(Path tempDirectory) {
        Class<?>[] tests = loadTests(tempDirectory);

        // Redirect output to a String
        PrintStream outOrg = System.out;
        PrintStream errOrg = System.err;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(outStream);
        System.setOut(ps);
        System.setErr(ps);
        Result run = junit.run(tests);

        // Restore output
        System.setOut(outOrg);
        System.setErr(errOrg);

        String out = new String(outStream.toByteArray(), StandardCharsets.UTF_8);

        result.setOutput(out);

        result.setStatistics(new InternalStatistics(run.getRunCount(), run.getFailureCount(), run.getIgnoreCount(),
                run.getRunTime()));
        result.setFailures(run.getFailures().stream().map(f -> new InternalFailure(f)).collect(Collectors.toList()));
    }

    @SuppressWarnings("deprecation")
    private void runCheckStyle(Path tempDirectory) {
        Checker checker = new Checker();

        try {
            Configuration conf = ConfigurationLoader.loadConfiguration(
                    getClass().getClassLoader().getResourceAsStream("default.xml"),
                    new PropertiesExpander(System.getProperties()), false);
            ClassLoader moduleClassLoader = this.getClass().getClassLoader();
            checker.setModuleClassLoader(moduleClassLoader);
            checker.configure(conf);

        } catch (CheckstyleException e1) {
            e1.printStackTrace();
        }
        LocalizedMessage.setLocale(Locale.ENGLISH);

        List<File> files = new ArrayList<>();
        for (CompilationUnit cu : compilationUnits.values()) {
            Path sourceFile = tempDirectory.resolve(cu.getClassName() + ".java");
            File file = sourceFile.toFile();
            files.add(file);
        }
        try {
            checker.addListener(new AuditListener() {

                @Override
                public void fileStarted(AuditEvent event) {
                }

                @Override
                public void fileFinished(AuditEvent event) {
                }

                @Override
                public void auditStarted(AuditEvent event) {

                }

                @Override
                public void auditFinished(AuditEvent event) {

                }

                @Override
                public void addException(AuditEvent event, Throwable throwable) {

                }

                @Override
                public void addError(AuditEvent event) {
                    File f = new File(event.getFileName());
                    int line = event.getLine();
                    int column = event.getColumn();

                    String message = event.getMessage();
                    CompilationUnit cu = backwardFileResolver.get(f);
                    result.addStyleError(cu, new CheckStyleError(line, column, message, cu));
                }
            });
            checker.process(files);
            Collection<CompileError> styleErrors = result.getStyleErrors();
            for (CompileError error : styleErrors) {
                String className = error.getCompilationUnit().getClassName();
                System.out.println(className + ": " +
                        error.getMessage() + "(" + error.getLineNumber() + "," + error.getColumnNumber() + ")");
            }

        } catch (CheckstyleException e) {
            e.printStackTrace();
        }
    }

    private Class<?>[] loadTests(Path dir) {
        List<Class<?>> testClasses = new ArrayList<>();

        URL url;
        try {
            url = dir.toUri().toURL();
        } catch (MalformedURLException e1) {
            // this shouldn't be possible
            throw new RuntimeException("Internal erorror while loading classes from " + dir, e1);
        }
        URL[] urls = new URL[] { url };
        try (URLClassLoader cl = new URLClassLoader(urls, this.getClass().getClassLoader(), null)) {

            for (CompilationUnit cu : compilationUnits.values()) {
                Class<?> loadedClass;
                try {
                    loadedClass = cl.loadClass(cu.getClassName());
                } catch (ClassNotFoundException e) {
                    // if nobody deleted the class files and compilation worked
                    // without errors this should not happen
                    throw new RuntimeException("Could not load class " + cu.getClassName());
                }
                if (cu.isATest())
                    testClasses.add(loadedClass);
            }
        } catch (IOException e1) {
            throw new RuntimeException("Problem closing Classloader", e1);
        }
        return testClasses.toArray(new Class[0]);
    }

    private void prepareCompiler() {

        compilationUnits.clear();

        for (CompilationUnit cu : cus) {
            String className = cu.getClassName();
            if (compilationUnits.containsKey(className))
                throw new IllegalArgumentException("Duplicate class names are not allowed.");
            compilationUnits.put(className, cu);

        }

        this.result = new InternalResult();
        backwardResolver.clear();
        backwardFileResolver.clear();
        forwardResolver.clear();
    }

    private void recursivlyDeleteTempFolder(Path path) throws IOException {

        Files.list(path).forEach(file -> {
            try {
                Files.delete(file);
            } catch (Exception e) {
                // ignore
            }
        });

        Files.delete(path);
        if (Files.exists(path)) {
            throw new IOException("Problem deleting compilation folder. Please remove the folder manually. Location: "
                    + path.toString());
        }
    }

    private void compileAllUnits() {

        DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticsCollector, null, null)) {

            populateBackwardResolver(fileManager);
            Iterable<? extends JavaFileObject> fileObjects = fileManager
                    .getJavaFileObjectsFromFiles(forwardResolver.values());

            runCompiler(compiler, diagnosticsCollector, fileManager, fileObjects);

            List<Diagnostic<? extends JavaFileObject>> results = diagnosticsCollector.getDiagnostics();
            for (Diagnostic<? extends JavaFileObject> r : results) {
                JavaFileObject source = r.getSource();
                URI uri = source.toUri();
                CompilationUnit cu = backwardResolver.get(uri);
                result.addProblem(cu, r);
            }

        } catch (IOException e) {
            throw new RuntimeException("Problem closing FileManager", e);
        }

    }

    private void populateBackwardResolver(StandardJavaFileManager fileManager) {
        for (CompilationUnit compilationUnit : compilationUnits.values()) {
            File file = forwardResolver.get(compilationUnit);
            JavaFileObject fileObject = fileManager.getJavaFileObjects(file).iterator().next();
            backwardResolver.put(fileObject.toUri(), compilationUnit);
            backwardFileResolver.put(file, compilationUnit);
        }
    }

    private void saveAllCompilationUnits(Path tempDirectory) {
        for (CompilationUnit unit : compilationUnits.values()) {
            saveCompilationUnitToFolder(tempDirectory, unit);
        }
    }

    private void saveCompilationUnitToFolder(Path folder, CompilationUnit cu) {
        Path path = folder.resolve(cu.getSourceFile());
        forwardResolver.put(cu, path.toFile());

        try {
            String classContent = cu.getClassContent();
            Files.write(path, classContent.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean runCompiler(JavaCompiler compiler, DiagnosticCollector<JavaFileObject> diagnostics,
            StandardJavaFileManager fileManager, Iterable<? extends JavaFileObject> fileObjects) {
        return compiler.getTask(null, fileManager, diagnostics, null, null, fileObjects).call();
    }

}
