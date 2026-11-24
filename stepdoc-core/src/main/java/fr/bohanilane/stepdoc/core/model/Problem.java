package fr.bohanilane.stepdoc.core.model;

import java.util.Objects;

/**
 * Something found while parsing that the user should fix: an unparsable file,
 * a step whose parameters do not match its method, and so on.
 * Parsing never stops on a problem, so one bad file does not hide the others.
 */
public record Problem(SourceLocation location, String message) {

    public Problem {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(message, "message");
    }
}
