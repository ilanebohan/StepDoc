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
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
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

    /**
     * The code a step runs: a method, or what a cucumber-java8 call is given.
     *
     * @param label      how a problem names it
     * @param methodName empty when it is not a method
     * @param parameters empty when they cannot be read: where cucumber-java8 expects a lambda,
     *                   Java also accepts a method reference or a variable
     */
    private record StepBody(String label, String methodName, Optional<NodeList<Parameter>> parameters) {
    }

    /** Method of cucumber-java8 that declares a parameter type: name, regular expression, lambda. */
    private static final String PARAMETER_TYPE_METHOD = "ParameterType";

    private final String file;
    private final CompilationUnit unit;
    private final CucumberStepAnnotations annotationResolver;
    private final LambdaGlueTypes lambdaGlueTypes;
    private final StringConstants constants;
    private final List<StepDefinition> steps = new ArrayList<>();
    private final List<ParameterTypeDefinition> parameterTypes = new ArrayList<>();
    private final List<Problem> problems = new ArrayList<>();

    SourceFileScan(String file, CompilationUnit unit, StringConstants constants) {
        this.file = file;
        this.unit = unit;
        this.constants = constants;
        this.annotationResolver = CucumberStepAnnotations.of(unit);
        this.lambdaGlueTypes = LambdaGlueTypes.of(unit);
    }

    StepCatalog scan() {
        unit.findAll(MethodDeclaration.class).forEach(this::scanMethod);
        unit.findAll(MethodCallExpr.class).forEach(this::scanCall);
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
        StepBody body = new StepBody("method " + method.getNameAsString(), method.getNameAsString(),
                Optional.of(method.getParameters()));
        for (StepAnnotation annotation : stepAnnotations) {
            Optional<Expression> expression = AnnotationValues.member(annotation.expression(), "value");
            addStep(annotation.keyword(), annotation.expression(), expression, body, documentation);
        }
    }

    /**
     * In a class that implements a cucumber-java8 interface, a call to one of the keyword
     * methods it inherits declares a step, and a call to {@code ParameterType} a parameter type.
     */
    private void scanCall(MethodCallExpr call) {
        boolean inheritedMethod = call.getScope().map(Expression::isThisExpr).orElse(true);
        Optional<TypeDeclaration<?>> type = enclosingTypeOf(call);
        if (!inheritedMethod || type.isEmpty() || !lambdaGlueTypes.isLambdaGlue(type.get())) {
            return;
        }
        int argumentCount = call.getArguments().size();
        if (argumentCount == 3 && PARAMETER_TYPE_METHOD.equals(call.getNameAsString())) {
            addLambdaParameterType(call);
        } else if (argumentCount == 2) {
            lambdaGlueTypes.keywordOf(type.get(), call.getNameAsString())
                    .ifPresent(keyword -> addLambdaStep(call, keyword));
        }
    }

    private void addLambdaStep(MethodCallExpr call, StepKeyword keyword) {
        Optional<NodeList<Parameter>> parameters = call.getArgument(1).toLambdaExpr().map(LambdaExpr::getParameters);
        StepBody body = new StepBody("the " + keyword.keyword() + " lambda", "", parameters);
        MethodDocumentation documentation =
                MethodDocumentation.ofLambda(commentAbove(call), parameters.orElseGet(NodeList::new));
        addStep(keyword, call, Optional.of(call.getArgument(0)), body, documentation);
    }

    /** As in Cucumber, a parameter type is named after its method unless the annotation names it. */
    private void addParameterType(MethodDeclaration method, AnnotationExpr annotation) {
        SourceLocation location = locationOf(annotation);
        String name = AnnotationValues.member(annotation, "name")
                .flatMap(constants::valueOf)
                .filter(value -> !value.isBlank())
                .orElse(method.getNameAsString());
        String description = MethodDocumentation.of(method, constants).description();
        AnnotationValues.member(annotation, "value").flatMap(constants::valueOf).ifPresentOrElse(
                regex -> addParameterType(location, name, regex, description),
                () -> reportUnreadableParameterType(location, name));
    }

    /** A name that cannot be read is reported the way it is written in the call. */
    private void addLambdaParameterType(MethodCallExpr call) {
        SourceLocation location = locationOf(call);
        Optional<String> name = constants.valueOf(call.getArgument(0));
        if (name.isEmpty()) {
            reportUnreadableParameterType(location, call.getArgument(0).toString());
            return;
        }
        String description = MethodDocumentation.ofLambda(commentAbove(call), new NodeList<>()).description();
        constants.valueOf(call.getArgument(1)).ifPresentOrElse(
                regex -> addParameterType(location, name.get(), regex, description),
                () -> reportUnreadableParameterType(location, name.get()));
    }

    private void addParameterType(SourceLocation location, String name, String regex, String description) {
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            problems.add(new Problem(location, "Parameter type '" + name
                    + "' has an invalid regular expression (" + e.getDescription() + "), it was ignored"));
            return;
        }
        parameterTypes.add(new ParameterTypeDefinition(name, regex, description, location));
    }

    private void reportUnreadableParameterType(SourceLocation location, String name) {
        problems.add(new Problem(location, "Parameter type '" + name
                + "' cannot be read from the scanned sources, it was ignored"));
    }

    private void checkStepDocParameterCount(MethodDeclaration method, MethodDocumentation documentation) {
        int methodParameterCount = method.getParameters().size();
        if (documentation.stepDocParameterCount() > methodParameterCount) {
            problems.add(new Problem(locationOf(method), "@StepDoc documents "
                    + documentation.stepDocParameterCount() + " parameter(s) but method "
                    + method.getNameAsString() + " takes " + methodParameterCount));
        }
    }

    /**
     * @param declaration what declares the step: its annotation, or the cucumber-java8 call
     * @param expression  the step text as written there, empty when the annotation gives none
     */
    private void addStep(StepKeyword keyword, Node declaration, Optional<Expression> expression, StepBody body,
                         MethodDocumentation documentation) {
        SourceLocation location = locationOf(declaration);
        Optional<String> text = expression.flatMap(constants::valueOf);
        if (text.isEmpty()) {
            problems.add(new Problem(location, "Step expression of " + body.label()
                    + " cannot be read from the scanned sources, the step was skipped"));
            return;
        }
        ExpressionSyntax syntax = StepExpressions.syntaxOf(text.get());
        List<String> expressionTypes = StepExpressions.parameterTypes(text.get(), syntax);
        checkArity(body, text.get(), expressionTypes.size(), location);

        steps.add(new StepDefinition(
                keyword.keyword(),
                keyword.kind(),
                text.get(),
                syntax,
                documentation.description(),
                parameters(body.parameters().orElseGet(NodeList::new), expressionTypes, documentation),
                enclosingTypeOf(declaration).map(SourceFileScan::nameOf).orElse(""),
                body.methodName(),
                location));
    }

    /** Cucumber accepts exactly one argument per expression parameter, plus an optional data table or doc string. */
    private void checkArity(StepBody body, String expression, int expressionParameterCount, SourceLocation location) {
        if (body.parameters().isEmpty()) {
            return;
        }
        int bodyParameterCount = body.parameters().get().size();
        boolean arityMatches = bodyParameterCount == expressionParameterCount
                || bodyParameterCount == expressionParameterCount + 1;
        if (!arityMatches) {
            problems.add(new Problem(location, "Step \"" + expression + "\" has " + expressionParameterCount
                    + " parameter(s) but " + body.label() + " takes " + bodyParameterCount));
        }
    }

    /** One entry per argument: expression parameters first, then the data table or doc string if any. */
    private static List<StepParameter> parameters(NodeList<Parameter> bodyParameters, List<String> expressionTypes,
                                                  MethodDocumentation documentation) {
        int argumentCount = Math.max(bodyParameters.size(), expressionTypes.size());
        List<StepParameter> parameters = new ArrayList<>();
        for (int i = 0; i < argumentCount; i++) {
            Optional<Parameter> bodyParameter = i < bodyParameters.size()
                    ? Optional.of(bodyParameters.get(i))
                    : Optional.empty();
            boolean fromExpression = i < expressionTypes.size();
            ParamDoc doc = documentation.parameter(i);
            parameters.add(new StepParameter(
                    bodyParameter.map(Parameter::getNameAsString).orElse(""),
                    bodyParameter.map(SourceFileScan::writtenTypeOf).orElse(""),
                    fromExpression
                            ? StepParameter.Source.EXPRESSION
                            : docArgumentSource(bodyParameter.orElseThrow().getType()),
                    fromExpression ? expressionTypes.get(i) : "",
                    doc.description(),
                    doc.examples()));
        }
        return parameters;
    }

    /** A lambda parameter may have no type, or {@code var}: neither tells a reader anything. */
    private static String writtenTypeOf(Parameter parameter) {
        Type type = parameter.getType();
        return type.isUnknownType() || type.isVarType() ? "" : type.asString();
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

    /** A comment written above a statement belongs to the statement, not to the call it is made of. */
    private static Optional<Comment> commentAbove(MethodCallExpr call) {
        return call.getComment().or(() -> call.getParentNode()
                .filter(ExpressionStmt.class::isInstance)
                .flatMap(Node::getComment));
    }

    private static Optional<TypeDeclaration<?>> enclosingTypeOf(Node node) {
        Optional<Node> ancestor = node.getParentNode();
        while (ancestor.isPresent()) {
            if (ancestor.get() instanceof TypeDeclaration<?> type) {
                return Optional.of(type);
            }
            ancestor = ancestor.get().getParentNode();
        }
        return Optional.empty();
    }

    private static String nameOf(TypeDeclaration<?> type) {
        return type.getFullyQualifiedName().orElse(type.getNameAsString());
    }

    private SourceLocation locationOf(Node node) {
        return new SourceLocation(file, node.getBegin().map(position -> position.line).orElse(1));
    }
}