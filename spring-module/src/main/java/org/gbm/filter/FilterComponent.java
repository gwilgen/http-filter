package org.gbm.filter;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;

@Component
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class FilterComponent {

    FilterCriteria filterCriteria;
    @Getter
    String filter;
    final HttpServletRequest request;

    @PostConstruct
    public void setFilterCriteria() {
        filter = request.getParameter("filter");
        if (null == filter || filter.isBlank()) {
            return;
        }
        filter = URLDecoder.decode(filter, Charset.defaultCharset());
        filterCriteria = FilterCriteria.from(filter);
    }

    public Predicate toQuerydslPredicate(Class<? extends EntityPathBase<?>> refClass) {
        if (null == filterCriteria) {
            return new BooleanBuilder();
        }

        return new PredicateFilterVisitor(refClass).visit(filterCriteria.ctx);
    }

}
