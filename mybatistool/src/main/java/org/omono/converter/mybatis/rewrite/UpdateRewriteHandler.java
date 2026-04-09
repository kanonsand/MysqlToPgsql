package org.omono.converter.mybatis.rewrite;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import org.omono.converter.mybatis.ConversionContext;
import org.omono.converter.mybatis.SqlAnalysisResult;
import org.omono.converter.mybatis.clause.ClauseContext;
import org.omono.converter.mybatis.clause.handler.FromClauseHandler;
import org.omono.converter.mybatis.clause.handler.SetClauseHandler;
import org.omono.converter.mybatis.clause.handler.WhereClauseHandler;
import org.omono.converter.schema.TableMapping;

/**
 * Handler for UPDATE statement rewrite.
 * Uses dedicated clause handlers for each SQL clause.
 */
public class UpdateRewriteHandler extends SqlRewriteHandler {
    
    @Override
    public ConversionContext.StatementType getStatementType() {
        return ConversionContext.StatementType.UPDATE;
    }
    
    @Override
    public boolean supports(SQLStatement stmt) {
        return stmt instanceof MySqlUpdateStatement;
    }
    
    @Override
    public void analyze(SQLStatement stmt, SqlAnalysisResult result) {
        if (!(stmt instanceof MySqlUpdateStatement)) {
            return;
        }
        
        MySqlUpdateStatement update = (MySqlUpdateStatement) stmt;
        
        // Create context for alias extraction
        ClauseContext ctx = new ClauseContext((TableMapping) null, result);
        
        // Extract table name and aliases
        result.setTableName(extractTableName(update.getTableSource()));
        new FromClauseHandler().process(update.getTableSource(), ctx);
        
        // NOTE: We don't collect column names here because:
        // 1. SET clause may have literals (not parameters)
        // 2. WHERE clause may have parameters
        // The parameter type conversion will be handled by the SET/WHERE clause handlers
        // based on the actual parameters in the SQL.
    }
    
    @Override
    public void convert(SQLStatement stmt, SqlAnalysisResult analysis, 
                        TableMapping mapping, ConversionContext context) {
        if (!(stmt instanceof MySqlUpdateStatement)) {
            return;
        }
        
        MySqlUpdateStatement update = (MySqlUpdateStatement) stmt;
        
        // Create context with all shared state
        ClauseContext ctx = new ClauseContext(mapping, analysis);
        
        // 1. Process FROM clause (table source)
        new FromClauseHandler().process(update.getTableSource(), ctx);
        
        // 2. Process SET clause (quote columns + convert literals)
        new SetClauseHandler().process(update.getItems(), ctx);
        
        // 3. Process WHERE clause
        new WhereClauseHandler().process(update.getWhere(), ctx);
    }
}