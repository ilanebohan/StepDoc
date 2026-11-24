package fr.bohanilane.stepdoc.core.util;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Locale;

/**
 * Spelling of enum constants in user configuration: lower case, words separated by dashes
 * ({@code UNDOCUMENTED_STEP} is {@code undocumented-step}). Lookup ignores case and
 * surrounding spaces. Internal helper, not part of the public API.
 */
public final class EnumNames {

    private EnumNames() {
    }

    /**
     * @param settingName name of the setting being read, used in the error message
     * @throws IllegalArgumentException listing the accepted values when {@code name} matches none
     */
    public static <E extends Enum<E>> E fromName(Class<E> type, String name, String settingName) {
        String normalized = name.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown " + settingName + " '" + name
                        + "', expected one of: " + validNames(type)));
    }

    public static String idOf(Enum<?> constant) {
        return constant.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static <E extends Enum<E>> String validNames(Class<E> type) {
        return Arrays.stream(type.getEnumConstants()).map(EnumNames::idOf).collect(joining(", "));
    }
}
