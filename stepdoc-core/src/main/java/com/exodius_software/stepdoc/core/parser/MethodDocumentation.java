package com.exodius_software.stepdoc.core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.Comment;

import com.exodius_software.stepdoc.core.parser.JavadocReader.MethodJavadoc;
import com.exodius_software.stepdoc.core.parser.StepDocReader.ParamDoc;
import com.exodius_software.stepdoc.core.parser.StepDocReader.StepDocValues;

/**
 * Documentation of the code behind a step, a method or a lambda. That of a method is merged
 * from its Javadoc and its optional {@code @StepDoc}: when both describe the same thing,
 * {@code @StepDoc} wins, as it was written for the catalog.
 *
 * @param parameters            one entry per method parameter, in declaration order
 * @param stepDocParameterCount number of {@code @StepDoc.Param}, to detect extra ones
 */
record MethodDocumentation(String description, List<ParamDoc> parameters, int stepDocParameterCount) {

    private static final ParamDoc UNDOCUMENTED = new ParamDoc("", List.of());

    static MethodDocumentation of(MethodDeclaration method, StringConstants constants) {
        return merge(JavadocReader.read(method), StepDocReader.read(method, constants), method.getParameters());
    }

    /**
     * A lambda can carry neither Javadoc nor {@code @StepDoc}: the block comment written above
     * the call that declares the step documents it, with the syntax of Javadoc.
     *
     * @param lambdaParameters empty when the call is not given a lambda
     */
    static MethodDocumentation ofLambda(Optional<Comment> commentAbove, NodeList<Parameter> lambdaParameters) {
        MethodJavadoc javadoc = commentAbove.map(JavadocReader::readComment).orElse(MethodJavadoc.NONE);
        return merge(javadoc, StepDocValues.NONE, lambdaParameters);
    }

    private static MethodDocumentation merge(MethodJavadoc javadoc, StepDocValues stepDoc,
                                             NodeList<Parameter> methodParameters) {
        List<ParamDoc> parameters = new ArrayList<>();
        for (int i = 0; i < methodParameters.size(); i++) {
            ParamDoc fromStepDoc = i < stepDoc.params().size() ? stepDoc.params().get(i) : UNDOCUMENTED;
            String fromJavadoc = javadoc.parameterDescriptions()
                    .getOrDefault(methodParameters.get(i).getNameAsString(), "");
            parameters.add(new ParamDoc(firstNonBlank(fromStepDoc.description(), fromJavadoc), fromStepDoc.examples()));
        }
        String description = firstNonBlank(stepDoc.description(), javadoc.description());
        return new MethodDocumentation(description, parameters, stepDoc.params().size());
    }

    /** Documentation of the parameter at {@code index}, empty for an argument the method does not declare. */
    ParamDoc parameter(int index) {
        return index < parameters.size() ? parameters.get(index) : UNDOCUMENTED;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred.isBlank() ? fallback : preferred;
    }
}
