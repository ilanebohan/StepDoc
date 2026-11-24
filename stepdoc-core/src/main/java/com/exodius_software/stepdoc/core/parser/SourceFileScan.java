package com.exodius_software.stepdoc.core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import com.exodius_software.stepdoc.core.model.ExpressionSyntax;
import com.exodius_software.stepdoc.core.model.ParameterTypeDefinition;
import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.SourceLocation;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.model.StepParameter;
import com.exodius_software.stepdoc.core.parser.CucumberStepAnnotations.StepKeyword;
import com.exodius_software.stepdoc.core.parser.StepDocReader.ParamDoc;

/** Extracts the steps of one parsed file. Single use: create, call {@link #scan()}, discard. */
final class SourceFileScan {

    private record StepAnnotation(AnnotationExpr expression, StepKeyword keyword) {
    }

    private final String file;
    private final CompilationUnit unit;
    private final CucumberStepAnnotations annotationResolver;
    private final StringConstants constants;
    private final List<StepDefinition> steps = new ArrayList<>();
    private final List<ParameterTypeDefinition> parameterTypes = new ArrayList<>();
    private final List<Problem> problems = new ArrayList<>();

    SourceFileScan(String file, CompilationUnit unit, StringConstants constants) {
        this.file = file;
        this.unit = unit;
        this.constants = constants;
        this.annotationResolver = CucumberStepAnnotations.of(unit);
    }

    StepCatalog scan() {
        unit.findAll(MethodDeclaration.class).forEach(this::scanMethod);
        return new StepCatalog(steps, parameterTypes, problems);
    }

    private void scanMethod(MethodDeclaration method) {
        method.getAnnotations().stream()
                .filter(annotationResolver::isParameterType)
                .forEach(annotation -> addParameterType(method, annotation));
        List<StepAnnotation> stepAnnotations = method.getAnnotations().stream()
                .flatMap(annotation -> annotationResolver.resolve(annotation)
                        .map(keyword -> new StepAnnotation(annotation, keyword))
                        .stream())
                .toList();
        if (stepAnnotations.isEmpty()) {
            return;
        }
        MethodDocumentation documentation = MethodDocumentation.of(method, constants);
        checkStepDocParameterCount(method, documentation);
        stepAnnotations.forEach(annotation -> addStep(method, annotation, documentation));
    }

    /** As in Cucumber, a parameter type is named after its method unless the annotation names it. */
    private void addParameterType(MethodDeclaration method, AnnotationExpr annotation) {
        SourceLocation location = locationOf(annotation);
        String name = AnnotationValues.member(annotation, "name")
                .flatMap(constants::valueOf)
                .filter(value -> !value.isBlank())
                .orElse(method.getNameAsString());
        Optional<String> regex = AnnotationValues.member(annotation, "value").flatMap(constants::valueOf);
        if (regex.isEmpty()) {
            problems.add(new Problem(location, "Parameter type '" + name
                    + "' cannot be read from the scanned sources, it was ignored"));
            return;
        }
        try {
            Pattern.compile(regex.get());
        } catch (PatternSyntaxException e) {
            problems.add(new Problem(location, "Parameter type '" + name
                    + "' has an invalid regular expression (" + e.getDescription() + "), it was ignored"));
            return;
        }
        String description = MethodDocumentation.of(method, constants).description();
        parameterTypes.add(new ParameterTypeDefinition(name, regex.get(), description, location));
    }

    private void checkStepDocParameterCount(MethodDeclaration method, MethodDocumentation documentation) {
        int methodParameterCount = method.getParameters().size();
        if (documentation.stepDocParameterCount() > methodParameterCount) {
            problems.add(new Problem(locationOf(method), "@StepDoc documents "
                    + documentation.stepDocParameterCount() + " parameter(s) but method "
                    + method.getNameAsString() + " takes " + methodParameterCount));
        }
    }

    private void addStep(MethodDeclaration method, StepAnnotation annotation, MethodDocumentation documentation) {
        SourceLocation location = locationOf(annotation.expression());
        Optional<String> expression = AnnotationValues.member(annotation.expression(), "value")
                .flatMap(constants::valueOf);
        if (expression.isEmpty()) {
            problems.add(new Problem(location, "Step expression of method " + method.getNameAsString()
                    + " cannot be read from the scanned sources, the step was skipped"));
            return;
        }
        ExpressionSyntax syntax = StepExpressions.syntaxOf(expression.get());
        List<String> expressionTypes = StepExpressions.parameterTypes(expression.get(), syntax);
        checkArity(method, expression.get(), expressionTypes.size(), location);

        steps.add(new StepDefinition(
                annotation.keyword().keyword(),
                annotation.keyword().kind(),
                expression.get(),
                syntax,
                documentation.description(),
                parameters(method.getParameters(), expressionTypes, documentation),
                declaringClassOf(method),
                method.getNameAsString(),
                location));
    }

    /** Cucumber accepts exactly one argument per expression parameter, plus an optional data table or doc string. */
    private void checkArity(MethodDeclaration method, String expression, int expressionParameterCount,
                            SourceLocation location) {
        int methodParameterCount = method.getParameters().size();
        boolean arityMatches = methodParameterCount == expressionParameterCount
                || methodParameterCount == expressionParameterCount + 1;
        if (!arityMatches) {
            problems.add(new Problem(location, "Step \"" + expression + "\" has " + expressionParameterCount
                    + " parameter(s) but method " + method.getNameAsString() + " takes " + methodParameterCount));
        }
    }

    /** One entry per argument: expression parameters first, then the data table or doc string if any. */
    private static List<StepParameter> parameters(NodeList<Parameter> methodParameters, List<String> expressionTypes,
                                                  MethodDocumentation documentation) {
        int argumentCount = Math.max(methodParameters.size(), expressionTypes.size());
        List<StepParameter> parameters = new ArrayList<>();
        for (int i = 0; i < argumentCount; i++) {
            Optional<Parameter> methodParameter = i < methodParameters.size()
                    ? Optional.of(methodParameters.get(i))
                    : Optional.empty();
            boolean fromExpression = i < expressionTypes.size();
            ParamDoc doc = documentation.parameter(i);
            parameters.add(new StepParameter(
                    methodParameter.map(Parameter::getNameAsString).orElse(""),
                    methodParameter.map(parameter -> parameter.getType().asString()).orElse(""),
                    fromExpression
                            ? StepParameter.Source.EXPRESSION
                            : docArgumentSource(methodParameter.orElseThrow().getType()),
                    fromExpression ? expressionTypes.get(i) : "",
                    doc.description(),
                    doc.examples()));
        }
        return parameters;
    }

    /**
     * Cucumber passes a doc string to a {@code String} or {@code DocString} argument,
     * a data table to anything else.
     */
    private static StepParameter.Source docArgumentSource(Type type) {
        String simpleName = type instanceof ClassOrInterfaceType classType
                ? classType.getNameAsString()
                : type.asString();
        boolean docString = "String".equals(simpleName) || "DocString".equals(simpleName);
        return docString ? StepParameter.Source.DOC_STRING : StepParameter.Source.DATA_TABLE;
    }

    private static String declaringClassOf(MethodDeclaration method) {
        Optional<Node> ancestor = method.getParentNode();
        while (ancestor.isPresent()) {
            if (ancestor.get() instanceof TypeDeclaration<?> type) {
                return type.getFullyQualifiedName().orElse(type.getNameAsString());
            }
            ancestor = ancestor.get().getParentNode();
        }
        return "";
    }

    private SourceLocation locationOf(Node node) {
        return new SourceLocation(file, node.getBegin().map(position -> position.line).orElse(1));
    }
}
