package fr.bohanilane.stepdoc.core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import fr.bohanilane.stepdoc.core.model.ExpressionSyntax;
import fr.bohanilane.stepdoc.core.model.Problem;
import fr.bohanilane.stepdoc.core.model.SourceLocation;
import fr.bohanilane.stepdoc.core.model.StepCatalog;
import fr.bohanilane.stepdoc.core.model.StepDefinition;
import fr.bohanilane.stepdoc.core.model.StepParameter;
import fr.bohanilane.stepdoc.core.parser.CucumberStepAnnotations.StepKeyword;
import fr.bohanilane.stepdoc.core.parser.StepDocReader.ParamDoc;

/** Extracts the steps of one parsed file. Single use: create, call {@link #scan()}, discard. */
final class SourceFileScan {

    private record StepAnnotation(AnnotationExpr expression, StepKeyword keyword) {
    }

    private final String file;
    private final CompilationUnit unit;
    private final CucumberStepAnnotations annotationResolver;
    private final List<StepDefinition> steps = new ArrayList<>();
    private final List<Problem> problems = new ArrayList<>();

    SourceFileScan(String file, CompilationUnit unit) {
        this.file = file;
        this.unit = unit;
        this.annotationResolver = CucumberStepAnnotations.of(unit);
    }

    StepCatalog scan() {
        unit.findAll(MethodDeclaration.class).forEach(this::scanMethod);
        return new StepCatalog(steps, problems);
    }

    private void scanMethod(MethodDeclaration method) {
        List<StepAnnotation> stepAnnotations = method.getAnnotations().stream()
                .flatMap(annotation -> annotationResolver.resolve(annotation)
                        .map(keyword -> new StepAnnotation(annotation, keyword))
                        .stream())
                .toList();
        if (stepAnnotations.isEmpty()) {
            return;
        }
        MethodDocumentation documentation = MethodDocumentation.of(method);
        checkStepDocParameterCount(method, documentation);
        stepAnnotations.forEach(annotation -> addStep(method, annotation, documentation));
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
                .flatMap(AnnotationValues::constantString);
        if (expression.isEmpty()) {
            problems.add(new Problem(location, "Step expression of method " + method.getNameAsString()
                    + " is not a string literal, the step was skipped"));
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

    /** Cucumber passes a doc string to a {@code String} or {@code DocString} argument, a data table to anything else. */
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
