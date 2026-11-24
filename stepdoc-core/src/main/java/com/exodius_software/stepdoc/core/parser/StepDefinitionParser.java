package com.exodius_software.stepdoc.core.parser;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;

import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.SourceLocation;
import com.exodius_software.stepdoc.core.model.StepCatalog;

/**
 * Builds a {@link StepCatalog} from Java sources. Works on source text only:
 * nothing is compiled, loaded or executed, so the scanned project needs no build.
 */
public final class StepDefinitionParser {

    private static final String JAVA_EXTENSION = ".java";

    private final JavaParser javaParser =
            new JavaParser(new ParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_25));

    /**
     * Scans every {@code .java} file under {@code sourceRoot}, recursively.
     * Files are read as UTF-8 and visited in relative path order, so the result
     * does not depend on the file system.
     */
    public StepCatalog parseDirectory(Path sourceRoot) throws IOException {
        List<Path> files;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            files = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(JAVA_EXTENSION))
                    .sorted(Comparator.comparing(path -> relativePath(sourceRoot, path)))
                    .toList();
        }
        List<StepCatalog> catalogs = new ArrayList<>();
        for (Path file : files) {
            catalogs.add(parseFile(relativePath(sourceRoot, file), file));
        }
        return StepCatalog.concat(catalogs);
    }

    /**
     * Parses one source file.
     *
     * @param relativePath where the source comes from, used to locate steps and problems
     */
    public StepCatalog parseSource(String relativePath, String source) {
        ParseResult<CompilationUnit> result = javaParser.parse(source);
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            return onlyProblem(relativePath, firstSyntaxErrorLine(result), syntaxErrorMessage(result));
        }
        return new SourceFileScan(relativePath, result.getResult().get()).scan();
    }

    private StepCatalog parseFile(String relativePath, Path file) throws IOException {
        try {
            return parseSource(relativePath, Files.readString(file));
        } catch (CharacterCodingException e) {
            return onlyProblem(relativePath, 1, "File is not valid UTF-8, it was skipped");
        }
    }

    private static StepCatalog onlyProblem(String relativePath, int line, String message) {
        return new StepCatalog(List.of(), List.of(new Problem(new SourceLocation(relativePath, line), message)));
    }

    private static int firstSyntaxErrorLine(ParseResult<CompilationUnit> result) {
        return result.getProblems().stream()
                .findFirst()
                .flatMap(com.github.javaparser.Problem::getLocation)
                .map(tokenRange -> tokenRange.getBegin().getRange().map(range -> range.begin.line).orElse(1))
                .orElse(1);
    }

    private static String syntaxErrorMessage(ParseResult<CompilationUnit> result) {
        String cause = result.getProblems().stream()
                .findFirst()
                .map(com.github.javaparser.Problem::getMessage)
                .orElse("unknown error");
        return "File could not be parsed, it was skipped: " + cause;
    }

    private static String relativePath(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }
}
