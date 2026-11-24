package com.exodius_software.stepdoc.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.exodius_software.stepdoc.core.DocumentationGenerator;
import com.exodius_software.stepdoc.core.DocumentationGenerator.OutputSettings;
import com.exodius_software.stepdoc.core.model.Problem;
import com.exodius_software.stepdoc.core.render.GroupBy;
import com.exodius_software.stepdoc.core.render.OutputFormat;
import com.exodius_software.stepdoc.core.render.RenderOptions;

/**
 * Generates the step definition documentation of the current project.
 * Problems found in the sources are logged as warnings and never fail the build:
 * that is the job of {@code stepdoc:check}.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_TEST_SOURCES, threadSafe = true)
public class GenerateMojo extends AbstractStepDocMojo {

    /** Directory where the documentation is written. */
    @Parameter(property = "stepdoc.outputDirectory", defaultValue = "${project.build.directory}/stepdoc")
    private File outputDirectory;

    /** Output file name, without extension. */
    @Parameter(property = "stepdoc.fileName", defaultValue = "steps")
    private String fileName;

    /** Formats to generate: {@code markdown}, {@code html}, or both. */
    @Parameter(property = "stepdoc.formats", defaultValue = "markdown,html")
    private List<String> formats;

    /** Document title. */
    @Parameter(property = "stepdoc.title", defaultValue = RenderOptions.DEFAULT_TITLE)
    private String title;

    /** Sections: {@code class} (one per step class) or {@code keyword} (one per Given, When, Then...). */
    @Parameter(property = "stepdoc.groupBy", defaultValue = "class")
    private String groupBy;

    /** Adds a column with the file and line of each step. */
    @Parameter(property = "stepdoc.showSourceLocation", defaultValue = "true")
    private boolean showSourceLocation;

    /** Adds a section listing what could not be documented. */
    @Parameter(property = "stepdoc.showProblems", defaultValue = "true")
    private boolean showProblems;

    /** CSS file appended to the default styles of the HTML page. */
    @Parameter(property = "stepdoc.htmlStylesheet")
    private File htmlStylesheet;

    @Override
    protected void run(Path sourceRoot) throws IOException {
        DocumentationGenerator.Result result = new DocumentationGenerator().generate(
                sourceRoot,
                new OutputSettings(outputDirectory.toPath(), fileName, outputFormats()),
                renderOptions());
        for (Problem problem : result.catalog().problems()) {
            getLog().warn("StepDoc: " + problem.location() + " " + problem.message());
        }
        getLog().info("StepDoc: " + result.catalog().steps().size() + " step(s) documented in "
                + result.writtenFiles());
    }

    private Set<OutputFormat> outputFormats() {
        return formats.stream()
                .filter(format -> !format.isBlank())
                .map(OutputFormat::fromName)
                .collect(Collectors.toSet());
    }

    private RenderOptions renderOptions() throws IOException {
        String extraCss = htmlStylesheet == null ? "" : Files.readString(htmlStylesheet.toPath());
        return new RenderOptions(title, GroupBy.fromName(groupBy), showSourceLocation, showProblems, extraCss);
    }
}
