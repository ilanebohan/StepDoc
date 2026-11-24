package com.exodius_software.stepdoc.core.parser;

import static java.util.stream.Collectors.joining;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.javadoc.description.JavadocDescriptionElement;
import com.github.javaparser.javadoc.description.JavadocInlineTag;

import com.exodius_software.stepdoc.core.util.Whitespace;

/** Turns Javadoc into plain text: one string per paragraph, inline tags unwrapped. */
final class JavadocReader {

    record MethodJavadoc(String description, Map<String, String> parameterDescriptions) {

        static final MethodJavadoc NONE = new MethodJavadoc("", Map.of());
    }

    private static final Pattern PARAGRAPH_TAG = Pattern.compile("(?i)</?p\\s*/?>");
    private static final Pattern BLANK_LINE = Pattern.compile("\\n\\s*\\n");

    private JavadocReader() {
    }

    static MethodJavadoc read(MethodDeclaration method) {
        return method.getJavadoc().map(JavadocReader::toMethodJavadoc).orElse(MethodJavadoc.NONE);
    }

    /**
     * Reads a block comment with the syntax of Javadoc, whether it opens with one star or two.
     * A line comment is not read: of several lines in a row, only the last one belongs to the
     * code that follows.
     */
    static MethodJavadoc readComment(Comment comment) {
        if (comment instanceof JavadocComment javadoc) {
            return toMethodJavadoc(javadoc.parse());
        }
        if (comment instanceof BlockComment block) {
            return toMethodJavadoc(StaticJavaParser.parseJavadoc(block.getContent(), false));
        }
        return MethodJavadoc.NONE;
    }

    private static MethodJavadoc toMethodJavadoc(Javadoc javadoc) {
        Map<String, String> parameterDescriptions = new HashMap<>();
        for (JavadocBlockTag tag : javadoc.getBlockTags()) {
            if (tag.getType() == JavadocBlockTag.Type.PARAM) {
                tag.getName().ifPresent(name -> parameterDescriptions.put(name, plainText(tag.getContent())));
            }
        }
        return new MethodJavadoc(plainText(javadoc.getDescription()), parameterDescriptions);
    }

    private static String plainText(JavadocDescription description) {
        StringBuilder raw = new StringBuilder();
        for (JavadocDescriptionElement element : description.getElements()) {
            raw.append(element instanceof JavadocInlineTag tag ? inlineTagText(tag) : element.toText());
        }
        return normalizeParagraphs(raw.toString());
    }

    /** {@code {@code x}} becomes {@code x}; {@code {@link Cart#add the cart}} becomes its label. */
    private static String inlineTagText(JavadocInlineTag tag) {
        String content = tag.getContent().trim();
        boolean isLink = tag.getType() == JavadocInlineTag.Type.LINK
                || tag.getType() == JavadocInlineTag.Type.LINKPLAIN;
        if (!isLink) {
            return content;
        }
        int labelStart = content.indexOf(' ');
        return labelStart < 0 ? content : content.substring(labelStart + 1).trim();
    }

    /** Collapses whitespace inside paragraphs; blank lines and {@code <p>} separate paragraphs. */
    private static String normalizeParagraphs(String text) {
        String withBreaks = PARAGRAPH_TAG.matcher(text).replaceAll("\n\n");
        return BLANK_LINE.splitAsStream(withBreaks)
                .map(Whitespace::collapse)
                .filter(paragraph -> !paragraph.isEmpty())
                .collect(joining("\n\n"));
    }
}
