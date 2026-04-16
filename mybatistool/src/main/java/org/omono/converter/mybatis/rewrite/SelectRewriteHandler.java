package org.omono.converter.mybatis.rewrite;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.*;
import org.omono.converter.mybatis.ConversionConfig;
import org.omono.converter.mybatis.ConversionContext;
import org.omono.converter.mybatis.SqlAnalysisResult;
import org.omono.converter.mybatis.clause.*;
import org.omono.converter.mybatis.clause.handler.*;
import org.omono.converter.schema.TableMapping;

import java.util.Map;

/**
 * Handler for SELECT statement rewrite.
 * Uses dedicated clause handlers for each SQL clause.
 */
public class SelectRewriteHandler extends SqlRewriteHandler {
    
    @Override
    public ConversionContext.StatementType getStatementType() {
        return ConversionContext.StatementType.SELECT;
    }
    
    @Override
    public boolean supports(SQLStatement stmt) {
        return stmt instanceof SQLSelectStatement;
    }
    
    @Override
    public void analyze(SQLStatement stmt, SqlAnalysisResult result) {
        if (!(stmt instanceof SQLSelectStatement)) {
            return;
        }
        
        SQLSelectStatement select = (SQLSelectStatement) stmt;
        SQLSelectQuery query = select.getSelect().getQuery();
        
        if (query instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) query;
            
            // Create context for alias extraction
            ClauseContext ctx = new ClauseContext((TableMapping) null, result);
            
            // Extract table name and aliases
            SQLTableSource tableSource = queryBlock.getFrom();
            result.setTableName(extractTableName(tableSource));
            new FromClauseHandler().process(tableSource, ctx);
            
            // Check for LIMIT clause
            if (queryBlock.getLimit() != null) {
                result.setHasLimitOffset(true);
            }
        }
    }
    
    @Override
    public void convert(SQLStatement stmt, SqlAnalysisResult analysis, 
                        TableMapping mapping, ConversionContext context,
                        ConversionConfig config) {
        if (!(stmt instanceof SQLSelectStatement)) {
            return;
        }
        
        SQLSelectStatement select = (SQLSelectStatement) stmt;
        SQLSelectQuery query = select.getSelect().getQuery();
        
        if (query instanceof SQLSelectQueryBlock) {
            ClauseContext ctx = new ClauseContext(mapping, config, analysis);
            processQueryBlock((SQLSelectQueryBlock) query, ctx);
        }
    }
    
    /**
     * Convert SELECT with multiple table mappings (for JOIN queries).
     * 
     * @param stmt the SQL statement
     * @param analysis analysis result
     * @param tableMappings map of table name/alias -> TableMapping
     * @param context conversion context
     * @param config conversion configuration
     */
    public void convertMulti(SQLStatement stmt, SqlAnalysisResult analysis,
                             Map<String, TableMapping> tableMappings, 
                             ConversionContext context, ConversionConfig config) {
        if (!(stmt instanceof SQLSelectStatement)) {
            return;
        }
        
        SQLSelectStatement select = (SQLSelectStatement) stmt;
        SQLSelectQuery query = select.getSelect().getQuery();
        
        if (query instanceof SQLSelectQueryBlock) {
            convertQueryBlockMulti((SQLSelectQueryBlock) query, analysis, tableMappings, config);
        }
    }
    
    /**
     * Convert a query block with multi-table mappings.
     */
    private void convertQueryBlockMulti(SQLSelectQueryBlock queryBlock,
                                        SqlAnalysisResult analysis,
                                        Map<String, TableMapping> tableMappings,
                                        ConversionConfig config) {
        ClauseContext ctx;
        if (tableMappings != null && !tableMappings.isEmpty()) {
            // Use multi-table constructor: (analysis, config, mappings)
            ctx = new ClauseContext(analysis, config, tableMappings);
        } else {
            // Use single-table constructor: (mapping, config, analysis)
            ctx = new ClauseContext((TableMapping) null, config, analysis);
        }
        processQueryBlock(queryBlock, ctx);
    }
    
    /**
     * Process query block with the given context.
     */
    private void processQueryBlock(SQLSelectQueryBlock queryBlock, ClauseContext ctx) {
        // 1. Process FROM clause (extract aliases and quote table names)
        new FromClauseHandler().process(queryBlock.getFrom(), ctx);
        
        // 2. Process SELECT columns
        new SelectClauseHandler().process(queryBlock.getSelectList(), ctx);
        
        // 3. Process WHERE clause
        new WhereClauseHandler().process(queryBlock.getWhere(), ctx);
        
        // 4. Process GROUP BY clause
        SQLSelectGroupByClause groupBy = queryBlock.getGroupBy();
        new GroupByClauseHandler().process(groupBy, ctx);
        
        // 5. Process HAVING clause (part of GROUP BY)
        if (groupBy != null) {
            new HavingClauseHandler().process(groupBy.getHaving(), ctx);
        }
        
        // 6. Process ORDER BY clause
        new OrderByClauseHandler().process(queryBlock.getOrderBy(), ctx);
        
        // 7. Process LIMIT clause
        new LimitClauseHandler().process(queryBlock.getLimit(), ctx);
    }
}