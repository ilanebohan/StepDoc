package com.exodius_software.stepdoc.core.render;

import com.exodius_software.stepdoc.core.model.StepCatalog;

/**
 * Turns a catalog into a document. Output only depends on the catalog and the options
 * (no date, no machine-specific path), so regenerating unchanged sources changes nothing.
 */
public interface CatalogRenderer {

    String render(StepCatalog catalog, RenderOptions options);
}
