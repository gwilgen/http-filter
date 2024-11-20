package org.gbm.filter;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Operator;
import com.querydsl.core.types.Template;
import com.querydsl.core.types.Templates;
import com.querydsl.core.types.dsl.Expressions;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Component
@Lazy
@Slf4j
public class ExpressionBuilder {

    Map<String, Function<Object[], Expression<?>>> map = new HashMap<>();
    Pattern funcPattern = Pattern.compile("[a-zA-Z_]([a-zA-Z0-9_.])*");

    @PostConstruct
    public void populate() {
        Reflections r = new Reflections("com.querydsl");
        List<? extends Operator> operators = r.getSubTypesOf(Operator.class).stream()
                .flatMap(cls -> Arrays.stream(cls.getFields())
                        .filter(f -> Operator.class.isAssignableFrom(f.getType()))
                        .map(f -> {
                            try {
                                // all fields are enums by convention
                                return (Operator) f.get(null);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }))
                .toList();
        List<? extends Templates> templates = r.getSubTypesOf(Templates.class).stream()
                .map(ts -> {
                    try {
                        // all classes have a static DEFAULT instance as convention
                        return (Templates) ts.getField("DEFAULT").get(null);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
        templates.forEach(ts -> {
            operators.forEach(op -> {
                Template t = ts.getTemplate(op);
                if (null == t) {
                    return;
                }
                Field f = ReflectionUtils.findField(t.getClass(), "template", String.class);
                assert f != null;
                f.setAccessible(true);
                String expr = (String) ReflectionUtils.getField(f, t);
                assert expr != null;
                String func = expr.split("\\(")[0].trim();
                if (!funcPattern.matcher(func).matches()) {
                    log.trace("Skipping non func: {}", expr);
                    return;
                }
                map.put(func, createExpression(op, t));
            });
        });
    }

    @SuppressWarnings("unchecked")
    protected Function<Object[], Expression<?>> createExpression(Operator op, Template t) {
        if (Number.class.isAssignableFrom(op.getType())) {
            return args -> Expressions.numberTemplate(Double.class, t, args);
        } else if (Boolean.class.isAssignableFrom(op.getType())) {
            return args -> Expressions.booleanTemplate(t, args);
        } else if (Comparable.class.isAssignableFrom(op.getType())) {
            // best effort...
            return args -> Expressions.comparableTemplate((Class<? extends Comparable<?>>) op.getType(), t, args);
        } else if (Object.class.equals(op.getType())) {
            log.trace("Skipping Object expression: {}", op);
        } else {
            log.trace("Unhandled expression: {}", op);
        }
        return null;
    }

    Function<Object[], Expression<?>> get(String function) {
        Function<Object[], Expression<?>> result = map.get(function);
        if (null == result) {
            throw new UnsupportedOperationException(function);
        }
        return result;
    }
}
