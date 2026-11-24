package com.exodius_software.stepdoc.core.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.exodius_software.stepdoc.core.model.StepKind;

class GherkinKeywordsTest {

    private static final Pattern STEP_ANNOTATION_CLASS =
            Pattern.compile("(io/cucumber/java/[a-z_]+)/([^/$]+)\\.class");

    /**
     * Guards the naming rules copied from cucumber-java: when a Cucumber upgrade adds a language
     * or changes how annotations are named, this test fails and names what is missing.
     */
    @Test
    void knowsEveryStepAnnotationOfCucumberJava() throws IOException, URISyntaxException {
        List<String> annotations = stepAnnotationsOfCucumberJava();

        assertThat(annotations).contains("io.cucumber.java.en.Given", "io.cucumber.java.de.Wenn");
        assertThat(annotations)
                .filteredOn(annotation -> kindOf(annotation).isEmpty())
                .as("cucumber-java step annotations unknown to GherkinKeywords")
                .isEmpty();
    }

    @Test
    void givesEachKeywordItsGherkinKind() {
        assertThat(GherkinKeywords.kindOf("io.cucumber.java.en", "Given")).contains(StepKind.GIVEN);
        assertThat(GherkinKeywords.kindOf("io.cucumber.java.en", "When")).contains(StepKind.WHEN);
        assertThat(GherkinKeywords.kindOf("io.cucumber.java.en", "Then")).contains(StepKind.THEN);
        assertThat(GherkinKeywords.kindOf("io.cucumber.java.en", "And")).contains(StepKind.AND);
        assertThat(GherkinKeywords.kindOf("io.cucumber.java.en", "But")).contains(StepKind.BUT);
    }

    @Test
    void namesAnnotationsLikeCucumberJava() {
        // "Gegeben sei": the space is dropped
        assertThat(GherkinKeywords.kindOf("io.cucumber.java.de", "Gegebensei")).contains(StepKind.GIVEN);
        // language "en-Scouse": lower case, dash replaced
        assertThat(GherkinKeywords.kindOf("io.cucumber.java.en_scouse", "Givun")).contains(StepKind.GIVEN);
    }

    @Test
    void mapsACucumberJava8InterfaceToTheAnnotationPackageOfItsLanguage() {
        assertThat(GherkinKeywords.packageOfLambdaInterface("En")).contains("io.cucumber.java.en");
        assertThat(GherkinKeywords.packageOfLambdaInterface("Zh_cn")).contains("io.cucumber.java.zh_cn");
        assertThat(GherkinKeywords.packageOfLambdaInterface("LambdaGlue")).isEmpty();
        assertThat(GherkinKeywords.packageOfLambdaInterface("en")).isEmpty();
    }

    @Test
    void knowsNoKeywordOutsideItsOwnLanguagePackage() {
        assertThat(GherkinKeywords.kindOf("io.cucumber.java.en", "Wenn")).isEmpty();
        assertThat(GherkinKeywords.kindOf("io.cucumber.java.en", "Before")).isEmpty();
        assertThat(GherkinKeywords.kindOf("io.cucumber.java", "Given")).isEmpty();
        assertThat(GherkinKeywords.kindOf("com.acme.workflow", "When")).isEmpty();
    }

    private static Optional<StepKind> kindOf(String qualifiedAnnotation) {
        int lastDot = qualifiedAnnotation.lastIndexOf('.');
        return GherkinKeywords.kindOf(qualifiedAnnotation.substring(0, lastDot), qualifiedAnnotation.substring(lastDot + 1));
    }

    /** Reads the jar as a file, so cucumber-java and its dependencies are never loaded. */
    private static List<String> stepAnnotationsOfCucumberJava() throws IOException, URISyntaxException {
        URL knownAnnotation = GherkinKeywordsTest.class.getClassLoader().getResource("io/cucumber/java/en/Given.class");
        JarURLConnection connection = (JarURLConnection) knownAnnotation.openConnection();
        try (JarFile jar = new JarFile(Path.of(connection.getJarFileURL().toURI()).toFile())) {
            return jar.stream()
                    .map(JarEntry::getName)
                    .map(STEP_ANNOTATION_CLASS::matcher)
                    .filter(Matcher::matches)
                    .filter(entry -> !entry.group(2).equals("package-info"))
                    .map(entry -> entry.group(1).replace('/', '.') + "." + entry.group(2))
                    .sorted()
                    .toList();
        }
    }
}
