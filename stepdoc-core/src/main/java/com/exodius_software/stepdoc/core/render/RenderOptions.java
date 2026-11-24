package com.exodius_software.stepdoc.core.render;

import java.util.Objects;

/**
 * What the generated documentation shows. Shared by every format; a format ignores
 * what it cannot express ({@code extraCss} only applies to HTML).
 *
 * @param title              document title
 * @param groupBy            how steps are split into sections
 * @param showSourceLocation adds a column with the file and line of each step
 * @param showProblems       adds a section listing what the parser could not handle
 * @param extraCss           CSS appended after the default HTML styles, empty for none
 */
public record RenderOptions(
        String title,
        GroupBy groupBy,
        boolean showSourceLocation,
        boolean showProblems,
        String extraCss) {

    public static final String DEFAULT_TITLE = "Step catalog";

    public RenderOptions {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(groupBy, "groupBy");
        Objects.requireNonNull(extraCss, "extraCss");
    }

    public static RenderOptions defaults() {
        return new RenderOptions(DEFAULT_TITLE, GroupBy.CLASS, true, true, "");
    }
}
