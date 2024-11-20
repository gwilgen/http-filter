package org.gbm.filter;

import lombok.Builder;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.gbm.filter.FilterParser;
import org.gbm.filter.FilterLexer;

@Builder
public class FilterCriteria {

    final static ANTLRErrorListener errorListener = new FilterErrorListener();
    FilterParser.ExpressionContext ctx;
    public static FilterCriteria from(String filter) {
        if (null == filter) {
            return FilterCriteria.builder().build();
        }
        FilterLexer lexer = new FilterLexer(CharStreams.fromString(filter));
        lexer.addErrorListener(errorListener);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FilterParser p = new FilterParser(tokens);
        p.addErrorListener(errorListener);
        p.setBuildParseTree(true);
        return FilterCriteria.builder().ctx(p.expression()).build();
    }

    @Override
    public boolean equals(Object o) {
        if (null == o) {
            return false;
        }
        if (!(o instanceof FilterCriteria fc)) {
            return false;
        }
        return ctx.getText().equals(fc.ctx.getText());
    }
}
