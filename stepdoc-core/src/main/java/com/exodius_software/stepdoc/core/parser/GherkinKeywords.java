package com.exodius_software.stepdoc.core.parser;

import static java.util.stream.Collectors.joining;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.exodius_software.stepdoc.core.model.StepKind;

import io.cucumber.gherkin.GherkinDialect;
import io.cucumber.gherkin.GherkinDialects;

/**
 * Knows the step annotations of cucumber-java for every Gherkin language, and the keyword
 * kind of each, without having cucumber-java on the classpath. The step methods of
 * cucumber-java8 follow the same names.
 *
 * <p>cucumber-java generates one package per Gherkin dialect and one annotation per step
 * keyword. This class applies the same naming rules to the dialects of the bundled Gherkin
 * library; a test checks the result against the real cucumber-java jar.
 */
final class GherkinKeywords {

    private static final String STEP_PACKAGE_PREFIX = "io.cucumber.java.";
    private static final String EMOJI_LANGUAGE = "em";

    /** Characters cucumber-java drops from a keyword to turn it into an annotation name. */
    private static final Pattern NOT_IN_ANNOTATION_NAMES = Pattern.compile("[\\s',!\u00AD\u2019]");
    private static final Pattern STARTS_WITH_DIGIT = Pattern.compile("\\d");
    private static final Pattern LANGUAGE_SEPARATOR = Pattern.compile("[\\s-]");

    private static final Map<String, Map<String, StepKind>> KINDS_BY_PACKAGE = kindsByPackage();

    private GherkinKeywords() {
    }

    /**
     * The kind of the step annotation {@code annotationName} of {@code packageName}, or empty
     * when that package has no such step annotation.
     */
    static Optional<StepKind> kindOf(String packageName, String annotationName) {
        Map<String, StepKind> kinds = KINDS_BY_PACKAGE.getOrDefault(packageName, Map.of());
        return Optional.ofNullable(kinds.get(Normalizer.normalize(annotationName, Normalizer.Form.NFC)));
    }

    /**
     * The cucumber-java package of the language that the cucumber-java8 interface
     * {@code interfaceName} declares steps in, or empty when there is no such interface.
     * cucumber-java8 names its interfaces after those packages, capitalised: {@code En},
     * {@code Zh_cn}, with methods named like the annotations of the package.
     */
    static Optional<String> packageOfLambdaInterface(String interfaceName) {
        String packageName = STEP_PACKAGE_PREFIX + interfaceName.toLowerCase(Locale.ROOT);
        boolean capitalised = Character.isUpperCase(interfaceName.codePointAt(0));
        return capitalised && KINDS_BY_PACKAGE.containsKey(packageName) ? Optional.of(packageName) : Optional.empty();
    }

    private static Map<String, Map<String, StepKind>> kindsByPackage() {
        Map<String, Map<String, StepKind>> kindsByPackage = new HashMap<>();
        for (GherkinDialect dialect : GherkinDialects.getDialects()) {
            Map<String, StepKind> kinds = new HashMap<>();
            register(kinds, dialect, dialect.getGivenKeywords(), StepKind.GIVEN);
            register(kinds, dialect, dialect.getWhenKeywords(), StepKind.WHEN);
            register(kinds, dialect, dialect.getThenKeywords(), StepKind.THEN);
            register(kinds, dialect, dialect.getAndKeywords(), StepKind.AND);
            register(kinds, dialect, dialect.getButKeywords(), StepKind.BUT);
            kindsByPackage.put(packageOf(dialect), kinds);
        }
        return kindsByPackage;
    }

    /** A keyword already registered keeps its kind: Given, When and Then win over And and But. */
    private static void register(Map<String, StepKind> kinds, GherkinDialect dialect, List<String> keywords,
                                 StepKind kind) {
        for (String keyword : keywords) {
            if (hasAnnotation(keyword)) {
                kinds.putIfAbsent(annotationName(dialect, keyword), kind);
            }
        }
    }

    /** cucumber-java generates no annotation for the {@code *} keyword nor for keywords starting with a digit. */
    private static boolean hasAnnotation(String keyword) {
        return !keyword.contains("*") && !STARTS_WITH_DIGIT.matcher(keyword).lookingAt();
    }

    private static String annotationName(GherkinDialect dialect, String keyword) {
        String identifier = EMOJI_LANGUAGE.equals(dialect.getLanguage()) ? emojiName(keyword) : keyword;
        String name = NOT_IN_ANNOTATION_NAMES.matcher(identifier).replaceAll("");
        return Normalizer.normalize(name, Normalizer.Form.NFC);
    }

    /** Emoji are not legal Java identifiers: cucumber-java uses their Unicode names, in title case. */
    private static String emojiName(String keyword) {
        return keyword.codePoints()
                .mapToObj(Character::getName)
                .flatMap(name -> Arrays.stream(name.split(" ")))
                .map(word -> word.substring(0, 1) + word.substring(1).toLowerCase(Locale.ROOT))
                .collect(joining(" "));
    }

    private static String packageOf(GherkinDialect dialect) {
        String language = LANGUAGE_SEPARATOR.matcher(dialect.getLanguage()).replaceAll("_");
        return STEP_PACKAGE_PREFIX + language.toLowerCase(Locale.ROOT);
    }
}
