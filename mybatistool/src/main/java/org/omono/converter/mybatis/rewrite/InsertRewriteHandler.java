package org.omono.converter.mybatis.rewrite;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import org.omono.converter.mybatis.ConversionConfig;
import org.omono.converter.mybatis.ConversionContext;
import org.omono.converter.mybatis.SqlAnalysisResult;
import org.omono.converter.mybatis.clause.ClauseContext;
import org.omono.converter.mybatis.clause.ColumnNameConverter;
import org.omono.converter.common.TypeCategory;
import org.omono.converter.schema.TableMapping;

import java.util.List;

/**
 * Handler for INSERT statement rewrite.
 */
public class InsertRewriteHandler extends SqlRewriteHandler {
    
    @Override
    public ConversionContext.StatementType getStatementType() {
        return ConversionContext.StatementType.INSERT;
    }
    
    @Override
    public boolean supports(SQLStatement stmt) {
        return stmt instanceof MySqlInsertStatement;
    }
    
    @Override
    public void analyze(SQLStatement stmt, SqlAnalysisResult result) {
        if (!(stmt instanceof MySqlInsertStatement)) {
            return;
        }
        
        MySqlInsertStatement insert = (MySqlInsertStatement) stmt;
        
        // Extract table name
        result.setTableName(extractTableName(insert.getTableName()));
        
        // Extract column names
        List<SQLExpr> columns = insert.getColumns();
        if (columns != null && !columns.isEmpty()) {
            for (SQLExpr col : columns) {
                if (col instanceof SQLIdentifierExpr) {
                    result.addColumnName(((SQLIdentifierExpr) col).getName());
                }
            }
        } else {
            // INSERT without column names
            result.setHasValuesWithoutColumns(true);
        }
    }
    
    @Override
    public void convert(SQLStatement stmt, SqlAnalysisResult analysis, 
                        TableMapping mapping, ConversionContext context,
                        ConversionConfig config) {
        if (!(stmt instanceof MySqlInsertStatement)) {
            return;
        }
        
        MySqlInsertStatement insert = (MySqlInsertStatement) stmt;
        
        // Create context with converters
        ClauseContext ctx = new ClauseContext(mapping, config, analysis);
        
        // Add column names if missing
        if (analysis.isHasValuesWithoutColumns() && mapping != null) {
            List<String> pgColumnNames = mapping.getPostgresColumnNames();
            for (String colName : pgColumnNames) {
                insert.addColumn(new SQLIdentifierExpr("\"" + colName + "\""));
            }
        }
        
        // Convert literals in VALUES clause
        if (mapping != null) {
            convertValuesLiterals(insert, ctx, analysis);
        }
        
        // Quote identifiers using ClauseContext
        stmt.accept(createQuoteVisitor(ctx));
    }
    
    /**
     * Convert literals in VALUES clause to match target column types.
     */
    private void convertValuesLiterals(MySqlInsertStatement insert, ClauseContext ctx, 
                                        SqlAnalysisResult analysis) {
        List<SQLExpr> columns = insert.getColumns();
        if (columns == null || columns.isEmpty()) {
            return;
        }
        
        // Get VALUES clause list
        List<SQLInsertStatement.ValuesClause> valuesList = insert.getValuesList();
        if (valuesList == null || valuesList.isEmpty()) {
            return;
        }
        
        // Process each VALUES clause (for multi-row INSERT)
        for (SQLInsertStatement.ValuesClause valuesClause : valuesList) {
            List<SQLExpr> values = valuesClause.getValues();
            if (values == null || values.size() != columns.size()) {
                continue;
            }
            
            // Convert each value based on its column type
            for (int i = 0; i < columns.size(); i++) {
                SQLExpr column = columns.get(i);
                if (!(column instanceof SQLIdentifierExpr)) {
                    continue;
                }
                
                String colName = ColumnNameConverter.cleanIdentifier(((SQLIdentifierExpr) column).getName());
                TypeCategory targetCategory = getColumnType(ctx, colName);
                
                // Convert the value expression
                SQLExpr value = values.get(i);
                SQLExpr converted = convertLiteral(value, targetCategory);
                
                if (converted != value) {
                    values.set(i, converted);
                }
            }
        }
    }
}