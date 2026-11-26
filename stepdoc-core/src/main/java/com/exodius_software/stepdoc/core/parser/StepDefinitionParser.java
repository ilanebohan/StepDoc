package com.exodius_software.stepdoc.core.parser;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;

import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.SourceLocation;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.util.FileTree;

/**
 * Builds a {@link StepCatalog} from Java sources. Works on source text only:
 * nothing is compiled, loaded or executed, so the scanned project needs no build.
 */
public final class StepDefinitionParser {

    private static final String JAVA_EXTENSION = ".java";

    private final JavaParser javaParser =
            new JavaParser(new ParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_25));

    /**
     * Scans every {@code .java} file under the {@code sourceRoots}, recursively.
     * Roots are visited in the order given and the files of each in relative path order, so
     * the result does not depend on the file system. All roots are read as one set of
     * sources: a step may use a constant declared under another root. A file that is not
     * valid in {@code encoding} is reported and skipped.
     */
    public StepCatalog parseDirectories(List<Path> sourceRoots, Charset encoding) throws IOException {
        List<ParsedFile> files = new ArrayList<>();
        for (Path sourceRoot : sourceRoots) {
            for (FileTree.Entry file : FileTree.filesWithExtension(sourceRoot, JAVA_EXTENSION)) {
                files.add(read(file, encoding));
            }
        }
        return scan(files);
    }

    /**
     * Parses one source file.
     *
     * @param relativePath where the source comes from, used to locate steps and problems
     */
    public StepCatalog parseSource(String relativePath, String source) {
        return scan(List.of(parse(relativePath, source)));
    }

    /** Constants are collected from every file first: a step may use one declared in another file. */
    private static StepCatalog scan(List<ParsedFile> files) {
        StringConstants constants = StringConstants.declaredIn(
                files.stream().flatMap(file -> file.unit().stream()).toList());
        List<StepCatalog> catalogs = new ArrayList<>();
        for (ParsedFile file : files) {
            catalogs.add(file.unit()
                    .map(unit -> new SourceFileScan(file.relativePath(), unit, constants).scan())
                    .orElseGet(() -> new StepCatalog(List.of(), file.problems())));
        }
        return StepCatalog.concat(catalogs);
    }

    private ParsedFile read(FileTree.Entry file, Charset encoding) throws IOException {
        try {
            return parse(file.relativePath(), Files.readString(file.path(), encoding));
        } catch (CharacterCodingException e) {
            return ParsedFile.unreadable(file.relativePath(), 1,
                    "File is not valid " + encoding.name() + ", it was skipped");
        }
    }

    private ParsedFile parse(String relativePath, String source) {
        ParseResult<CompilationUnit> result = javaParser.parse(source);
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            return ParsedFile.unreadable(relativePath, firstSyntaxErrorLine(result), syntaxErrorMessage(result));
        }
        return new ParsedFile(relativePath, result.getResult(), List.of());
    }

    /** A source file after parsing: its syntax tree, or the problem that prevented reading it. */
    private record ParsedFile(String relativePath, Optional<CompilationUnit> unit, List<Problem> problems) {

        static ParsedFile unreadable(String relativePath, int line, String message) {
            Problem problem = new Problem(new SourceLocation(relativePath, line), message);
            return new ParsedFile(relativePath, Optional.empty(), List.of(problem));
        }
    }

    private static int firstSyntaxErrorLine(ParseResult<CompilationUnit> result) {
        return result.getProblems().stream()
                .findFirst()
                .flatMap(com.github.javaparser.Problem::getLocation)
                .flatMap(tokenRange -> tokenRange.getBegin().getRange())
                .map(range -> range.begin.line)
                .orElse(1);
    }

    private static String syntaxErrorMessage(ParseResult<CompilationUnit> result) {
        String cause = result.getProblems().stream()
                .findFirst()
                .map(com.github.javaparser.Problem::getMessage)
                .orElse("unknown error");
        return "File could not be parsed, it was skipped: " + cause;
    }
}
