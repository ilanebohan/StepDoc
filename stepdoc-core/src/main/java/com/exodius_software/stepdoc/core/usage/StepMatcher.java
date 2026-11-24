package com.exodius_software.stepdoc.core.usage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.exodius_software.stepdoc.core.model.ParameterTypeDefinition;
import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.usage.FeatureFiles.FeatureStep;
import com.exodius_software.stepdoc.core.util.Whitespace;

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

    private final Map<String, Expressions> expressionsByLanguage = new HashMap<>();
    private final List<ParameterTypeDefinition> parameterTypes;

    /**
     * @param parameterTypes the types the scanned project declares; any other custom type is
     *                       assumed to match anything
     */
    StepMatcher(List<ParameterTypeDefinition> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    /**
     * @throws CucumberExpressionException when the expression of the definition cannot be
     *                                     compiled or applied, in which case nothing is known
     */
    boolean matches(StepDefinition definition, FeatureStep step) {
        return expressionsByLanguage
                .computeIfAbsent(step.language(), language -> new Expressions(language, parameterTypes))
                .compile(definition.expression())
                .match(step.text())
                .isPresent();
    }

    /** The declared parameter types Cucumber itself would refuse, a name declared twice for instance. */
    List<Problem> parameterTypeProblems() {
        return new Expressions(Locale.ENGLISH.toLanguageTag(), parameterTypes).problems;
    }

    /**
     * Compiles expressions for one Gherkin language. Cucumber reads numbers the way the
     * language of the feature file writes them: {@code 1.000,50} is a number in German,
     * not in English.
     */
    private static final class Expressions {

        private final ParameterTypeRegistry registry;
        private final ExpressionFactory factory;
        private final Set<String> assumedTypes = new HashSet<>();
        private final Map<String, Expression> compiled = new HashMap<>();
        private final List<Problem> problems = new ArrayList<>();

        Expressions(String language, List<ParameterTypeDefinition> declaredTypes) {
            registry = new ParameterTypeRegistry(Locale.forLanguageTag(language));
            factory = new ExpressionFactory(registry);
            declaredTypes.forEach(this::declare);
        }

        private void declare(ParameterTypeDefinition type) {
            try {
                define(type.name(), type.regex());
            } catch (CucumberExpressionException e) {
                problems.add(new Problem(type.location(), "Parameter type '" + type.name()
                        + "' cannot be used: " + Whitespace.collapse(e.getMessage())));
            }
        }

        /** Compiled once: the same definition is matched against every step of the feature files. */
        Expression compile(String expression) {
            Expression known = compiled.get(expression);
            if (known != null) {
                return known;
            }
            Expression created = create(expression);
            compiled.put(expression, created);
            return created;
        }

        private Expression create(String expression) {
            while (true) {
                try {
                    return factory.createExpression(expression);
                } catch (UndefinedParameterTypeException undefined) {
                    assume(undefined);
                }
            }
        }

        /**
         * A type the scanned sources do not declare is defined elsewhere, in code StepDoc never
         * runs, so its regex is unknown. It is assumed to match anything: that can hide an
         * unused step, but never calls a used one unused.
         */
        private void assume(UndefinedParameterTypeException undefined) {
            String name = undefined.getUndefinedParameterTypeName();
            if (!assumedTypes.add(name)) {
                throw undefined; // already assumed and still rejected: do not loop forever
            }
            define(name, MATCHES_ANYTHING);
        }

        private void define(String name, String regex) {
            registry.defineParameterType(new ParameterType<>(name, regex, String.class, AS_TEXT, false, false));
        }
    }
}
