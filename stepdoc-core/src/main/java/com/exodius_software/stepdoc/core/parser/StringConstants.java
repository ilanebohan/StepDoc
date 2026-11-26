package com.exodius_software.stepdoc.core.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;

/**
 * Reads string values from the syntax trees of the scanned sources: literals, text blocks,
 * concatenations, and the constants those sources declare.
 *
 * <p>A constant is found the way a reader of the code finds it: in the enclosing classes, then
 * through the imports, then in the package. Nothing is compiled, so a constant declared outside
 * the scanned sources (a dependency, another module) has no value here.
 */
final class StringConstants {

    /** Initializer of every constant, by {@code qualified.Type#NAME}. */
    private final Map<String, Expression> initializers;

    private StringConstants(Map<String, Expression> initializers) {
        this.initializers = initializers;
    }

    static StringConstants declaredIn(Collection<CompilationUnit> units) {
        Map<String, Expression> initializers = new HashMap<>();
        for (CompilationUnit unit : units) {
            for (FieldDeclaration field : unit.findAll(FieldDeclaration.class)) {
                declaringConstantType(field).ifPresent(type -> field.getVariables().forEach(variable ->
                        variable.getInitializer().ifPresent(initializer ->
                                initializers.put(key(type, variable.getNameAsString()), initializer))));
            }
        }
        return new StringConstants(initializers);
    }

    /** The value of {@code expression}, or empty when it cannot be known from the scanned sources. */
    Optional<String> valueOf(Expression expression) {
        return valueOf(expression, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private Optional<String> valueOf(Expression expression, Set<Expression> beingResolved) {
        if (expression instanceof StringLiteralExpr literal) {
            return Optional.of(literal.asString());
        }
        if (expression instanceof TextBlockLiteralExpr textBlock) {
            return Optional.of(textBlock.asString());
        }
        if (expression instanceof EnclosedExpr enclosed) {
            return valueOf(enclosed.getInner(), beingResolved);
        }
        if (expression instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            return valueOf(binary.getLeft(), beingResolved)
                    .flatMap(left -> valueOf(binary.getRight(), beingResolved).map(right -> left + right));
        }
        Optional<Expression> initializer = initializerOf(expression);
        if (initializer.isEmpty() || !beingResolved.add(initializer.get())) {
            return Optional.empty(); // not a constant, or a constant defined in terms of itself
        }
        Optional<String> value = valueOf(initializer.get(), beingResolved);
        beingResolved.remove(initializer.get());
        return value;
    }

    private Optional<Expression> initializerOf(Expression reference) {
        if (reference instanceof NameExpr name) {
            return firstDeclared(typesProviding(name), name.getNameAsString());
        }
        if (reference instanceof FieldAccessExpr access) {
            return firstDeclared(typesNamed(access.getScope().toString(), access), access.getNameAsString());
        }
        return Optional.empty();
    }

    private Optional<Expression> firstDeclared(List<String> types, String constant) {
        return types.stream()
                .map(type -> initializers.get(key(type, constant)))
                .filter(initializer -> initializer != null)
                .findFirst();
    }

    /** Types whose constants can be written without a qualifier: enclosing types, then static imports. */
    private static List<String> typesProviding(NameExpr name) {
        List<String> types = new ArrayList<>(enclosingTypes(name));
        for (ImportDeclaration declaration : importsOf(name)) {
            if (!declaration.isStatic()) {
                continue;
            }
            if (declaration.isAsterisk()) {
                types.add(declaration.getNameAsString());
            } else if (declaration.getName().getIdentifier().equals(name.getNameAsString())) {
                declaration.getName().getQualifier().ifPresent(type -> types.add(type.asString()));
            }
        }
        return types;
    }

    /**
     * Qualified names {@code typeName} may stand for where {@code usage} is written, most likely
     * first. {@code typeName} can be simple ({@code Phrases}), nested ({@code Phrases.Cart}) or
     * already qualified.
     */
    private static List<String> typesNamed(String typeName, Node usage) {
        String firstSegment = typeName.contains(".") ? typeName.substring(0, typeName.indexOf('.')) : typeName;
        String otherSegments = typeName.substring(firstSegment.length());
        List<String> candidates = new ArrayList<>();
        for (String enclosing : enclosingTypes(usage)) {
            if (enclosing.equals(firstSegment) || enclosing.endsWith("." + firstSegment)) {
                candidates.add(enclosing + otherSegments);
            }
            candidates.add(enclosing + "." + typeName);
        }
        for (ImportDeclaration declaration : importsOf(usage)) {
            if (!declaration.isStatic() && !declaration.isAsterisk()
                    && declaration.getName().getIdentifier().equals(firstSegment)) {
                candidates.add(declaration.getNameAsString() + otherSegments);
            }
        }
        candidates.add(packagePrefixOf(usage) + typeName);
        for (ImportDeclaration declaration : importsOf(usage)) {
            if (!declaration.isStatic() && declaration.isAsterisk()) {
                candidates.add(declaration.getNameAsString() + "." + typeName);
            }
        }
        candidates.add(typeName);
        return candidates;
    }

    /** Qualified names of the types around {@code node}, innermost first. */
    private static List<String> enclosingTypes(Node node) {
        List<String> types = new ArrayList<>();
        Optional<Node> ancestor = node.getParentNode();
        while (ancestor.isPresent()) {
            if (ancestor.get() instanceof TypeDeclaration<?> type) {
                type.getFullyQualifiedName().ifPresent(types::add);
            }
            ancestor = ancestor.get().getParentNode();
        }
        return types;
    }

    private static List<ImportDeclaration> importsOf(Node node) {
        return node.findCompilationUnit().map(unit -> List.copyOf(unit.getImports())).orElse(List.of());
    }

    private static String packagePrefixOf(Node node) {
        return node.findCompilationUnit()
                .flatMap(CompilationUnit::getPackageDeclaration)
                .map(declaration -> declaration.getNameAsString() + ".")
                .orElse("");
    }

    /**
     * The type declaring {@code field}, when the field is a constant: {@code static final},
     * or declared by an interface or an annotation, where every field is one.
     */
    private static Optional<String> declaringConstantType(FieldDeclaration field) {
        if (!(field.getParentNode().orElse(null) instanceof TypeDeclaration<?> type)) {
            return Optional.empty();
        }
        boolean alwaysConstant = type instanceof AnnotationDeclaration
                || type instanceof ClassOrInterfaceDeclaration declaration && declaration.isInterface();
        boolean constant = alwaysConstant || field.isStatic() && field.isFinal();
        return constant ? type.getFullyQualifiedName() : Optional.empty();
    }

    private static String key(String type, String constant) {
        return type + "#" + constant;
    }
}
