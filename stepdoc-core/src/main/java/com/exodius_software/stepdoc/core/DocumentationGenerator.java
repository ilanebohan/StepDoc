package com.exodius_software.stepdoc.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.exodius_software.stepdoc.core.model.StepCatalog;
import com.exodius_software.stepdoc.core.parser.StepDefinitionParser;
import com.exodius_software.stepdoc.core.render.OutputFormat;
import com.exodius_software.stepdoc.core.render.RenderOptions;

/** Entry point for build tools: scan sources, write one file per requested format. */
public final class DocumentationGenerator {

    /**
     * Where and how the documentation is written.
     *
     * @param directory created if missing
     * @param baseName  file name without extension, each format adds its own
     * @param formats   at least one
     */
    public record OutputSettings(Path directory, String baseName, Set<OutputFormat> formats) {

        public OutputSettings {
            Objects.requireNonNull(directory, "directory");
            Objects.requireNonNull(baseName, "baseName");
            if (baseName.isBlank()) {
                throw new IllegalArgumentException("Output file name must not be blank");
            }
            if (formats.isEmpty()) {
                throw new IllegalArgumentException("At least one output format is required");
            }
            formats = Set.copyOf(formats);
        }
    }

    /** What was generated, so the caller can report it. */
    public record Result(StepCatalog catalog, List<Path> writtenFiles) {

        public Result {
            writtenFiles = List.copyOf(writtenFiles);
        }
    }

    private final StepDefinitionParser parser = new StepDefinitionParser();

    public Result generate(SourceSettings sources, OutputSettings output, RenderOptions options) throws IOException {
        StepCatalog catalog = parser.parseDirectories(sources.roots(), sources.encoding());
        Files.createDirectories(output.directory());
        List<Path> writtenFiles = new ArrayList<>();
        for (OutputFormat format : EnumSet.copyOf(output.formats())) { // enum order: stable output listing
            Path file = output.directory().resolve(output.baseName() + "." + format.fileExtension());
            Files.writeString(file, format.renderer().render(catalog, options));
            writtenFiles.add(file);
        }
        return new Result(catalog, writtenFiles);
    }
}
