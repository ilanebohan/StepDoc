package com.exodius_software.stepdoc.core.render;

import static com.exodius_software.stepdoc.core.render.TestSteps.catalog;
import static com.exodius_software.stepdoc.core.render.TestSteps.expressionParameter;
import static com.exodius_software.stepdoc.core.render.TestSteps.step;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.exodius_software.stepdoc.core.model.StepKind;

class HtmlRendererTest {

    private final CatalogRenderer renderer = OutputFormat.HTML.renderer();

    @Test
    void escapesEverythingComingFromTheSources() {
        String html = renderer.render(catalog(
                step(StepKind.WHEN, "I search for \"<script>alert(1)</script>\"",
                        "Finds <img src=x onerror=alert(1)> books", "shop.SearchSteps", 7,
                        expressionParameter("query", "String", "string", "a 'quoted' & raw value", "<b>"))),
                new RenderOptions("Shop <steps>", GroupBy.CLASS, true, true, ""));

        assertThat(html)
                .doesNotContain("<script>alert", "<img", "<b>", "<steps>")
                .contains("<title>Shop &lt;steps&gt;</title>")
                .contains("<code class=\"expression\">I search for &quot;&lt;script&gt;alert(1)&lt;/script&gt;&quot;</code>")
                .contains("<p>Finds &lt;img src=x onerror=alert(1)&gt; books</p>")
                .contains("a &#39;quoted&#39; &amp; raw value");
    }

    @Test
    void rendersParametersWithTypeDescriptionAndExamples() {
        String html = renderer.render(catalog(
                step(StepKind.WHEN, "I add {int} books", "", "shop.CartSteps", 3,
                        expressionParameter("quantity", "int", "int", "number of copies", "1", "12"))),
                RenderOptions.defaults());

        assertThat(html).contains("<li><span class=\"name\">quantity</span> <code>{int}</code> number of copies "
                + "<span class=\"examples\">e.g. <code>1</code>, <code>12</code></span></li>");
    }

    @Test
    void appendsCustomCssWithoutLettingItCloseTheStyleElement() {
        RenderOptions options = new RenderOptions("Steps", GroupBy.CLASS, true, true,
                "h1 { color: teal; }</style><script>alert(1)</script>");

        String html = renderer.render(catalog(step(StepKind.WHEN, "I add a book", "shop.CartSteps", 1)), options);

        assertThat(html).contains("h1 { color: teal; }<\\/style><script>alert(1)<\\/script>\n</style>");
        assertThat(html.indexOf("</style>")).isEqualTo(html.lastIndexOf("</style>"));
    }

    @Test
    void leavesOutTheSourceColumnWhenDisabled() {
        RenderOptions options = new RenderOptions("Steps", GroupBy.CLASS, false, true, "");

        String html = renderer.render(catalog(step(StepKind.WHEN, "I add a book", "shop.CartSteps", 1)), options);

        assertThat(html).doesNotContain("<th>Source</th>", "CartSteps.java");
    }

    @Test
    void showsNoFilterWhenThereIsNothingToDocument() {
        String html = renderer.render(catalog(), RenderOptions.defaults());

        assertThat(html).contains("No step definitions found.").doesNotContain("id=\"filter\"");
    }
}
