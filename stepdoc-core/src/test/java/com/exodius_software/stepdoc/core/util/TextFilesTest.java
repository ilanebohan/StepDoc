package com.exodius_software.stepdoc.core.util;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TextFilesTest {

    private static final String CSS = "/* caf\u00e9 */ h1 { color: teal; }";

    @TempDir
    Path directory;

    @Test
    void readsTheFileInTheGivenEncoding() throws IOException {
        Path file = Files.write(directory.resolve("style.css"), CSS.getBytes(ISO_8859_1));

        assertThat(TextFiles.read(file, ISO_8859_1, "htmlStylesheet")).isEqualTo(CSS);
    }

    @Test
    void blamesTheSettingWhenTheFileDoesNotExist() {
        Path missing = directory.resolve("missing.css");

        assertThatThrownBy(() -> TextFiles.read(missing, UTF_8, "htmlStylesheet"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("htmlStylesheet points to " + missing + ", which does not exist");
    }

    @Test
    void blamesTheSettingWhenTheFileIsNotValidInTheEncoding() throws IOException {
        Path file = Files.write(directory.resolve("style.css"), CSS.getBytes(ISO_8859_1));

        assertThatThrownBy(() -> TextFiles.read(file, UTF_8, "htmlStylesheet"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("htmlStylesheet points to " + file + ", which is not valid UTF-8");
    }
}