package com.exodius_software.stepdoc.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import com.exodius_software.stepdoc.core.SourceSettings;

/**
 * What every StepDoc goal shares: where the steps are, how to skip, and how errors
 * reach Maven. A bad setting or an I/O error is an execution error (the build is broken);
 * only a goal finding a real issue in the steps reports a failure.
 */
abstract class AbstractStepDocMojo extends AbstractMojo {

    /** Root directory scanned for step definitions. */
    @Parameter(property = "stepdoc.sourceDirectory", defaultValue = "${project.build.testSourceDirectory}")
    private File sourceDirectory;

    /**
     * Root directories scanned for step definitions, for a project that keeps them in several
     * places, another module of the build for instance. They are read as one set of sources.
     * When set, {@code sourceDirectory} is not used.
     */
    @Parameter(property = "stepdoc.sourceDirectories")
    private List<File> sourceDirectories;

    /**
     * Encoding of the step definition sources and of the files a setting points to, such as
     * {@code htmlStylesheet}. UTF-8 when the project sets none. Feature files follow the rule
     * of Gherkin instead: UTF-8, unless they start with an {@code # encoding:} comment.
     */
    @Parameter(property = "stepdoc.encoding", defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    /** Skips every StepDoc goal. */
    @Parameter(property = "stepdoc.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("StepDoc: skipped");
            return;
        }
        List<File> configured = sourceDirectories == null || sourceDirectories.isEmpty()
                ? List.of(sourceDirectory)
                : sourceDirectories;
        List<Path> roots = configured.stream().filter(File::isDirectory).map(File::toPath).toList();
        if (roots.isEmpty()) {
            getLog().info("StepDoc: " + configured + " not found, nothing to do");
            return;
        }
        configured.stream()
                .filter(directory -> !directory.isDirectory())
                .forEach(directory -> getLog().warn("StepDoc: " + directory + " not found, it was skipped"));
        try {
            run(SourceSettings.of(roots, encoding));
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("StepDoc configuration error: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException("StepDoc I/O error: " + e.getMessage(), e);
        }
    }

    /**
     * The goal itself, called only when there is something to work on.
     *
     * @throws IllegalArgumentException for an invalid setting
     */
    protected abstract void run(SourceSettings sources) throws IOException, MojoFailureException;
}
