package fr.bohanilane.stepdoc.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates the step definition documentation of the current project.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_TEST_SOURCES, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    /** Root directory scanned for step definitions. */
    @Parameter(property = "stepdoc.sourceDirectory", defaultValue = "${project.build.testSourceDirectory}")
    private File sourceDirectory;

    /** Directory where the documentation is written. */
    @Parameter(property = "stepdoc.outputDirectory", defaultValue = "${project.build.directory}/stepdoc")
    private File outputDirectory;

    @Override
    public void execute() {
        getLog().info("StepDoc: scanning " + sourceDirectory + " -> " + outputDirectory + " (not implemented yet)");
    }
}
