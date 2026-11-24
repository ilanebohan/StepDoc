package com.exodius_software.stepdoc.core.render;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;

import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.model.StepParameter;
import com.exodius_software.stepdoc.core.render.CatalogLayout.Section;

/**
 * GitHub Flavored Markdown, also understood by GitLab. Each section is a table;
 * multi-line content uses {@code <br>} since a table cell cannot hold a real line break.
 */
final class MarkdownRenderer implements CatalogRenderer {

    private static final String LINE_BREAK = "<br>";

    @Override
    public String render(StepCatalog catalog, RenderOptions options) {
        List<Section> sections = CatalogLayout.sections(catalog, options.groupBy());
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(singleLine(options.title())).append("\n\n");
        markdown.append(CatalogLayout.summary(catalog, sections)).append("\n");
        for (Section section : sections) {
            appendSection(markdown, section, options);
        }
        if (options.showProblems() && !catalog.problems().isEmpty()) {
            appendProblems(markdown, catalog.problems());
        }
        return markdown.toString();
    }

    private static void appendSection(StringBuilder markdown, Section section, RenderOptions options) {
        markdown.append("\n## ").append(singleLine(section.title())).append("\n\n");
        if (!section.subtitle().isEmpty()) {
            markdown.append(codeSpan(section.subtitle())).append("\n\n");
        }
        List<String> headers = new ArrayList<>(List.of("Keyword", "Step", "Description", "Parameters"));
        if (options.showSourceLocation()) {
            headers.add("Source");
        }
        markdown.append(row(headers));
        markdown.append(row(headers.stream().map(header -> "---").toList()));
        for (StepDefinition step : section.steps()) {
            List<String> cells = new ArrayList<>(List.of(
                    step.keyword(),
                    codeSpan(step.expression()),
                    text(step.description()),
                    step.parameters().stream()
                            .map(parameter -> parameter(step, parameter))
                            .collect(joining(LINE_BREAK))));
            if (options.showSourceLocation()) {
                cells.add(codeSpan(step.location().toString()));
            }
            markdown.append(row(cells));
        }
    }

    /** {@code **quantity** `{int}` number of copies (e.g. `1`, `12`)} */
    private static String parameter(StepDefinition step, StepParameter parameter) {
        List<String> parts = new ArrayList<>();
        if (!parameter.name().isEmpty()) {
            parts.add("**" + text(parameter.name()) + "**");
        }
        String type = CatalogLayout.typeLabel(step, parameter);
        if (!type.isEmpty()) {
            parts.add(codeSpan(type));
        }
        if (!parameter.description().isEmpty()) {
            parts.add(text(parameter.description()));
        }
        if (!parameter.examples().isEmpty()) {
            String examples = parameter.examples().stream().map(MarkdownRenderer::codeSpan).collect(joining(", "));
            parts.add("(e.g. " + examples + ")");
        }
        return String.join(" ", parts);
    }

    private static void appendProblems(StringBuilder markdown, List<Problem> problems) {
        markdown.append("\n## Problems\n\n");
        for (Problem problem : problems) {
            markdown.append("- ").append(codeSpan(problem.location().toString()))
                    .append(" ").append(text(problem.message())).append("\n");
        }
    }

    private static String row(List<String> cells) {
        return "| " + String.join(" | ", cells) + " |\n";
    }

    /** Plain text in a table cell: no pipe ending the cell, no HTML interpreted, line breaks kept. */
    private static String text(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("|", "\\|")
                .replace("\n", LINE_BREAK);
    }

    /**
     * Inline code. Entities are not decoded inside code spans, so only the pipe is escaped
     * (GFM requires it even there). A value containing backticks gets a longer fence.
     */
    private static String codeSpan(String value) {
        String content = singleLine(value).replace("|", "\\|");
        return content.contains("`") ? "`` " + content + " ``" : "`" + content + "`";
    }

    private static String singleLine(String value) {
        return value.replace('\n', ' ').replace('\r', ' ');
    }
}
