package fr.bohanilane.stepdoc.core.parser;

import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;

/** Reads annotation attribute values straight from the syntax tree, without symbol resolution. */
final class AnnotationValues {

    private AnnotationValues() {
    }

    static Optional<Expression> member(AnnotationExpr annotation, String memberName) {
        if (annotation instanceof SingleMemberAnnotationExpr single) {
            return "value".equals(memberName) ? Optional.of(single.getMemberValue()) : Optional.empty();
        }
        if (annotation instanceof NormalAnnotationExpr normal) {
            return normal.getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals(memberName))
                    .map(MemberValuePair::getValue)
                    .findFirst();
        }
        return Optional.empty();
    }

    /**
     * Value of a string literal, text block or concatenation of those. Anything else,
     * a constant reference for instance, would need the classpath, so it is empty.
     */
    static Optional<String> constantString(Expression expression) {
        if (expression instanceof StringLiteralExpr literal) {
            return Optional.of(literal.asString());
        }
        if (expression instanceof TextBlockLiteralExpr textBlock) {
            return Optional.of(textBlock.asString());
        }
        if (expression instanceof EnclosedExpr enclosed) {
            return constantString(enclosed.getInner());
        }
        if (expression instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            return constantString(binary.getLeft())
                    .flatMap(left -> constantString(binary.getRight()).map(right -> left + right));
        }
        return Optional.empty();
    }

    /** Elements of an array value; Java allows a single element to be written without braces. */
    static List<Expression> elements(Expression expression) {
        return expression instanceof ArrayInitializerExpr array ? array.getValues() : List.of(expression);
    }
}
