package com.exodius_software.stepdoc.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * What every StepDoc goal shares: where the steps are, how to skip, and how errors
 * reach Maven. A bad setting or an I/O error is an execution error (the build is broken);
 * only a goal finding a real issue in the steps reports a failure.
 */
abstract class AbstractStepDocMojo extends AbstractMojo {

    /** Root directory scanned for step definitions. */
    @Parameter(property = "stepdoc.sourceDirectory", defaultValue = "${project.build.testSourceDirectory}")
    private File sourceDirectory;

    /** Skips every StepDoc goal. */
    @Parameter(property = "stepdoc.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("StepDoc: skipped");
            return;
        }
        if (!sourceDirectory.isDirectory()) {
            getLog().info("StepDoc: " + sourceDirectory + " does not exist, nothing to do");
            return;
        }
        try {
            run(sourceDirectory.toPath());
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
    protected abstract void run(Path sourceRoot) throws IOException, MojoFailureException;
}
