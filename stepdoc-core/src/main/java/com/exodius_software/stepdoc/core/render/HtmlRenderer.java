package com.exodius_software.stepdoc.core.render;

import java.util.List;
import java.util.Locale;

import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.model.StepDefinition;
import com.exodius_software.stepdoc.core.model.StepParameter;
import com.exodius_software.stepdoc.core.render.CatalogLayout.Section;

/**
 * A single self-contained page: styles and the filter script are inlined, nothing is
 * loaded from the network, so the file can be opened locally or published as is.
 * Every value coming from the scanned sources is escaped.
 */
final class HtmlRenderer implements CatalogRenderer {

    @Override
    public String render(StepCatalog catalog, RenderOptions options) {
        List<Section> sections = CatalogLayout.sections(catalog, options.groupBy());
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(escape(options.title())).append("</title>\n")
                .append("<style>\n").append(HtmlAssets.STYLE);
        if (!options.extraCss().isEmpty()) {
            html.append(safeInsideStyleElement(options.extraCss())).append('\n');
        }
        html.append("</style>\n</head>\n<body>\n<header>\n")
                .append("<h1>").append(escape(options.title())).append("</h1>\n")
                .append("<p class=\"summary\">")
                .append(sections.isEmpty() ? "No step definitions found." : escape(CatalogLayout.summary(catalog, sections)))
                .append("</p>\n");
        if (!sections.isEmpty()) {
            html.append("<input type=\"search\" id=\"filter\" placeholder=\"Filter steps\" aria-label=\"Filter steps\">\n");
        }
        html.append("</header>\n<main>\n");
        for (Section section : sections) {
            appendSection(html, section, options);
        }
        html.append("<p id=\"no-match\" hidden>No step matches the filter.</p>\n");
        if (options.showProblems() && !catalog.problems().isEmpty()) {
            appendProblems(html, catalog.problems());
        }
        html.append("</main>\n<script>\n").append(HtmlAssets.FILTER_SCRIPT).append("</script>\n</body>\n</html>\n");
        return html.toString();
    }

    private static void appendSection(StringBuilder html, Section section, RenderOptions options) {
        html.append("<section class=\"group\">\n<h2>").append(escape(section.title())).append("</h2>\n");
        if (!section.subtitle().isEmpty()) {
            html.append("<p class=\"subtitle\"><code>").append(escape(section.subtitle())).append("</code></p>\n");
        }
        html.append("<table>\n<thead><tr><th>Keyword</th><th>Step</th><th>Description</th><th>Parameters</th>");
        if (options.showSourceLocation()) {
            html.append("<th>Source</th>");
        }
        html.append("</tr></thead>\n<tbody>\n");
        for (StepDefinition step : section.steps()) {
            appendStep(html, step, options);
        }
        html.append("</tbody>\n</table>\n</section>\n");
    }

    private static void appendStep(StringBuilder html, StepDefinition step, RenderOptions options) {
        html.append("<tr>")
                .append("<td><span class=\"keyword keyword-").append(step.kind().name().toLowerCase(Locale.ROOT))
                .append("\">").append(escape(step.keyword())).append("</span></td>")
                .append("<td><code class=\"expression\">").append(escape(step.expression())).append("</code></td>")
                .append("<td>").append(paragraphs(step.description())).append("</td>")
                .append("<td>");
        if (!step.parameters().isEmpty()) {
            html.append("<ul class=\"parameters\">");
            step.parameters().forEach(parameter -> appendParameter(html, step, parameter));
            html.append("</ul>");
        }
        html.append("</td>");
        if (options.showSourceLocation()) {
            html.append("<td class=\"source\">").append(escape(step.location().toString())).append("</td>");
        }
        html.append("</tr>\n");
    }

    private static void appendParameter(StringBuilder html, StepDefinition step, StepParameter parameter) {
        html.append("<li>");
        if (!parameter.name().isEmpty()) {
            html.append("<span class=\"name\">").append(escape(parameter.name())).append("</span> ");
        }
        String type = CatalogLayout.typeLabel(step, parameter);
        if (!type.isEmpty()) {
            html.append("<code>").append(escape(type)).append("</code>");
        }
        if (!parameter.description().isEmpty()) {
            html.append(" ").append(escape(parameter.description()).replace("\n\n", "<br>"));
        }
        if (!parameter.examples().isEmpty()) {
            html.append(" <span class=\"examples\">e.g. ");
            for (int i = 0; i < parameter.examples().size(); i++) {
                html.append(i == 0 ? "" : ", ").append("<code>").append(escape(parameter.examples().get(i))).append("</code>");
            }
            html.append("</span>");
        }
        html.append("</li>");
    }

    private static void appendProblems(StringBuilder html, List<Problem> problems) {
        html.append("<section class=\"problems\">\n<h2>Problems</h2>\n<ul>\n");
        for (Problem problem : problems) {
            html.append("<li><code>").append(escape(problem.location().toString())).append("</code> ")
                    .append(escape(problem.message())).append("</li>\n");
        }
        html.append("</ul>\n</section>\n");
    }

    private static String paragraphs(String text) {
        if (text.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        for (String paragraph : text.split("\n\n")) {
            html.append("<p>").append(escape(paragraph)).append("</p>");
        }
        return html.toString();
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

    /**
     * Custom CSS is trusted project configuration, but a {@code </style>} inside it would still
     * end the element and turn the rest into markup. {@code <\/} keeps the same meaning in CSS.
     */
    private static String safeInsideStyleElement(String css) {
        return css.replace("</", "<\\/");
    }
}
