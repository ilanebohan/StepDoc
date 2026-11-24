package com.exodius_software.stepdoc.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lists the files of a directory tree the same way on every machine: paths relative to the
 * root, with {@code /} separators, in a stable order. Internal helper, not part of the
 * public API.
 */
public final class FileTree {

    /**
     * @param path         where to read the file
     * @param relativePath path from the root, the one shown to users
     */
    public record Entry(Path path, String relativePath) {
    }

    private FileTree() {
    }

    /** Files under {@code root} whose name ends with {@code extension}, recursively, sorted by relative path. */
    public static List<Entry> filesWithExtension(Path root, String extension) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(extension))
                    .map(path -> new Entry(path, root.relativize(path).toString().replace('\\', '/')))
                    .sorted(Comparator.comparing(Entry::relativePath))
                    .toList();
        }
    }
}
