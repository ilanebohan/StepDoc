package fr.bohanilane.stepdoc.core.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Name;

import fr.bohanilane.stepdoc.core.model.StepKind;

/**
 * Tells, from a file's imports alone, which annotations are Cucumber step annotations.
 *
 * <p>Explicit imports and fully qualified names work for every Gherkin language.
 * Wildcard imports only resolve the English keywords: the content of a package is
 * unknown without a classpath, and guessing would turn {@code @SuppressWarnings("x")}
 * or a hook into a step.
 */
final class CucumberStepAnnotations {

    record StepKeyword(String keyword, StepKind kind) {
    }

    private static final Pattern STEP_PACKAGE = Pattern.compile("io\\.cucumber\\.java\\.[a-z_]+");
    private static final String ENGLISH_PACKAGE = "io.cucumber.java.en";
    private static final Set<String> ENGLISH_KEYWORDS = Set.of("Given", "When", "Then", "And", "But");

    private final Map<String, String> packageBySimpleName;
    private final boolean englishWildcardImported;

    private CucumberStepAnnotations(Map<String, String> packageBySimpleName, boolean englishWildcardImported) {
        this.packageBySimpleName = packageBySimpleName;
        this.englishWildcardImported = englishWildcardImported;
    }

    static CucumberStepAnnotations of(CompilationUnit unit) {
        Map<String, String> packageBySimpleName = new HashMap<>();
        boolean englishWildcardImported = false;
        for (ImportDeclaration declaration : unit.getImports()) {
            if (declaration.isStatic()) {
                continue;
            }
            Name name = declaration.getName();
            if (declaration.isAsterisk()) {
                englishWildcardImported |= ENGLISH_PACKAGE.equals(name.asString());
            } else {
                name.getQualifier().ifPresent(qualifier ->
                        packageBySimpleName.put(name.getIdentifier(), qualifier.asString()));
            }
        }
        return new CucumberStepAnnotations(packageBySimpleName, englishWildcardImported);
    }

    /** The step keyword carried by {@code annotation}, or empty when it is not a step annotation. */
    Optional<StepKeyword> resolve(AnnotationExpr annotation) {
        Name name = annotation.getName();
        String simpleName = name.getIdentifier();
        return name.getQualifier()
                .map(Name::asString)
                .or(() -> importedPackageOf(simpleName))
                .filter(pkg -> STEP_PACKAGE.matcher(pkg).matches())
                .map(pkg -> new StepKeyword(simpleName, kindOf(pkg, simpleName)));
    }

    private Optional<String> importedPackageOf(String simpleName) {
        String explicitPackage = packageBySimpleName.get(simpleName);
        if (explicitPackage != null) {
            return Optional.of(explicitPackage);
        }
        boolean fromEnglishWildcard = englishWildcardImported && ENGLISH_KEYWORDS.contains(simpleName);
        return fromEnglishWildcard ? Optional.of(ENGLISH_PACKAGE) : Optional.empty();
    }

    private static StepKind kindOf(String pkg, String keyword) {
        if (!ENGLISH_PACKAGE.equals(pkg)) {
            return StepKind.OTHER;
        }
        return switch (keyword) {
            case "Given" -> StepKind.GIVEN;
            case "When" -> StepKind.WHEN;
            case "Then" -> StepKind.THEN;
            case "And" -> StepKind.AND;
            case "But" -> StepKind.BUT;
            default -> StepKind.OTHER;
        };
    }
}
