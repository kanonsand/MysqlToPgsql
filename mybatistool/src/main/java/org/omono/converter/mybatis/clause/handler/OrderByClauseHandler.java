package org.omono.converter.mybatis.clause.handler;

import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.SQLExpr;
import org.omono.converter.mybatis.clause.ClauseContext;

/**
 * Handler for ORDER BY clause.
 * Processes column references and applies quoting.
 */
public class OrderByClauseHandler implements ClauseHandler {
    
    @Override
    public void process(Object clause, ClauseContext ctx) {
        if (clause == null || !(clause instanceof SQLOrderBy)) {
            return;
        }
        
        ((SQLOrderBy) clause).accept(new OrderByVisitor(ctx));
    }
    
    private static class OrderByVisitor extends MySqlASTVisitorAdapter {
        private final ClauseContext ctx;
        
        OrderByVisitor(ClauseContext ctx) {
            this.ctx = ctx;
        }
        
        @Override
        public boolean visit(SQLIdentifierExpr x) {
            if (!ctx.isQuoteIdentifiers()) {
                return true;
            }
            
            String name = x.getName();
            if (name != null && !WhereClauseHandler.isPlaceholder(name)) {
                // Simple column name without table alias
                String quotedName = ctx.convertAndQuoteColumnName(name);
                x.setName(quotedName);
            }
            return true;
        }
        
        @Override
        public boolean visit(SQLPropertyExpr x) {
            if (!ctx.isQuoteIdentifiers()) {
                return true;
            }
            
            String colName = x.getName();
            if (colName != null) {
                // Extract table alias from owner
                SQLExpr owner = x.getOwner();
                String tableAlias = null;
                if (owner instanceof SQLIdentifierExpr) {
                    tableAlias = ((SQLIdentifierExpr) owner).getName();
                }
                
                // Qualified column name with table alias
                String quotedName = ctx.convertAndQuoteColumnName(colName, tableAlias);
                x.setName(quotedName);
                
                // Quote the table alias
                if (tableAlias != null) {
                    String quotedTable = ctx.convertAndQuoteTableName(tableAlias);
                    ((SQLIdentifierExpr) owner).setName(quotedTable);
                }
            }
            return true;
        }
    }
}