package com.exodius_software.stepdoc.core.util;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads the text files a setting points to. Internal helper, not part of the public API. */
public final class TextFiles {

    private TextFiles() {
    }

    /**
     * @param settingName name of the setting that points to {@code file}, used in the error message
     * @throws IllegalArgumentException when the file does not exist or is not valid in
     *                                  {@code encoding}: the setting is wrong, the disk is fine
     */
    public static String read(Path file, Charset encoding, String settingName) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(settingName + " points to " + file + ", which does not exist");
        }
        try {
            return Files.readString(file, encoding);
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException(
                    settingName + " points to " + file + ", which is not valid " + encoding.name(), e);
        }
    }
}
