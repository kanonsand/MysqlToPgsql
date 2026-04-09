package org.omono.converter.mybatis.interceptor;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import org.omono.converter.common.TypeCategory;
import org.omono.converter.mybatis.ConversionConfig;
import org.omono.converter.mybatis.ConversionContext;
import org.omono.converter.mybatis.ConversionControl;
import org.omono.converter.mybatis.SqlAnalysisResult;
import org.omono.converter.mybatis.rewrite.*;
import org.omono.converter.schema.SchemaRegistry;
import org.omono.converter.schema.TableMapping;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.*;

/**
 * SQL rewrite interceptor for MySQL to PostgreSQL conversion.
 * Intercepts StatementHandler.prepare() to rewrite SQL before execution.
 * 
 * Uses SqlRewriteHandler strategy pattern for clean, extensible statement handling.
 */
@Intercepts({
    @Signature(type = StatementHandler.class, 
               method = "prepare", 
               args = {Connection.class, Integer.class})
})
public class SqlRewriteInterceptor implements Interceptor {
    
    private static final Logger log = LoggerFactory.getLogger(SqlRewriteInterceptor.class);
    
    private final SchemaRegistry schemaRegistry;
    private final ConversionConfig config;
    
    // Handler list (ordered)
    private final List<SqlRewriteHandler> handlers;
    
    // Quick lookup: StatementType -> Handler
    private final Map<ConversionContext.StatementType, SqlRewriteHandler> handlerMap;
    
    public SqlRewriteInterceptor(SchemaRegistry schemaRegistry, ConversionConfig config) {
        this.schemaRegistry = schemaRegistry;
        this.config = config;
        
        // Initialize handlers
        this.handlers = Arrays.asList(
            new InsertRewriteHandler(),
            new UpdateRewriteHandler(),
            new DeleteRewriteHandler(),
            new SelectRewriteHandler()
        );
        
        // Build quick lookup map
        this.handlerMap = new EnumMap<>(ConversionContext.StatementType.class);
        for (SqlRewriteHandler handler : handlers) {
            handlerMap.put(handler.getStatementType(), handler);
        }
    }
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // Check if conversion is disabled globally
        if (!config.isEnabled()) {
            return invocation.proceed();
        }
        
        // Check if conversion is skipped via ThreadLocal
        if (ConversionControl.shouldSkip()) {
            if (log.isDebugEnabled()) {
                log.debug("[SqlRewriteInterceptor] Skipping conversion (ThreadLocal flag set)");
            }
            return invocation.proceed();
        }
        
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        
        // Unwrap RoutingStatementHandler to get the real handler
        while (metaObject.hasGetter("h")) {
            Object h = metaObject.getValue("h");
            if (h == null) break;
            metaObject = SystemMetaObject.forObject(h);
        }
        if (metaObject.hasGetter("target")) {
            metaObject = SystemMetaObject.forObject(metaObject.getValue("target"));
        }
        
        // Get MappedStatement for command type
        MappedStatement mappedStatement = null;
        SqlCommandType sqlCommandType = null;
        
        if (metaObject.hasGetter("mappedStatement")) {
            mappedStatement = (MappedStatement) metaObject.getValue("mappedStatement");
            sqlCommandType = mappedStatement.getSqlCommandType();
        }
        
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();
        
        // Fallback: detect command type from SQL if mappedStatement not available
        if (sqlCommandType == null) {
            sqlCommandType = detectSqlCommandType(originalSql);
        }
        
        // Check if SQL ID is excluded
        if (mappedStatement != null) {
            String sqlId = mappedStatement.getId();
            if (config.isSqlIdExcluded(sqlId)) {
                return invocation.proceed();
            }
        }
        
        if (originalSql == null || originalSql.trim().isEmpty()) {
            return invocation.proceed();
        }
        
        // Analyze SQL
        SqlAnalysisResult analysis = analyzeSql(originalSql, sqlCommandType);
        
        // Check if table is excluded
        if (analysis.getTableName() != null && config.isTableExcluded(analysis.getTableName())) {
            return invocation.proceed();
        }
        
        // Convert SQL and create context
        ConversionContext context = new ConversionContext();
        String convertedSql = convertSql(originalSql, analysis, context);
        
        // Update SQL in BoundSql
        if (!originalSql.equals(convertedSql)) {
            setFieldValue(boundSql, "sql", convertedSql);
        }
        
        // Attach context for ParameterProxyInterceptor
        if (context.needsConversion()) {
            context.attachTo(boundSql);
        }
        
        return invocation.proceed();
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
        // No properties needed
    }
    
    /**
     * Detect SQL command type from the SQL string.
     */
    private SqlCommandType detectSqlCommandType(String sql) {
        if (sql == null) return SqlCommandType.SELECT;
        String upperSql = sql.trim().toUpperCase();
        if (upperSql.startsWith("INSERT")) return SqlCommandType.INSERT;
        if (upperSql.startsWith("UPDATE")) return SqlCommandType.UPDATE;
        if (upperSql.startsWith("DELETE")) return SqlCommandType.DELETE;
        return SqlCommandType.SELECT;
    }
    
    /**
     * Analyze SQL to extract metadata using handlers
     */
    private SqlAnalysisResult analyzeSql(String sql, SqlCommandType commandType) {
        SqlAnalysisResult result = new SqlAnalysisResult();
        
        try {
            List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
            if (statements.isEmpty()) {
                return result;
            }
            
            SQLStatement stmt = statements.get(0);
            
            // Set statement type
            ConversionContext.StatementType stmtType = toStatementType(commandType);
            result.setStatementType(stmtType);
            
            // Use handler to analyze
            for (SqlRewriteHandler handler : handlers) {
                if (handler.supports(stmt)) {
                    handler.analyze(stmt, result);
                    break;
                }
            }
            
            // Count parameters
            result.setParameterCount(countParameters(sql));
            
        } catch (Exception e) {
            log.warn("SQL analysis failed: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Convert SQL and populate ConversionContext
     */
    private String convertSql(String sql, SqlAnalysisResult analysis, ConversionContext context) {
        context.setTableName(analysis.getTableName());
        context.setStatementType(analysis.getStatementType());
        
        TableMapping mapping = schemaRegistry.getTableMapping(analysis.getTableName());
        
        List<SQLStatement> statements;
        try {
            statements = SQLUtils.parseStatements(sql, DbType.mysql);
        } catch (Exception e) {
            return sql;
        }
        
        if (statements.isEmpty()) {
            return sql;
        }
        
        SQLStatement stmt = statements.get(0);
        
        // Collect all table mappings for this query (for JOIN support)
        Map<String, TableMapping> allMappings = collectTableMappings(analysis);
        
        // Use handler to convert
        SqlRewriteHandler handler = handlerMap.get(analysis.getStatementType());
        if (handler != null && handler.supports(stmt)) {
            // For SELECT statements with multiple tables, use multi-table conversion
            if (handler instanceof SelectRewriteHandler && allMappings.size() > 1) {
                ((SelectRewriteHandler) handler).convertMulti(stmt, analysis, allMappings, context);
            } else {
                handler.convert(stmt, analysis, mapping, context);
            }
        } else {
            // Fallback: just quote identifiers
            quoteIdentifiers(stmt, mapping);
        }
        
        // Generate type categories for parameters
        // Only for INSERT statements where parameters correspond directly to columns
        if (analysis.isInsert() && mapping != null) {
            TypeCategory[] categories = mapping.getParameterTypeCategories(analysis.getColumnNames());
            context.setParameterCategories(categories);
            
            if (log.isDebugEnabled()) {
                log.debug("[SqlRewriteInterceptor] Table: {}, Columns: {}, Categories: {}", 
                    analysis.getTableName(), analysis.getColumnNames(), Arrays.toString(categories));
            }
        }
        
        return SQLUtils.toSQLString(statements, DbType.postgresql);
    }
    
    /**
     * Collect all table mappings used in the query.
     * This is needed for JOIN queries that reference multiple tables.
     */
    private Map<String, TableMapping> collectTableMappings(SqlAnalysisResult analysis) {
        Map<String, TableMapping> mappings = new HashMap<>();
        
        // Add mapping for main table
        if (analysis.getTableName() != null) {
            TableMapping mainMapping = schemaRegistry.getTableMapping(analysis.getTableName());
            if (mainMapping != null) {
                mappings.put(analysis.getTableName().toLowerCase(), mainMapping);
            }
        }
        
        // Add mappings for all tables referenced in the query
        for (Map.Entry<String, String> entry : analysis.getAliasToTable().entrySet()) {
            String alias = entry.getKey();
            String tableName = entry.getValue();
            TableMapping tableMapping = schemaRegistry.getTableMapping(tableName);
            if (tableMapping != null) {
                mappings.put(alias.toLowerCase(), tableMapping);
                mappings.put(tableName.toLowerCase(), tableMapping);
            }
        }
        
        return mappings;
    }
    
    /**
     * Quote identifiers without specific handler (fallback)
     */
    private void quoteIdentifiers(SQLStatement stmt, TableMapping mapping) {
        stmt.accept(new MySqlASTVisitorAdapter() {
            @Override
            public boolean visit(SQLExprTableSource x) {
                String name = x.getTableName();
                if (name != null && !name.startsWith("\"")) {
                    x.setExpr("\"" + cleanIdentifier(name) + "\"");
                }
                return true;
            }
        });
    }
    
    private ConversionContext.StatementType toStatementType(SqlCommandType commandType) {
        switch (commandType) {
            case INSERT: return ConversionContext.StatementType.INSERT;
            case UPDATE: return ConversionContext.StatementType.UPDATE;
            case DELETE: return ConversionContext.StatementType.DELETE;
            default: return ConversionContext.StatementType.SELECT;
        }
    }
    
    private String cleanIdentifier(String name) {
        if (name == null) return null;
        return name.replace("`", "").replace("\"", "");
    }
    
    private int countParameters(String sql) {
        int count = 0;
        for (char c : sql.toCharArray()) {
            if (c == '?') {
                count++;
            }
        }
        return count;
    }
    
    private void setFieldValue(Object object, String fieldName, Object value) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}