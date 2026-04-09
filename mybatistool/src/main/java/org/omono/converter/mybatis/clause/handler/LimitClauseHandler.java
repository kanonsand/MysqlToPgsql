package org.omono.converter.mybatis.clause.handler;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import org.omono.converter.mybatis.clause.ClauseContext;

/**
 * Handler for LIMIT clause.
 * MySQL and PostgreSQL LIMIT syntax is mostly compatible,
 * but this handler provides a place for any necessary conversions.
 * 
 * MySQL: LIMIT offset, count  or LIMIT count OFFSET offset
 * PostgreSQL: LIMIT count OFFSET offset
 */
public class LimitClauseHandler implements ClauseHandler {
    
    @Override
    public void process(Object clause, ClauseContext ctx) {
        if (clause == null || !(clause instanceof SQLLimit)) {
            return;
        }
        
        SQLLimit limit = (SQLLimit) clause;
        limit.accept(new LimitClauseVisitor(ctx));
    }
    
    /**
     * Visitor for LIMIT clause.
     */
    private static class LimitClauseVisitor extends MySqlASTVisitorAdapter {
        private final ClauseContext ctx;
        
        LimitClauseVisitor(ClauseContext ctx) {
            this.ctx = ctx;
        }
        
        @Override
        public boolean visit(SQLLimit x) {
            // PostgreSQL and MySQL LIMIT syntax is mostly compatible
            // LIMIT count [OFFSET offset]
            
            // Process the row count (LIMIT n)
            SQLExpr rowCount = x.getRowCount();
            if (rowCount != null) {
                processLimitValue(rowCount);
            }
            
            // Process the offset (OFFSET n)
            SQLExpr offset = x.getOffset();
            if (offset != null) {
                processLimitValue(offset);
            }
            
            return true;
        }
        
        private void processLimitValue(SQLExpr expr) {
            // Ensure numeric literals are properly formatted
            // PostgreSQL accepts integer literals directly
            if (expr instanceof SQLIntegerExpr) {
                // Already an integer, no conversion needed
            } else if (expr instanceof SQLNumberExpr) {
                // Decimal number - could round or leave as-is
                // PostgreSQL accepts decimal in LIMIT
            }
            // Note: If expr is a parameter placeholder (?), it stays as-is
        }
    }
}
