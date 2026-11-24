package com.exodius_software.stepdoc.core.util;

import java.util.regex.Pattern;

/** Internal helper, not part of the public API. */
public final class Whitespace {

    private static final Pattern RUN = Pattern.compile("\\s+");

    private Whitespace() {
    }

    /** Puts {@code text} on one line: every run of spaces and line breaks becomes a single space. */
    public static String collapse(String text) {
        return RUN.matcher(text).replaceAll(" ").trim();
    }
}
