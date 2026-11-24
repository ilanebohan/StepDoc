package fr.bohanilane.stepdoc.core.render;

import fr.bohanilane.stepdoc.core.util.EnumNames;

/** How steps are split into sections. */
public enum GroupBy {
    /** One section per step definition class, steps ordered by keyword. */
    CLASS,
    /** One section per keyword (Given, When, Then...), steps in source order. */
    KEYWORD;

    /** Case-insensitive lookup, for build tool configuration. */
    public static GroupBy fromName(String name) {
        return EnumNames.fromName(GroupBy.class, name, "groupBy");
    }
}
