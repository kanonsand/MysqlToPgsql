package org.omono.converter.mybatis.rewrite;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import org.omono.converter.mybatis.ConversionConfig;
import org.omono.converter.mybatis.ConversionContext;
import org.omono.converter.mybatis.SqlAnalysisResult;
import org.omono.converter.mybatis.clause.ClauseContext;
import org.omono.converter.mybatis.clause.handler.FromClauseHandler;
import org.omono.converter.mybatis.clause.handler.WhereClauseHandler;
import org.omono.converter.schema.TableMapping;

/**
 * Handler for DELETE statement rewrite.
 * Uses dedicated clause handlers for each SQL clause.
 */
public class DeleteRewriteHandler extends SqlRewriteHandler {
    
    @Override
    public ConversionContext.StatementType getStatementType() {
        return ConversionContext.StatementType.DELETE;
    }
    
    @Override
    public boolean supports(SQLStatement stmt) {
        return stmt instanceof MySqlDeleteStatement;
    }
    
    @Override
    public void analyze(SQLStatement stmt, SqlAnalysisResult result) {
        if (!(stmt instanceof MySqlDeleteStatement)) {
            return;
        }
        
        MySqlDeleteStatement delete = (MySqlDeleteStatement) stmt;
        
        // Create context for alias extraction
        ClauseContext ctx = new ClauseContext(null, result);
        
        // Extract table name and aliases
        result.setTableName(extractTableName(delete.getTableSource()));
        new FromClauseHandler().process(delete.getTableSource(), ctx);
    }
    
    @Override
    public void convert(SQLStatement stmt, SqlAnalysisResult analysis, 
                        TableMapping mapping, ConversionContext context,
                        ConversionConfig config) {
        if (!(stmt instanceof MySqlDeleteStatement)) {
            return;
        }
        
        MySqlDeleteStatement delete = (MySqlDeleteStatement) stmt;
        
        // Create context with all shared state
        ClauseContext ctx = new ClauseContext(mapping, config, analysis);
        
        // 1. Process FROM clause (table source)
        new FromClauseHandler().process(delete.getTableSource(), ctx);
        
        // 2. Process WHERE clause
        new WhereClauseHandler().process(delete.getWhere(), ctx);
    }
}
