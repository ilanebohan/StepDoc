package com.exodius_software.stepdoc.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class SourceSettingsTest {

    private static final List<Path> ROOTS = List.of(Path.of("src/test/java"));

    @Test
    void readsUtf8WhenTheProjectSetsNoEncoding() {
        assertThat(SourceSettings.of(ROOTS, null).encoding()).isEqualTo(StandardCharsets.UTF_8);
        assertThat(SourceSettings.of(ROOTS, " ").encoding()).isEqualTo(StandardCharsets.UTF_8);
    }

    @Test
    void acceptsACharsetNameWhateverItsCase() {
        assertThat(SourceSettings.of(ROOTS, " iso-8859-1 ").encoding()).isEqualTo(StandardCharsets.ISO_8859_1);
    }

    @Test
    void rejectsANameThatIsNotACharset() {
        assertThatThrownBy(() -> SourceSettings.of(ROOTS, "latin-42"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown encoding 'latin-42'");
        assertThatThrownBy(() -> SourceSettings.of(ROOTS, "not a charset"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown encoding 'not a charset'");
    }
}