package org.omono.converter.mybatis.clause.handler;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import org.omono.converter.mybatis.clause.ClauseContext;
import org.omono.converter.mybatis.clause.ColumnNameConverter;

/**
 * Handler for SELECT clause.
 * Processes column references and applies quoting/column name mapping.
 */
public class SelectClauseHandler implements ClauseHandler {
    
    @Override
    public void process(Object clause, ClauseContext ctx) {
        if (clause == null) {
            return;
        }
        
        if (clause instanceof Iterable) {
            for (Object item : (Iterable<?>) clause) {
                processSelectItem(item, ctx);
            }
        } else {
            processSelectItem(clause, ctx);
        }
    }
    
    private void processSelectItem(Object item, ClauseContext ctx) {
        if (!(item instanceof SQLSelectItem)) {
            return;
        }
        
        SQLSelectItem selectItem = (SQLSelectItem) item;
        SQLExpr expr = selectItem.getExpr();
        
        // Pass selectItem to visitor so it can check for existing alias
        expr.accept(new SelectColumnVisitor(ctx, selectItem));
    }
    
    /**
     * Visitor for processing column expressions in SELECT clause.
     */
    private static class SelectColumnVisitor extends MySqlASTVisitorAdapter {
        private final ClauseContext ctx;
        private final SQLSelectItem selectItem;
        
        SelectColumnVisitor(ClauseContext ctx, SQLSelectItem selectItem) {
            this.ctx = ctx;
            this.selectItem = selectItem;
        }
        
        @Override
        public boolean visit(SQLIdentifierExpr x) {
            String name = x.getName();
            if (name != null && !isPlaceholder(name)) {
                String cleanName = ColumnNameConverter.cleanIdentifier(name);
                String convertedName = ctx.convertColumnName(name);
                
                // Check if the select item already has an alias
                boolean hasExistingAlias = selectItem.getAlias() != null && !selectItem.getAlias().isEmpty();
                
                // Add alias only if column name is different AND no existing alias
                if (ctx.isQuoteIdentifiers() && ctx.needsAlias(cleanName) && !hasExistingAlias) {
                    String quotedName = ctx.quoteIdentifier(convertedName);
                    String originalQuoted = ctx.quoteIdentifier(cleanName);
                    x.setName(quotedName + " AS " + originalQuoted);
                } else if (ctx.isQuoteIdentifiers()) {
                    // Simple column name without table alias - just quote it
                    String quotedName = ctx.convertAndQuoteColumnName(name);
                    x.setName(quotedName);
                }
            }
            return true;
        }
        
        @Override
        public boolean visit(SQLPropertyExpr x) {
            String colName = x.getName();
            if (colName != null) {
                // Extract table alias from owner
                SQLExpr owner = x.getOwner();
                String tableAlias = null;
                if (owner instanceof SQLIdentifierExpr) {
                    tableAlias = ((SQLIdentifierExpr) owner).getName();
                }
                
                String cleanColName = ColumnNameConverter.cleanIdentifier(colName);
                String convertedName = ctx.convertColumnName(colName, tableAlias);
                
                // Check if the select item already has an alias
                boolean hasExistingAlias = selectItem.getAlias() != null && !selectItem.getAlias().isEmpty();
                
                // Add alias only if column name is different AND no existing alias
                if (ctx.isQuoteIdentifiers() && ctx.needsAlias(cleanColName) && !hasExistingAlias) {
                    String quotedName = ctx.quoteIdentifier(convertedName);
                    String originalQuoted = ctx.quoteIdentifier(cleanColName);
                    x.setName(quotedName + " AS " + originalQuoted);
                } else if (ctx.isQuoteIdentifiers()) {
                    // Qualified column name with table alias - just quote it
                    String quotedName = ctx.convertAndQuoteColumnName(colName, tableAlias);
                    x.setName(quotedName);
                }
                
                // Quote the table alias
                if (ctx.isQuoteIdentifiers() && owner instanceof SQLIdentifierExpr) {
                    String quotedTable = ctx.convertAndQuoteTableName(tableAlias);
                    ((SQLIdentifierExpr) owner).setName(quotedTable);
                }
            }
            return true;
        }
        
        private boolean isPlaceholder(String name) {
            return name.equals("?") || name.startsWith(":") || 
                   name.startsWith("#{") || name.startsWith("${");
        }
    }
}