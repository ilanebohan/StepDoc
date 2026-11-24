package com.exodius_software.stepdoc.core.parser;

import java.util.List;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;

/** Reads {@code @StepDoc} from the syntax tree; the annotation itself never reaches the classpath. */
final class StepDocReader {

    record ParamDoc(String description, List<String> examples) {
    }

    record StepDocValues(String description, List<ParamDoc> params) {

        static final StepDocValues NONE = new StepDocValues("", List.of());
    }

    private static final String SIMPLE_NAME = "StepDoc";
    private static final String QUALIFIED_NAME = "com.exodius_software.stepdoc.annotations.StepDoc";

    private StepDocReader() {
    }

    static StepDocValues read(MethodDeclaration method) {
        return method.getAnnotations().stream()
                .filter(StepDocReader::isStepDoc)
                .findFirst()
                .map(StepDocReader::values)
                .orElse(StepDocValues.NONE);
    }

    private static boolean isStepDoc(AnnotationExpr annotation) {
        String name = annotation.getNameAsString();
        return SIMPLE_NAME.equals(name) || QUALIFIED_NAME.equals(name);
    }

    private static StepDocValues values(AnnotationExpr stepDoc) {
        String description = AnnotationValues.member(stepDoc, "value").map(StepDocReader::text).orElse("");
        List<ParamDoc> params = AnnotationValues.member(stepDoc, "params")
                .map(AnnotationValues::elements)
                .orElse(List.of())
                .stream()
                .filter(AnnotationExpr.class::isInstance)
                .map(element -> paramDoc((AnnotationExpr) element))
                .toList();
        return new StepDocValues(description, params);
    }

    private static ParamDoc paramDoc(AnnotationExpr param) {
        String description = AnnotationValues.member(param, "description").map(StepDocReader::text).orElse("");
        List<String> examples = AnnotationValues.member(param, "examples")
                .map(AnnotationValues::elements)
                .orElse(List.of())
                .stream()
                .map(StepDocReader::text)
                .toList();
        return new ParamDoc(description, examples);
    }

    /**
     * A constant reference cannot be resolved without the classpath. Its source text is kept
     * rather than dropped, so the gap shows up in the generated documentation.
     */
    private static String text(Expression expression) {
        return AnnotationValues.constantString(expression).orElseGet(expression::toString);
    }
}
