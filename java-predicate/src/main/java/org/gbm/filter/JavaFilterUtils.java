package org.gbm.filter;

import java.util.Optional;
import java.util.function.Predicate;

public class JavaFilterUtils {

    public static <T> Predicate<T> asPredicate(FilterCriteria filterCriteria) {
        if (null == filterCriteria.ctx) {
            return x -> true;
        }
        return o -> matches(filterCriteria, o);
    }

    protected static <T> boolean matches(FilterCriteria filterCriteria, T obj) {
        FilterBaseVisitor<Boolean> visitor = new BooleanFilterVisitor(obj);
        return Optional.ofNullable(visitor.visit(filterCriteria.ctx)).orElse(false);
    }
}
