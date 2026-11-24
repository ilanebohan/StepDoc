package com.exodius_software.stepdoc.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SourceSettingsTest {

    private static final Path ROOT = Path.of("src/test/java");

    @Test
    void readsUtf8WhenTheProjectSetsNoEncoding() {
        assertThat(SourceSettings.of(ROOT, null).encoding()).isEqualTo(StandardCharsets.UTF_8);
        assertThat(SourceSettings.of(ROOT, " ").encoding()).isEqualTo(StandardCharsets.UTF_8);
    }

    @Test
    void acceptsACharsetNameWhateverItsCase() {
        assertThat(SourceSettings.of(ROOT, " iso-8859-1 ").encoding()).isEqualTo(StandardCharsets.ISO_8859_1);
    }

    @Test
    void rejectsANameThatIsNotACharset() {
        assertThatThrownBy(() -> SourceSettings.of(ROOT, "latin-42"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown encoding 'latin-42'");
        assertThatThrownBy(() -> SourceSettings.of(ROOT, "not a charset"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown encoding 'not a charset'");
    }
}