package com.exodius_software.stepdoc.core;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * What is scanned and how it is read.
 *
 * @param root     directory holding the step definition sources, searched recursively
 * @param encoding encoding of the source files
 */
public record SourceSettings(Path root, Charset encoding) {

    public SourceSettings {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(encoding, "encoding");
    }

    /**
     * For build tool configuration, where the encoding is a name and may be missing.
     *
     * @param encodingName a charset name; {@code null} or blank means UTF-8, never the default
     *                     of the machine, so that a build gives the same result everywhere
     * @throws IllegalArgumentException when no such charset exists
     */
    public static SourceSettings of(Path root, String encodingName) {
        if (encodingName == null || encodingName.isBlank()) {
            return new SourceSettings(root, StandardCharsets.UTF_8);
        }
        try {
            return new SourceSettings(root, Charset.forName(encodingName.trim()));
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new IllegalArgumentException("Unknown encoding '" + encodingName + "'", e);
        }
    }
}
