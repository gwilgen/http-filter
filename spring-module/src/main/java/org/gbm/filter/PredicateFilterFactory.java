package org.gbm.filter;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PredicateFilterFactory {

    ApplicationContext ctx;

    public Predicate from(Class<? extends EntityPathBase<?>> refClass, String filter) {
        if (null == filter) {
            return new BooleanBuilder();
        }
        return ctx.getBean(PredicateFilterVisitor.class).withRefClass(refClass).visit(FilterCriteria.from(filter).ctx);
    }
}
