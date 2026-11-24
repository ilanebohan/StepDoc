package com.exodius_software.stepdoc.core.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import com.exodius_software.stepdoc.core.parser.CucumberStepAnnotations.StepKeyword;

/**
 * Tells, from a file's imports alone, which classes declare their steps with lambdas.
 *
 * <p>cucumber-java8 has one interface per Gherkin language ({@code io.cucumber.java8.En}). Its
 * methods are named like the step annotations of that language, and a class implementing it
 * declares a step with each call to one of them: {@code When("I pay", () -> ...)}.
 *
 * <p>Only the interfaces a class lists itself are seen. What it inherits from a superclass is
 * not: that would need the whole type hierarchy, which sources alone do not give.
 */
final class LambdaGlueTypes {

    private static final String LAMBDA_PACKAGE = "io.cucumber.java8";

    /** Parent of the language interfaces: enough to declare a parameter type, not a step. */
    private static final String BASE_INTERFACE = "LambdaGlue";

    private final Map<String, String> packageBySimpleName;
    private final boolean lambdaPackageWildcard;

    private LambdaGlueTypes(Map<String, String> packageBySimpleName, boolean lambdaPackageWildcard) {
        this.packageBySimpleName = packageBySimpleName;
        this.lambdaPackageWildcard = lambdaPackageWildcard;
    }

    static LambdaGlueTypes of(CompilationUnit unit) {
        Map<String, String> packageBySimpleName = new HashMap<>();
        boolean lambdaPackageWildcard = false;
        for (ImportDeclaration declaration : unit.getImports()) {
            if (declaration.isStatic()) {
                continue;
            }
            Name name = declaration.getName();
            if (!declaration.isAsterisk()) {
                name.getQualifier().ifPresent(qualifier ->
                        packageBySimpleName.put(name.getIdentifier(), qualifier.asString()));
            } else if (LAMBDA_PACKAGE.equals(name.asString())) {
                lambdaPackageWildcard = true;
            }
        }
        return new LambdaGlueTypes(packageBySimpleName, lambdaPackageWildcard);
    }

    /** Whether {@code type} implements a cucumber-java8 interface itself. */
    boolean isLambdaGlue(TypeDeclaration<?> type) {
        return !lambdaInterfacesOf(type).isEmpty();
    }

    /**
     * The keyword of the step that {@code type} declares by calling {@code methodName}, or empty
     * when no language {@code type} implements has a step method of that name.
     */
    Optional<StepKeyword> keywordOf(TypeDeclaration<?> type, String methodName) {
        return lambdaInterfacesOf(type).stream()
                .flatMap(interfaceName -> GherkinKeywords.packageOfLambdaInterface(interfaceName).stream())
                .flatMap(languagePackage -> GherkinKeywords.kindOf(languagePackage, methodName).stream())
                .findFirst()
                .map(kind -> new StepKeyword(methodName, kind));
    }

    private List<String> lambdaInterfacesOf(TypeDeclaration<?> type) {
        if (!(type instanceof NodeWithImplements<?> implementing)) {
            return List.of();
        }
        return implementing.getImplementedTypes().stream()
                .filter(this::isLambdaInterface)
                .map(ClassOrInterfaceType::getNameAsString)
                .toList();
    }

    /**
     * As in Java, an import by name wins over a wildcard import. Behind the wildcard only the
     * interfaces known to exist are accepted: any other name is a type of the scanned project.
     */
    private boolean isLambdaInterface(ClassOrInterfaceType type) {
        String simpleName = type.getNameAsString();
        return type.getScope()
                .map(ClassOrInterfaceType::asString)
                .or(() -> Optional.ofNullable(packageBySimpleName.get(simpleName)))
                .map(LAMBDA_PACKAGE::equals)
                .orElseGet(() -> lambdaPackageWildcard && existsInLambdaPackage(simpleName));
    }

    private static boolean existsInLambdaPackage(String simpleName) {
        return BASE_INTERFACE.equals(simpleName) || GherkinKeywords.packageOfLambdaInterface(simpleName).isPresent();
    }
}