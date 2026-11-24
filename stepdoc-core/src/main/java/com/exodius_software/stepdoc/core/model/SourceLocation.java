package com.exodius_software.stepdoc.core.model;

import java.util.Objects;

/**
 * Position of an element in the scanned sources.
 *
 * @param file path relative to the scanned root, always with {@code /} separators
 * @param line 1-based line number
 */
public record SourceLocation(String file, int line) {

    public SourceLocation {
        Objects.requireNonNull(file, "file");
    }

    @Override
    public String toString() {
        return file + ":" + line;
    }
}
