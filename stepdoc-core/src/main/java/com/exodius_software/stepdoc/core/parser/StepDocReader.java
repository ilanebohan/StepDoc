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

    static StepDocValues read(MethodDeclaration method, StringConstants constants) {
        return method.getAnnotations().stream()
                .filter(StepDocReader::isStepDoc)
                .findFirst()
                .map(stepDoc -> values(stepDoc, constants))
                .orElse(StepDocValues.NONE);
    }

    private static boolean isStepDoc(AnnotationExpr annotation) {
        String name = annotation.getNameAsString();
        return SIMPLE_NAME.equals(name) || QUALIFIED_NAME.equals(name);
    }

    private static StepDocValues values(AnnotationExpr stepDoc, StringConstants constants) {
        String description = AnnotationValues.member(stepDoc, "value")
                .map(value -> text(value, constants))
                .orElse("");
        List<ParamDoc> params = AnnotationValues.member(stepDoc, "params")
                .map(AnnotationValues::elements)
                .orElse(List.of())
                .stream()
                .filter(AnnotationExpr.class::isInstance)
                .map(element -> paramDoc((AnnotationExpr) element, constants))
                .toList();
        return new StepDocValues(description, params);
    }

    private static ParamDoc paramDoc(AnnotationExpr param, StringConstants constants) {
        String description = AnnotationValues.member(param, "description")
                .map(value -> text(value, constants))
                .orElse("");
        List<String> examples = AnnotationValues.member(param, "examples")
                .map(AnnotationValues::elements)
                .orElse(List.of())
                .stream()
                .map(example -> text(example, constants))
                .toList();
        return new ParamDoc(description, examples);
    }

    /**
     * A constant declared outside the scanned sources has no value here. Its source text is
     * kept rather than dropped, so the gap shows up in the generated documentation.
     */
    private static String text(Expression expression, StringConstants constants) {
        return constants.valueOf(expression).orElseGet(expression::toString);
    }
}
