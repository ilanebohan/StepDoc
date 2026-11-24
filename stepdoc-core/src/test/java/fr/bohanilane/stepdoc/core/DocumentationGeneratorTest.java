package fr.bohanilane.stepdoc.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.bohanilane.stepdoc.core.DocumentationGenerator.OutputSettings;
import fr.bohanilane.stepdoc.core.render.OutputFormat;
import fr.bohanilane.stepdoc.core.render.RenderOptions;

class DocumentationGeneratorTest {

    @TempDir
    Path workspace;

    @Test
    void writesOneFilePerRequestedFormatIntoACreatedDirectory() throws IOException {
        Path sources = Files.createDirectories(workspace.resolve("src"));
        Files.writeString(sources.resolve("CartSteps.java"), """
                import io.cucumber.java.en.When;
                class CartSteps { @When("I add a book") void add() {} }
                """);
        Path output = workspace.resolve("target/stepdoc");

        DocumentationGenerator.Result result = new DocumentationGenerator().generate(sources,
                new OutputSettings(output, "catalog", Set.of(OutputFormat.HTML, OutputFormat.MARKDOWN)),
                RenderOptions.defaults());

        assertThat(result.writtenFiles())
                .containsExactly(output.resolve("catalog.md"), output.resolve("catalog.html"));
        assertThat(Files.readString(output.resolve("catalog.md"))).contains("`I add a book`");
        assertThat(Files.readString(output.resolve("catalog.html"))).contains("I add a book");
        assertThat(result.catalog().steps()).hasSize(1);
    }

    @Test
    void writesOnlyTheSelectedFormat() throws IOException {
        Path sources = Files.createDirectories(workspace.resolve("src"));
        Path output = workspace.resolve("out");

        new DocumentationGenerator().generate(sources,
                new OutputSettings(output, "steps", Set.of(OutputFormat.HTML)), RenderOptions.defaults());

        try (var files = Files.list(output)) {
            assertThat(files).containsExactly(output.resolve("steps.html"));
        }
    }
}
