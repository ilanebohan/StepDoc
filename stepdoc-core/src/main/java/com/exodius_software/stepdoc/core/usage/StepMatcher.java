package com.exodius_software.stepdoc.core.usage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.usage.FeatureFiles.FeatureStep;

import io.cucumber.cucumberexpressions.CucumberExpressionException;
import io.cucumber.cucumberexpressions.Expression;
import io.cucumber.cucumberexpressions.ExpressionFactory;
import io.cucumber.cucumberexpressions.ParameterType;
import io.cucumber.cucumberexpressions.ParameterTypeRegistry;
import io.cucumber.cucumberexpressions.Transformer;
import io.cucumber.cucumberexpressions.UndefinedParameterTypeException;

/**
 * Tells whether a step definition matches a step of a feature file, with Cucumber's own
 * expression library, so the answer is the one Cucumber would give at run time.
 */
final class StepMatcher {

    private static final String MATCHES_ANYTHING = ".*";
    private static final Transformer<String> AS_TEXT = text -> text;

    private final Map<String, List<String>> textsByLanguage = new LinkedHashMap<>();
    private final Map<String, Expressions> expressionsByLanguage = new HashMap<>();

    StepMatcher(Collection<FeatureStep> featureSteps) {
        for (FeatureStep step : featureSteps) {
            textsByLanguage.computeIfAbsent(step.language(), language -> new ArrayList<>()).add(step.text());
        }
    }

    /**
     * @throws CucumberExpressionException when the expression of the step cannot be compiled
     *                                     or applied, in which case its usage is unknown
     */
    boolean isUsed(StepDefinition step) {
        for (Map.Entry<String, List<String>> texts : textsByLanguage.entrySet()) {
            Expression expression = expressionsByLanguage
                    .computeIfAbsent(texts.getKey(), Expressions::new)
                    .compile(step.expression());
            if (texts.getValue().stream().anyMatch(text -> expression.match(text).isPresent())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compiles expressions for one Gherkin language. Cucumber reads numbers the way the
     * language of the feature file writes them: {@code 1.000,50} is a number in German,
     * not in English.
     */
    private static final class Expressions {

        private final ParameterTypeRegistry parameterTypes;
        private final ExpressionFactory factory;
        private final Set<String> assumedTypes = new HashSet<>();

        Expressions(String language) {
            parameterTypes = new ParameterTypeRegistry(Locale.forLanguageTag(language));
            factory = new ExpressionFactory(parameterTypes);
        }

        Expression compile(String expression) {
            while (true) {
                try {
                    return factory.createExpression(expression);
                } catch (UndefinedParameterTypeException undefined) {
                    assume(undefined);
                }
            }
        }

        /**
         * A custom parameter type is defined in the user's code, which StepDoc never runs, so
         * its regex is unknown. It is assumed to match anything: that can hide an unused step,
         * but never calls a used one unused.
         */
        private void assume(UndefinedParameterTypeException undefined) {
            String name = undefined.getUndefinedParameterTypeName();
            if (!assumedTypes.add(name)) {
                throw undefined; // already assumed and still rejected: do not loop forever
            }
            parameterTypes.defineParameterType(
                    new ParameterType<>(name, MATCHES_ANYTHING, String.class, AS_TEXT, false, false));
        }
    }
}
