package org.gbm.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;
import org.gbm.filter.method.FilterMethodsProvider;

@AllArgsConstructor
public class BooleanFilterVisitor extends org.gbm.filter.FilterBaseVisitor<Boolean> {

    static final ObjectMapper objMapper = new ObjectMapper();

    Object obj;

    @Override
    public Boolean visit(ParseTree parseTree) {
        return switch (parseTree) {
            case FilterParser.ParenthesisContext parenthesisContext -> visitParenthesis(parenthesisContext);
            case FilterParser.OperationContext operationContext -> visitOperation(operationContext);
            case FilterParser.ComparationContext comparationContext -> visitComparation(comparationContext);
            case FilterParser.NegationContext negationContext -> visitNegation(negationContext);
            case null -> throw new UnsupportedOperationException("Missing parseTree");
            default -> throw new UnsupportedOperationException("Unsupported expression: " + parseTree.getClass());
        };
    }

    @Override
    public Boolean visitOperation(FilterParser.OperationContext ctx) {
        ParseTree argA = ctx.getChild(0);
        ParseTree expr = ctx.getChild(1);
        ParseTree argB = ctx.getChild(2);
        return switch (expr.getText()) {
            case "and" -> visit(argA) && visit(argB);
            case "or" -> visit(argA) || visit(argB);
            default -> throw new UnsupportedOperationException("operation not implemented: " + expr.getText());
        };
    }

    @Override
    public Boolean visitParenthesis(FilterParser.ParenthesisContext ctx) {
        return visit(ctx.getChild(1));
    }

    Method getMethod(String field) throws Exception {
        return obj.getClass().getMethod("get" + StringUtils.capitalize(field));
    }

    Object coerce(String value, Class<?> type) {
        try {
            if (!String.class.equals(type) && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return objMapper.readValue(value, type);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not coerce '%s' to %s", value, type.getSimpleName()), e);
        }
    }

    Object extract(ParseTree eval) {
        if (eval instanceof FilterParser.FieldContext) {
            try {
                return getMethod(eval.getText()).invoke(obj);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Could not obtain field '%s'", eval.getText()), e);
            }
        }
        String function = eval.getChild(0).getText();
        Method method = Optional.ofNullable(FilterMethodsProvider.get(function))
                .orElseThrow(() -> new RuntimeException("Unimplemented method: " + function));
        Object[] args = new Object[(eval.getChildCount() - 2) / 2];
        for (int i = 0; i < args.length; i++) {
            ParseTree arg = eval.getChild(i * 2 + 2);
            Object o = null;
            if (arg instanceof FilterParser.FieldContext) {
                o = extract(arg);
            } else if (arg instanceof FilterParser.TextContext || arg instanceof FilterParser.NumberContext) {
                o = coerce(arg.getText(), method.getParameterTypes()[i]);
            } else {
                // TODO: nested functions
                throw new UnsupportedOperationException();
            }
            args[i] = o;
        }
        try {
            return method.invoke(null, args);
        } catch (Exception e) {
            throw new RuntimeException("Could not call method: " + function, e);
        }
    }

    boolean compareFieldToLiteral(ParseTree eval, ParseTree expr, String literal) {
        Object curr = extract(eval);
        Object value = coerce(literal, curr.getClass());
        return switch (expr.getText()) {
            case "eq" -> Objects.equals(curr, value);
            case "ne" -> !Objects.equals(curr, value);
            case "lt" -> ((Number) curr).doubleValue() < ((Number) value).doubleValue();
            case "gt" -> ((Number) curr).doubleValue() > ((Number) value).doubleValue();
            case "le" -> ((Number) curr).doubleValue() <= ((Number) value).doubleValue();
            case "ge" -> ((Number) curr).doubleValue() >= ((Number) value).doubleValue();
            case "in" -> value.toString().toUpperCase().contains(curr.toString().toUpperCase());
            default -> throw new UnsupportedOperationException();
        };
    }

    boolean compareLiteralToField(String literal, ParseTree expr, ParseTree eval) {
        Object curr = extract(eval);
        Object value = coerce(literal, curr.getClass());
        return switch (expr.getText()) {
            case "eq" -> Objects.equals(curr, value);
            case "ne" -> !Objects.equals(curr, value);
            case "lt" -> ((Number) value).doubleValue() < ((Number) curr).doubleValue();
            case "gt" -> ((Number) value).doubleValue() > ((Number) curr).doubleValue();
            case "le" -> ((Number) value).doubleValue() <= ((Number) curr).doubleValue();
            case "ge" -> ((Number) value).doubleValue() >= ((Number) curr).doubleValue();
            case "in" -> curr.toString().toUpperCase().contains(value.toString().toUpperCase());
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public Boolean visitNegation(FilterParser.NegationContext ctx) {
        return !visit(ctx.getChild(0));
    }

    @Override
    public Boolean visitComparation(FilterParser.ComparationContext ctx) {
        ParseTree argA = ctx.getChild(0);
        ParseTree expr = ctx.getChild(1);
        ParseTree argB = ctx.getChild(2);
        try {
            if ((argA instanceof FilterParser.FieldContext || argA instanceof FilterParser.FunctionContext)
                    && (argB instanceof FilterParser.TextContext || argB instanceof FilterParser.NumberContext)) {
                return compareFieldToLiteral(argA, expr, argB.getText());
            } else if ((argA instanceof FilterParser.TextContext || argA instanceof FilterParser.NumberContext)
                    && (argB instanceof FilterParser.FieldContext || argB instanceof FilterParser.FunctionContext)) {
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
