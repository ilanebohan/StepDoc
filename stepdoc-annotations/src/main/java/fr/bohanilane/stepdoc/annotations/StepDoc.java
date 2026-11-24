package fr.bohanilane.stepdoc.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional enrichment of a Cucumber step definition's documentation.
 *
 * <p>Javadoc remains the default source of documentation. Use this annotation only
 * when you need what Javadoc cannot express cleanly, such as example values.
 * It is retained in source only, so it has no runtime footprint.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface StepDoc {

    /** Description of what the step does. Overrides the Javadoc summary when set. */
    String value() default "";

    /**
     * Parameter documentation, in method parameter order. A trailing data table or
     * doc string argument is documented the same way. Takes precedence over {@code @param}.
     */
    Param[] params() default {};

    /** Documentation of a single step parameter. */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({})
    @interface Param {

        /** What the parameter represents. */
        String description();

        /** Example values, shown as-is in the generated documentation. */
        String[] examples() default {};
    }
}
