package fr.bohanilane.stepdoc.core.parser;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import fr.bohanilane.stepdoc.core.parser.JavadocReader.MethodJavadoc;
import fr.bohanilane.stepdoc.core.parser.StepDocReader.ParamDoc;
import fr.bohanilane.stepdoc.core.parser.StepDocReader.StepDocValues;

/**
 * Documentation of a step method, merged from its Javadoc and its optional {@code @StepDoc}.
 * When both describe the same thing, {@code @StepDoc} wins: it was written for the catalog.
 *
 * @param parameters            one entry per method parameter, in declaration order
 * @param stepDocParameterCount number of {@code @StepDoc.Param}, to detect extra ones
 */
record MethodDocumentation(String description, List<ParamDoc> parameters, int stepDocParameterCount) {

    private static final ParamDoc UNDOCUMENTED = new ParamDoc("", List.of());

    static MethodDocumentation of(MethodDeclaration method) {
        MethodJavadoc javadoc = JavadocReader.read(method);
        StepDocValues stepDoc = StepDocReader.read(method);

        NodeList<Parameter> methodParameters = method.getParameters();
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
