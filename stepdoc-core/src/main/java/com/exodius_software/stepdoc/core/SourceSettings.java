package com.exodius_software.stepdoc.core;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * What is scanned and how it is read.
 *
 * @param roots    directories holding the step definition sources, each searched recursively
 *                 and all read as one set of sources
 * @param encoding encoding of the source files
 */
public record SourceSettings(List<Path> roots, Charset encoding) {

    public SourceSettings {
        roots = List.copyOf(roots);
        Objects.requireNonNull(encoding, "encoding");
    }

    /**
     * For build tool configuration, where the encoding is a name and may be missing.
     *
     * @param encodingName a charset name; {@code null} or blank means UTF-8, never the default
     *                     of the machine, so that a build gives the same result everywhere
     * @throws IllegalArgumentException when no such charset exists
     */
    public static SourceSettings of(List<Path> roots, String encodingName) {
        if (encodingName == null || encodingName.isBlank()) {
            return new SourceSettings(roots, StandardCharsets.UTF_8);
        }
        try {
            return new SourceSettings(roots, Charset.forName(encodingName.trim()));
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new IllegalArgumentException("Unknown encoding '" + encodingName + "'", e);
        }
    }
}
