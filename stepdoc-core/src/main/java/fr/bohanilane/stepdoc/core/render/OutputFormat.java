package fr.bohanilane.stepdoc.core.render;

import fr.bohanilane.stepdoc.core.util.EnumNames;

/** Supported documentation formats. */
public enum OutputFormat {

    MARKDOWN("md", new MarkdownRenderer()),
    HTML("html", new HtmlRenderer());

    private final String fileExtension;
    private final CatalogRenderer renderer;

    OutputFormat(String fileExtension, CatalogRenderer renderer) {
        this.fileExtension = fileExtension;
        this.renderer = renderer;
    }

    public String fileExtension() {
        return fileExtension;
    }

    public CatalogRenderer renderer() {
        return renderer;
    }

    /** Case-insensitive lookup, for build tool configuration. */
    public static OutputFormat fromName(String name) {
        return EnumNames.fromName(OutputFormat.class, name, "format");
    }
}
