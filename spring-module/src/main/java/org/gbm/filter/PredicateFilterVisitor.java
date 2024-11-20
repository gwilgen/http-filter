package org.gbm.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.StringExpression;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.gbm.filter.properties.FilterHelper;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class PredicateFilterVisitor extends org.gbm.filter.FilterBaseVisitor<Predicate> {

    static final ObjectMapper objMapper = new ObjectMapper();

    final FilterHelper filterHelper;
    final ExpressionBuilder expressionBuilder;

    EntityPathBase<?> refInstance;

    public PredicateFilterVisitor withRefClass(Class<? extends EntityPathBase<?>> refClass) {
        try {
            refInstance = (EntityPathBase<?>) Arrays.stream(refClass.getFields())
                    .filter(f -> refClass.isAssignableFrom(f.getType()))
                    .filter(f -> Modifier.isStatic(f.getModifiers()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find singleton instance of " + refClass))
                    .get(null);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Could not create PredicateFieldVisitor", e);
        }
    }

    @Override
    public Predicate visit(ParseTree parseTree) {
        return switch (parseTree) {
            case org.gbm.filter.FilterParser.ParenthesisContext parenthesisContext -> visitParenthesis(
                    parenthesisContext);
            case org.gbm.filter.FilterParser.OperationContext operationContext -> visitOperation(operationContext);
            case org.gbm.filter.FilterParser.ComparationContext comparationContext -> visitComparation(
                    comparationContext);
            case org.gbm.filter.FilterParser.NegationContext negationContext -> visitNegation(negationContext);
            case null -> throw new UnsupportedOperationException("Missing parseTree");
            default -> throw new UnsupportedOperationException("Unsupported expression: " + parseTree.getClass());
        };
    }

    public Predicate visitOperation(org.gbm.filter.FilterParser.OperationContext ctx) {
        ParseTree argA = ctx.getChild(0);
        ParseTree expr = ctx.getChild(1);
        ParseTree argB = ctx.getChild(2);
        return switch (expr.getText()) {
            case "and" -> new BooleanBuilder(visit(argA)).and(visit(argB));
            case "or" -> new BooleanBuilder(visit(argA)).or(visit(argB));
            default -> throw new UnsupportedOperationException("operation not implemented: " + expr.getText());
        };
    }

    public Predicate visitParenthesis(org.gbm.filter.FilterParser.ParenthesisContext ctx) {
        return visit(ctx.getChild(1));
    }

    Expression<?> extract(ParseTree eval) {
        if (eval instanceof org.gbm.filter.FilterParser.FieldContext) {
            try {
                String field = eval.getText();
                return (SimpleExpression<?>)
                        refInstance.getClass().getField(field).get(refInstance);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Could not obtain field '%s'", eval.getText()), e);
            }
        }
        String function = eval.getChild(0).getText();
        Object[] args = new Object[(eval.getChildCount() - 2) / 2];
        for (int i = 0; i < args.length; i++) {
            ParseTree arg = eval.getChild(i * 2 + 2);
            Object o = null;
            if (arg instanceof org.gbm.filter.FilterParser.TextContext
                    || arg instanceof org.gbm.filter.FilterParser.NumberContext) {
                o = coerce(
                        arg.getText(),
                        arg instanceof org.gbm.filter.FilterParser.TextContext ? String.class : Double.class);
            } else {
                o = extract(arg);
            }
            args[i] = o;
        }
        return expressionBuilder.get(function).apply(args);
    }

    Object coerce(String value, Class<?> type) {
        try {
            if (!List.of(String.class, Date.class).contains(type) && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            if (Date.class.equals(type)) {
                return filterHelper.getDateFormat().parse(value);
            }
            return objMapper.readValue(value, type);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not coerce '%s' to %s", value, type.getSimpleName()), e);
        }
    }

    @SuppressWarnings("unchecked")
    <T> Predicate compareFieldToLiteral(ParseTree eval, ParseTree expr, String literal) {
        SimpleExpression<T> curr = (SimpleExpression<T>) extract(eval);
        Expression<T> value = (Expression<T>) ConstantImpl.create(coerce(literal, curr.getType()));
        return switch (expr.getText()) {
            case "eq" -> curr.eq(value);
            case "ne" -> curr.eq(value).not();
            case "lt" -> Expressions.booleanOperation(Ops.LT, curr, value);
            case "gt" -> Expressions.booleanOperation(Ops.GT, curr, value);
            case "le" -> Expressions.booleanOperation(Ops.LOE, curr, value);
            case "ge" -> Expressions.booleanOperation(Ops.GOE, curr, value);
            case "in" -> ((StringExpression) value).contains((Expression<String>) curr);
            default -> throw new UnsupportedOperationException(expr.getText());
        };
    }

    @SuppressWarnings("unchecked")
    <T> Predicate compareLiteralToField(String literal, ParseTree expr, ParseTree eval) {
        SimpleExpression<T> curr = (SimpleExpression<T>) extract(eval);
        Expression<T> value = (Expression<T>) ConstantImpl.create(coerce(literal, curr.getType()));
        return switch (expr.getText()) {
            case "eq" -> curr.eq(value);
            case "ne" -> curr.eq(value).not();
            case "lt" -> Expressions.booleanOperation(Ops.LT, value, curr);
            case "gt" -> Expressions.booleanOperation(Ops.GT, value, curr);
            case "le" -> Expressions.booleanOperation(Ops.LOE, value, curr);
            case "ge" -> Expressions.booleanOperation(Ops.GOE, value, curr);
            case "in" -> ((StringExpression) curr).contains((Expression<String>) value);
            default -> throw new UnsupportedOperationException(expr.getText());
        };
    }

    @Override
    public Predicate visitNegation(org.gbm.filter.FilterParser.NegationContext ctx) {
        return visit(ctx.getChild(0)).not();
    }

    @Override
    public Predicate visitComparation(org.gbm.filter.FilterParser.ComparationContext ctx) {
        ParseTree argA = ctx.getChild(0);
        ParseTree expr = ctx.getChild(1);
        ParseTree argB = ctx.getChild(2);
        try {
            if ((argA instanceof org.gbm.filter.FilterParser.FieldContext
                            || argA instanceof org.gbm.filter.FilterParser.FunctionContext)
                    && (argB instanceof org.gbm.filter.FilterParser.TextContext
                            || argB instanceof org.gbm.filter.FilterParser.NumberContext)) {
                return compareFieldToLiteral(argA, expr, argB.getText());
            } else if ((argA instanceof org.gbm.filter.FilterParser.TextContext
                            || argA instanceof org.gbm.filter.FilterParser.NumberContext)
                    && (argB instanceof org.gbm.filter.FilterParser.FieldContext
                            || argB instanceof org.gbm.filter.FilterParser.FunctionContext)) {
                return compareLiteralToField(argA.getText(), expr, argB);
            } else {
                // TODO: support 'name contains city'?
                throw new UnsupportedOperationException();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not evaluate expression: " + ctx.getText(), e);
        }
    }
}
