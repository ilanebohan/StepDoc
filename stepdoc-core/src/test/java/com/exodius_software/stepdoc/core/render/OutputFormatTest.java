package com.exodius_software.stepdoc.core.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OutputFormatTest {

    @Test
    void acceptsNamesWhateverTheirCase() {
        assertThat(OutputFormat.fromName(" html ")).isEqualTo(OutputFormat.HTML);
        assertThat(OutputFormat.fromName("Markdown")).isEqualTo(OutputFormat.MARKDOWN);
    }

    @Test
    void rejectsAnUnknownNameListingTheValidOnes() {
        assertThatThrownBy(() -> OutputFormat.fromName("pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown format 'pdf', expected one of: markdown, html");
    }
}
