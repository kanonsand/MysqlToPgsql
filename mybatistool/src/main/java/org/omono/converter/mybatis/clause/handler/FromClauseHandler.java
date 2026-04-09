package org.omono.converter.mybatis.clause.handler;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLLateralViewTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUnionQueryTableSource;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import org.omono.converter.mybatis.clause.ClauseContext;

/**
 * Handler for FROM clause.
 * Processes table sources, extracts alias mappings, and quotes table names.
 */
public class FromClauseHandler implements ClauseHandler {
    
    @Override
    public void process(Object clause, ClauseContext ctx) {
        if (clause == null || !(clause instanceof SQLTableSource)) {
            return;
        }
        
        SQLTableSource tableSource = (SQLTableSource) clause;
        
        // Extract aliases first
        extractAliases(tableSource, ctx);
        
        // Process for quoting (table names, aliases, and ON clause)
        processTableSource(tableSource, ctx);
    }
    
    /**
     * Extract table aliases from a table source.
     */
    private void extractAliases(SQLTableSource tableSource, ClauseContext ctx) {
        if (tableSource instanceof SQLExprTableSource) {
            SQLExprTableSource exprTable = (SQLExprTableSource) tableSource;
            String tableName = TableNameConverter.cleanIdentifier(exprTable.getTableName());
            String alias = exprTable.getAlias();
            
            if (alias != null && !alias.isEmpty()) {
                ctx.addAlias(TableNameConverter.cleanIdentifier(alias), tableName);
            } else {
                ctx.addAlias(tableName, tableName);
            }
        } else if (tableSource instanceof SQLJoinTableSource) {
            SQLJoinTableSource join = (SQLJoinTableSource) tableSource;
            extractAliases(join.getLeft(), ctx);
            extractAliases(join.getRight(), ctx);
        } else if (tableSource instanceof SQLUnionQueryTableSource) {
            String alias = tableSource.getAlias();
            if (alias != null && !alias.isEmpty()) {
                ctx.addAlias(TableNameConverter.cleanIdentifier(alias), TableNameConverter.cleanIdentifier(alias));
            }
        } else if (tableSource instanceof SQLSubqueryTableSource) {
            String alias = tableSource.getAlias();
            if (alias != null && !alias.isEmpty()) {
                ctx.addAlias(TableNameConverter.cleanIdentifier(alias), TableNameConverter.cleanIdentifier(alias));
            }
        } else if (tableSource instanceof SQLLateralViewTableSource) {
            String alias = tableSource.getAlias();
            if (alias != null && !alias.isEmpty()) {
                ctx.addAlias(TableNameConverter.cleanIdentifier(alias), TableNameConverter.cleanIdentifier(alias));
            }
        }
    }
    
    /**
     * Process table source: quote table names, aliases, and ON clause.
     */
    private void processTableSource(SQLTableSource tableSource, ClauseContext ctx) {
        if (tableSource instanceof SQLExprTableSource) {
            SQLExprTableSource exprTable = (SQLExprTableSource) tableSource;
            
            // Quote table name
            String name = exprTable.getTableName();
            if (name != null) {
                String quotedName = ctx.convertAndQuoteTableName(name);
                exprTable.setExpr(quotedName);
            }
            
            // Note: Don't quote alias here - Druid will handle quoting on output
            // Just clean the alias if it has quotes
            String alias = exprTable.getAlias();
            if (alias != null && !alias.isEmpty()) {
                String cleanAlias = TableNameConverter.cleanIdentifier(alias);
                exprTable.setAlias(cleanAlias);
            }
            
        } else if (tableSource instanceof SQLJoinTableSource) {
            SQLJoinTableSource join = (SQLJoinTableSource) tableSource;
            
            // Process left and right
            processTableSource(join.getLeft(), ctx);
            processTableSource(join.getRight(), ctx);
            
            // Process ON clause
            SQLExpr condition = join.getCondition();
            if (condition != null) {
                processOnCondition(condition, ctx);
            }
        }
    }
    
    /**
     * Process ON clause expression to quote identifiers.
     */
    private void processOnCondition(SQLExpr expr, ClauseContext ctx) {
        if (expr == null) return;
        
        if (expr instanceof SQLPropertyExpr) {
            // table.column format
            SQLPropertyExpr prop = (SQLPropertyExpr) expr;
            
            // Quote column name
            String colName = prop.getName();
            String quotedCol = ctx.convertAndQuoteColumnName(colName);
            prop.setName(quotedCol);
            
            // Quote owner (table alias)
            if (prop.getOwner() instanceof SQLIdentifierExpr) {
                SQLIdentifierExpr owner = (SQLIdentifierExpr) prop.getOwner();
                String quotedOwner = ctx.quoteIdentifier(owner.getName());
                owner.setName(quotedOwner);
            }
        } else if (expr instanceof SQLIdentifierExpr) {
            // Just column name (no table prefix)
            SQLIdentifierExpr ident = (SQLIdentifierExpr) expr;
            String quotedName = ctx.convertAndQuoteColumnName(ident.getName());
            ident.setName(quotedName);
        }
        
        // Recursively process child expressions using visitor
        expr.accept(new OnClauseVisitor(ctx));
    }
    
    /**
     * Visitor for processing ON clause expressions.
     */
    private static class OnClauseVisitor extends MySqlASTVisitorAdapter {
        
        private final ClauseContext ctx;
        
        OnClauseVisitor(ClauseContext ctx) {
            this.ctx = ctx;
        }
        
        @Override
        public boolean visit(SQLPropertyExpr x) {
            // Quote column name
            String colName = x.getName();
            String quotedCol = ctx.convertAndQuoteColumnName(colName);
            x.setName(quotedCol);
            
            // Quote owner (table alias)
            if (x.getOwner() instanceof SQLIdentifierExpr) {
                SQLIdentifierExpr owner = (SQLIdentifierExpr) x.getOwner();
                String quotedOwner = ctx.quoteIdentifier(owner.getName());
                owner.setName(quotedOwner);
            }
            return true;
        }
        
        @Override
        public boolean visit(SQLIdentifierExpr x) {
            // Quote identifier (column name without table prefix)
            String quotedName = ctx.convertAndQuoteColumnName(x.getName());
            x.setName(quotedName);
            return true;
        }
    }
}
