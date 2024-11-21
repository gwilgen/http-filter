package org.gbm.filter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class FilterUrlReader {

    @Getter
    String filter;

    final HttpServletRequest request;
    final ApplicationContext ctx;

    @PostConstruct
    public void setFilterCriteria() {
        filter = request.getParameter("filter");
        if (null == filter || filter.isBlank()) {
            return;
        }
        filter = URLDecoder.decode(filter, Charset.defaultCharset());
    }
}
