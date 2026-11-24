package com.exodius_software.stepdoc.core.parser;

import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;

/** Finds annotation attributes in the syntax tree. {@link StringConstants} reads their values. */
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

    /** Elements of an array value; Java allows a single element to be written without braces. */
    static List<Expression> elements(Expression expression) {
        return expression instanceof ArrayInitializerExpr array ? array.getValues() : List.of(expression);
    }
}
