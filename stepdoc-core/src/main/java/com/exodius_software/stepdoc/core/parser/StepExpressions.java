package com.exodius_software.stepdoc.core.parser;

import static com.exodius_software.stepdoc.core.model.ExpressionSyntax.CUCUMBER_EXPRESSION;
import static com.exodius_software.stepdoc.core.model.ExpressionSyntax.REGULAR_EXPRESSION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exodius_software.stepdoc.core.model.ExpressionSyntax;

/** Reads step patterns the way Cucumber does, without depending on Cucumber itself. */
final class StepExpressions {

    private StepExpressions() {
    }

    static ExpressionSyntax syntaxOf(String expression) {
        boolean anchored = expression.startsWith("^") || expression.endsWith("$");
        boolean slashed = expression.length() >= 2 && expression.startsWith("/") && expression.endsWith("/");
        return anchored || slashed ? REGULAR_EXPRESSION : CUCUMBER_EXPRESSION;
    }

    /**
     * Types of the pattern's parameters, in order. Regex groups carry no type,
     * so each one comes back as an empty string.
     */
    static List<String> parameterTypes(String expression, ExpressionSyntax syntax) {
        return syntax == CUCUMBER_EXPRESSION
                ? cucumberParameterTypes(expression)
                : Collections.nCopies(countCapturingGroups(expression), "");
    }

    private static List<String> cucumberParameterTypes(String expression) {
        List<String> types = new ArrayList<>();
        StringBuilder currentType = null;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '\\') {
                i++; // escaped character, never a placeholder delimiter
            } else if (c == '{' && currentType == null) {
                currentType = new StringBuilder();
            } else if (c == '}' && currentType != null) {
                types.add(currentType.toString());
                currentType = null;
            } else if (currentType != null) {
                currentType.append(c);
            }
        }
        return types;
    }

    private static int countCapturingGroups(String regex) {
        int count = 0;
        boolean inCharacterClass = false;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '\\') {
                i++;
            } else if (inCharacterClass) {
                inCharacterClass = c != ']';
            } else if (c == '[') {
                inCharacterClass = true;
            } else if (c == '(' && isCapturingGroup(regex, i)) {
                count++;
            }
        }
        return count;
    }

    /** {@code (x)} and {@code (?<name>x)} capture; {@code (?:x)}, lookarounds and flags do not. */
    private static boolean isCapturingGroup(String regex, int openParenthesis) {
        int next = openParenthesis + 1;
        if (next >= regex.length() || regex.charAt(next) != '?') {
            return true;
        }
        return next + 2 < regex.length()
                && regex.charAt(next + 1) == '<'
                && Character.isLetter(regex.charAt(next + 2));
    }
}
