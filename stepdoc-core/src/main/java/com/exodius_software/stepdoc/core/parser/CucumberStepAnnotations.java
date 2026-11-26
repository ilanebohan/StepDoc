package com.exodius_software.stepdoc.core.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Name;

import com.exodius_software.stepdoc.core.model.StepKind;

/**
 * Tells, from a file's imports alone, which annotations are Cucumber step annotations.
 *
 * <p>An annotation imported by name or written fully qualified is a step as soon as its
 * package is a cucumber-java language package. Behind a wildcard import the content of the
 * package is unknown without a classpath, so only the keywords {@link GherkinKeywords} knows
 * are accepted: guessing would turn {@code @SuppressWarnings("x")} or a hook into a step.
 */
final class CucumberStepAnnotations {

    record StepKeyword(String keyword, StepKind kind) {
    }

    private static final Pattern STEP_PACKAGE = Pattern.compile("io\\.cucumber\\.java\\.[a-z_]+");
    private static final String GLUE_PACKAGE = "io.cucumber.java";
    private static final String PARAMETER_TYPE = "ParameterType";

    private final Map<String, String> packageBySimpleName;
    private final List<String> wildcardStepPackages;
    private final boolean gluePackageWildcard;

    private CucumberStepAnnotations(Map<String, String> packageBySimpleName, List<String> wildcardStepPackages,
                                    boolean gluePackageWildcard) {
        this.packageBySimpleName = packageBySimpleName;
        this.wildcardStepPackages = wildcardStepPackages;
        this.gluePackageWildcard = gluePackageWildcard;
    }

    static CucumberStepAnnotations of(CompilationUnit unit) {
        Map<String, String> packageBySimpleName = new HashMap<>();
        List<String> wildcardStepPackages = new ArrayList<>();
        boolean gluePackageWildcard = false;
        for (ImportDeclaration declaration : unit.getImports()) {
            if (declaration.isStatic()) {
                continue;
            }
            Name name = declaration.getName();
            if (!declaration.isAsterisk()) {
                name.getQualifier().ifPresent(qualifier ->
                        packageBySimpleName.put(name.getIdentifier(), qualifier.asString()));
            } else if (isStepPackage(name.asString())) {
                wildcardStepPackages.add(name.asString());
            } else if (GLUE_PACKAGE.equals(name.asString())) {
                gluePackageWildcard = true;
            }
        }
        return new CucumberStepAnnotations(packageBySimpleName, wildcardStepPackages, gluePackageWildcard);
    }

    /** The step keyword carried by {@code annotation}, or empty when it is not a step annotation. */
    Optional<StepKeyword> resolve(AnnotationExpr annotation) {
        Name name = annotation.getName();
        String simpleName = name.getIdentifier();
        return name.getQualifier()
                .map(Name::asString)
                .or(() -> importedPackageOf(simpleName))
                .filter(CucumberStepAnnotations::isStepPackage)
                .map(pkg -> new StepKeyword(simpleName, kindOf(pkg, simpleName)));
    }

    /** Whether {@code annotation} is Cucumber's {@code @ParameterType}, which declares a parameter type. */
    boolean isParameterType(AnnotationExpr annotation) {
        Name name = annotation.getName();
        if (!PARAMETER_TYPE.equals(name.getIdentifier())) {
            return false;
        }
        return name.getQualifier()
                .map(Name::asString)
                .or(() -> Optional.ofNullable(packageBySimpleName.get(PARAMETER_TYPE)))
                .map(GLUE_PACKAGE::equals)
                .orElse(gluePackageWildcard);
    }

    /** As in Java, an import by name wins over a wildcard import. */
    private Optional<String> importedPackageOf(String simpleName) {
        String explicitPackage = packageBySimpleName.get(simpleName);
        if (explicitPackage != null) {
            return Optional.of(explicitPackage);
        }
        return wildcardStepPackages.stream()
                .filter(pkg -> GherkinKeywords.kindOf(pkg, simpleName).isPresent())
                .findFirst();
    }

    /**
     * A named annotation that the bundled Gherkin library does not know (a keyword added by a
     * newer Cucumber, for instance) is still a step: it is listed, with {@link StepKind#OTHER}.
     */
    private static StepKind kindOf(String pkg, String keyword) {
        return GherkinKeywords.kindOf(pkg, keyword).orElse(StepKind.OTHER);
    }

    private static boolean isStepPackage(String packageName) {
        return STEP_PACKAGE.matcher(packageName).matches();
    }
}
