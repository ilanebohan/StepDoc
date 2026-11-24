package fr.bohanilane.stepdoc.core.render;

import java.util.List;

import fr.bohanilane.stepdoc.core.model.ExpressionSyntax;
import fr.bohanilane.stepdoc.core.model.Problem;
import fr.bohanilane.stepdoc.core.model.SourceLocation;
import fr.bohanilane.stepdoc.core.model.StepCatalog;
import fr.bohanilane.stepdoc.core.model.StepDefinition;
import fr.bohanilane.stepdoc.core.model.StepKind;
import fr.bohanilane.stepdoc.core.model.StepParameter;

/** Builders for renderer tests, so each test only shows the values it is about. */
final class TestSteps {

    private TestSteps() {
    }

    static StepDefinition step(StepKind kind, String expression, String declaringClass, int line) {
        return step(kind, expression, "", declaringClass, line);
    }

    static StepDefinition step(StepKind kind, String expression, String description, String declaringClass,
                               int line, StepParameter... parameters) {
        String keyword = kind.name().charAt(0) + kind.name().substring(1).toLowerCase();
        String file = "steps/" + declaringClass.substring(declaringClass.lastIndexOf('.') + 1) + ".java";
        return new StepDefinition(keyword, kind, expression, ExpressionSyntax.CUCUMBER_EXPRESSION, description,
                List.of(parameters), declaringClass, "step" + line, new SourceLocation(file, line));
    }

    static StepParameter expressionParameter(String name, String javaType, String expressionType,
                                             String description, String... examples) {
        return new StepParameter(name, javaType, StepParameter.Source.EXPRESSION, expressionType, description,
                List.of(examples));
    }

    static StepParameter dataTable(String name) {
        return new StepParameter(name, "DataTable", StepParameter.Source.DATA_TABLE, "", "", List.of());
    }

    static StepCatalog catalog(StepDefinition... steps) {
        return new StepCatalog(List.of(steps), List.of());
    }

    static StepCatalog catalogWithProblem(String file, int line, String message, StepDefinition... steps) {
        return new StepCatalog(List.of(steps), List.of(new Problem(new SourceLocation(file, line), message)));
    }
}
