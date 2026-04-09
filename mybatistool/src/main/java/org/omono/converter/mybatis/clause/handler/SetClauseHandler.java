package org.omono.converter.mybatis.clause.handler;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import org.omono.converter.common.TypeCategory;
import org.omono.converter.mybatis.clause.ClauseContext;
import org.omono.converter.mybatis.clause.ColumnNameConverter;
import org.omono.converter.mybatis.type.mapping.SqlTypeMapper;

import java.util.List;

/**
 * Handler for SET clause in UPDATE statements.
 * Processes:
 * 1. Quotes column names for target database
 * 2. Converts literals to match target column types
 * 3. Strips table alias from column names (PostgreSQL doesn't support SET alias.column)
 */
public class SetClauseHandler implements ClauseHandler {
    
    @Override
    public void process(Object clause, ClauseContext ctx) {
        if (clause == null) {
            return;
        }
        
        // Handle single item or list
        if (clause instanceof List) {
            for (Object item : (List<?>) clause) {
                processSetItem(item, ctx);
            }
        } else {
            processSetItem(clause, ctx);
        }
    }
    
    private void processSetItem(Object item, ClauseContext ctx) {
        if (!(item instanceof SQLUpdateSetItem)) {
            return;
        }
        
        SQLUpdateSetItem setItem = (SQLUpdateSetItem) item;
        setItem.accept(new SetClauseVisitor(ctx));
    }
    
    /**
     * Visitor for SET clause items.
     */
    private static class SetClauseVisitor extends MySqlASTVisitorAdapter {
        private final ClauseContext ctx;
        
        SetClauseVisitor(ClauseContext ctx) {
            this.ctx = ctx;
        }
        
        @Override
        public boolean visit(SQLUpdateSetItem x) {
            // Quote the column name
            SQLExpr column = x.getColumn();
            
            if (column instanceof SQLPropertyExpr) {
                // Handle alias.column syntax (e.g., u.status)
                // PostgreSQL doesn't support this in SET clause, so we strip the alias
                SQLPropertyExpr propExpr = (SQLPropertyExpr) column;
                String colName = propExpr.getName();
                String tableAlias = null;
                
                // Get alias from owner
                SQLExpr owner = propExpr.getOwner();
                if (owner instanceof SQLIdentifierExpr) {
                    tableAlias = ((SQLIdentifierExpr) owner).getName();
                }
                
                String quotedName = ctx.convertAndQuoteColumnName(colName, tableAlias);
                
                // Replace the property expression with a simple identifier
                // PostgreSQL doesn't support "alias"."column" in SET clause
                x.setColumn(new SQLIdentifierExpr(quotedName));
            } else if (column instanceof SQLIdentifierExpr) {
                String name = ((SQLIdentifierExpr) column).getName();
                String quotedName = ctx.convertAndQuoteColumnName(name);
                ((SQLIdentifierExpr) column).setName(quotedName);
            }
            
            // Convert literal value based on column type
            if (ctx.isConvertLiterals() && ctx.hasMapping()) {
                convertLiteralValue(x);
            }
            
            return true;
        }
        
        private void convertLiteralValue(SQLUpdateSetItem setItem) {
            SQLExpr column = setItem.getColumn();
            String colName = null;
            
            if (column instanceof SQLIdentifierExpr) {
                colName = ColumnNameConverter.cleanIdentifier(((SQLIdentifierExpr) column).getName());
            } else if (column instanceof SQLPropertyExpr) {
                colName = ColumnNameConverter.cleanIdentifier(((SQLPropertyExpr) column).getName());
            }
            
            if (colName == null) {
                return;
            }
            
            String targetType = ctx.getColumnType(colName);
            if (targetType == null) {
                return;
            }
            
            TypeCategory targetCategory = 
                SqlTypeMapper.getCategory(targetType);
            
            SQLExpr value = setItem.getValue();
            SQLExpr converted = WhereClauseHandler.convertLiteral(value, targetCategory);
            
            if (converted != value) {
                setItem.setValue(converted);
            }
        }
    }
}
