package com.exodius_software.stepdoc.core.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exodius_software.stepdoc.core.model.ExpressionSyntax;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.model.StepKind;
import com.exodius_software.stepdoc.core.model.StepParameter;

/** Format-independent layout decisions, so Markdown and HTML always present the same thing. */
final class CatalogLayout {

    /**
     * A titled block of steps.
     *
     * @param subtitle extra context under the title, empty when there is none
     */
    record Section(String title, String subtitle, List<StepDefinition> steps) {
    }

    private CatalogLayout() {
    }

    static List<Section> sections(StepCatalog catalog, GroupBy groupBy) {
        return switch (groupBy) {
            case CLASS -> sectionsByClass(catalog.steps());
            case KEYWORD -> sectionsByKeyword(catalog.steps());
        };
    }

    /** Classes in order of first appearance; inside a class, Given before When before Then. */
    private static List<Section> sectionsByClass(List<StepDefinition> steps) {
        Map<String, List<StepDefinition>> stepsByClass = new LinkedHashMap<>();
        for (StepDefinition step : steps) {
            stepsByClass.computeIfAbsent(step.declaringClass(), key -> new ArrayList<>()).add(step);
        }
        List<Section> sections = new ArrayList<>();
        stepsByClass.forEach((declaringClass, classSteps) -> {
            List<StepDefinition> byKeyword = new ArrayList<>(classSteps);
            byKeyword.sort(Comparator.comparing(StepDefinition::kind)); // stable: source order kept per kind
            String title = simpleName(declaringClass);
            String subtitle = title.equals(declaringClass) ? "" : declaringClass;
            sections.add(new Section(title, subtitle, byKeyword));
        });
        return sections;
    }

    private static List<Section> sectionsByKeyword(List<StepDefinition> steps) {
        Map<StepKind, List<StepDefinition>> stepsByKind = new EnumMap<>(StepKind.class);
        for (StepDefinition step : steps) {
            stepsByKind.computeIfAbsent(step.kind(), key -> new ArrayList<>()).add(step);
        }
        List<Section> sections = new ArrayList<>();
        stepsByKind.forEach((kind, kindSteps) -> sections.add(new Section(kindTitle(kind), "", kindSteps)));
        return sections;
    }

    /**
     * Type shown next to a parameter: the Cucumber Expression placeholder, since that is what
     * test writers type ({@code {}} included), or the Java type for a regex group. Empty for
     * a regex group the method does not declare: there is no type to show.
     */
    static String typeLabel(StepDefinition step, StepParameter parameter) {
        return switch (parameter.source()) {
            case DATA_TABLE -> "data table";
            case DOC_STRING -> "doc string";
            case EXPRESSION -> step.syntax() == ExpressionSyntax.CUCUMBER_EXPRESSION
                    ? "{" + parameter.expressionType() + "}"
                    : parameter.javaType();
        };
    }

    static String summary(StepCatalog catalog, List<Section> sections) {
        return count(catalog.steps().size(), "step") + " in " + count(sections.size(), "section") + ".";
    }

    private static String count(int quantity, String noun) {
        return quantity + " " + noun + (quantity == 1 ? "" : "s");
    }

    private static String kindTitle(StepKind kind) {
        return switch (kind) {
            case GIVEN -> "Given";
            case WHEN -> "When";
            case THEN -> "Then";
            case AND -> "And";
            case BUT -> "But";
            case OTHER -> "Other keywords";
        };
    }

    private static String simpleName(String qualifiedName) {
        return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }
}
