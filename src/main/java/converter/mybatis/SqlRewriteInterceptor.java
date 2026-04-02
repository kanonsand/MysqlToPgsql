package converter.mybatis;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLLiteralExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGSelectQueryBlock;
import converter.schema.ColumnDefinition;
import converter.schema.SchemaRegistry;
import converter.schema.SqlTypeMapper;
import converter.schema.TableMapping;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL rewrite interceptor for MySQL to PostgreSQL conversion.
 * Intercepts StatementHandler.prepare() to rewrite SQL before execution.
 */
@Intercepts({
    @Signature(type = StatementHandler.class, 
               method = "prepare", 
               args = {Connection.class, Integer.class})
})
public class SqlRewriteInterceptor implements Interceptor {
    
    private final SchemaRegistry schemaRegistry;
    private final ConversionConfig config;
    
    public SqlRewriteInterceptor(SchemaRegistry schemaRegistry, ConversionConfig config) {
        this.schemaRegistry = schemaRegistry;
        this.config = config;
    }
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!config.isEnabled()) {
            return invocation.proceed();
        }
        
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        
        // Get MappedStatement for command type
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("mappedStatement");
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        
        // Check if SQL ID is excluded
        String sqlId = mappedStatement.getId();
        if (config.isSqlIdExcluded(sqlId)) {
            return invocation.proceed();
        }
        
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();
        
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
     * Analyze SQL to extract metadata
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
            switch (commandType) {
                case INSERT:
                    result.setStatementType(ConversionContext.StatementType.INSERT);
                    analyzeInsert(stmt, result);
                    break;
                case UPDATE:
                    result.setStatementType(ConversionContext.StatementType.UPDATE);
                    analyzeUpdate(stmt, result);
                    break;
                case DELETE:
                    result.setStatementType(ConversionContext.StatementType.DELETE);
                    analyzeDelete(stmt, result);
                    break;
                case SELECT:
                    result.setStatementType(ConversionContext.StatementType.SELECT);
                    analyzeSelect(stmt, result);
                    break;
                default:
                    result.setStatementType(ConversionContext.StatementType.SELECT);
            }
            
            // Count parameters
            result.setParameterCount(countParameters(sql));
            
        } catch (Exception e) {
            // Log error but don't fail
            System.err.println("SQL analysis failed: " + e.getMessage());
        }
        
        return result;
    }
    
    private void analyzeInsert(SQLStatement stmt, SqlAnalysisResult result) {
        if (stmt instanceof MySqlInsertStatement) {
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
    }
    
    private void analyzeUpdate(SQLStatement stmt, SqlAnalysisResult result) {
        if (stmt instanceof MySqlUpdateStatement) {
            MySqlUpdateStatement update = (MySqlUpdateStatement) stmt;
            result.setTableName(extractTableNameFromTableSource(update.getTableSource()));
            
            // Extract SET columns
            List<SQLUpdateSetItem> items = update.getItems();
            if (items != null) {
                for (SQLUpdateSetItem item : items) {
                    SQLExpr column = item.getColumn();
                    if (column instanceof SQLIdentifierExpr) {
                        result.addColumnName(((SQLIdentifierExpr) column).getName());
                    }
                }
            }
        }
    }
    
    private void analyzeDelete(SQLStatement stmt, SqlAnalysisResult result) {
        if (stmt instanceof MySqlDeleteStatement) {
            MySqlDeleteStatement delete = (MySqlDeleteStatement) stmt;
            result.setTableName(extractTableNameFromTableSource(delete.getTableSource()));
        }
    }
    
    private void analyzeSelect(SQLStatement stmt, SqlAnalysisResult result) {
        if (stmt instanceof SQLSelectStatement) {
            SQLSelectStatement select = (SQLSelectStatement) stmt;
            SQLSelectQuery query = select.getSelect().getQuery();
            
            if (query instanceof SQLSelectQueryBlock) {
                SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) query;
                
                // Extract table name
                SQLTableSource tableSource = queryBlock.getFrom();
                if (tableSource instanceof SQLExprTableSource) {
                    result.setTableName(extractTableNameFromTableSource(tableSource));
                }
                
                // LIMIT/OFFSET handling is done automatically by Druid when converting to PostgreSQL
                // Check if query has LIMIT clause for analysis purposes
                String sql = stmt.toString().toUpperCase();
                if (sql.contains("LIMIT")) {
                    result.setHasLimitOffset(true);
                }
            }
        }
    }
    
    private int extractIntValue(SQLExpr expr) {
        if (expr instanceof SQLIntegerExpr) {
            return ((SQLIntegerExpr) expr).getNumber().intValue();
        }
        return -1;
    }
    
    /**
     * Convert SQL and populate ConversionContext
     */
    private String convertSql(String sql, SqlAnalysisResult analysis, ConversionContext context) {
        context.setTableName(analysis.getTableName());
        context.setStatementType(analysis.getStatementType());
        
        TableMapping mapping = schemaRegistry.getTableMapping(analysis.getTableName());
        
        if (mapping == null) {
            // No mapping found, just quote identifiers
            return quoteIdentifiers(sql, analysis.getStatementType());
        }
        
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
        
        // Convert based on statement type
        switch (analysis.getStatementType()) {
            case INSERT:
                convertInsertStatement(stmt, analysis, mapping, context);
                break;
            case UPDATE:
                convertUpdateStatement(stmt, analysis, mapping, context);
                break;
            case DELETE:
                convertDeleteStatement(stmt, mapping, context);
                break;
            case SELECT:
                convertSelectStatement(stmt, analysis, mapping, context);
                break;
        }
        
        // Generate parameter mapping and type categories
        if (analysis.isInsert() || analysis.isUpdate()) {
            int[] paramMapping = analysis.isHasValuesWithoutColumns() 
                    ? mapping.getParameterMapping()
                    : mapping.getParameterMappingWithColumns(analysis.getColumnNames());
            context.setParameterMapping(paramMapping);
            
            SqlTypeMapper.TypeCategory[] categories = mapping.getParameterTypeCategories(analysis.getColumnNames());
            context.setParameterCategories(categories);
        }
        
        return statements.toString();
    }
    
    private void convertInsertStatement(SQLStatement stmt, SqlAnalysisResult analysis, 
                                        TableMapping mapping, ConversionContext context) {
        if (!(stmt instanceof MySqlInsertStatement)) {
            return;
        }
        
        MySqlInsertStatement insert = (MySqlInsertStatement) stmt;
        
        // Add column names if missing
        if (analysis.isHasValuesWithoutColumns()) {
            List<String> pgColumnNames = mapping.getPostgresColumnNames();
            for (String colName : pgColumnNames) {
                insert.addColumn(new SQLIdentifierExpr("\"" + colName + "\""));
            }
        }
        
        // Quote identifiers
        insert.accept(new MySqlASTVisitorAdapter() {
            @Override
            public boolean visit(SQLExprTableSource x) {
                String name = x.getTableName();
                if (name != null && !name.startsWith("\"")) {
                    x.setExpr("\"" + cleanIdentifier(name) + "\"");
                }
                return true;
            }
            
            @Override
            public boolean visit(SQLIdentifierExpr x) {
                String name = x.getName();
                if (name != null && !name.startsWith("\"") && !isValuePlaceholder(name)) {
                    String cleanName = cleanIdentifier(name);
                    String pgColName = mapping.getPostgresColumnName(cleanName);
                    x.setName("\"" + pgColName + "\"");
                }
                return true;
            }
        });
    }
    
    private void convertUpdateStatement(SQLStatement stmt, SqlAnalysisResult analysis,
                                        TableMapping mapping, ConversionContext context) {
        if (!(stmt instanceof MySqlUpdateStatement)) {
            return;
        }
        
        MySqlUpdateStatement update = (MySqlUpdateStatement) stmt;
        
        update.accept(new MySqlASTVisitorAdapter() {
            @Override
            public boolean visit(SQLExprTableSource x) {
                String name = x.getTableName();
                if (name != null && !name.startsWith("\"")) {
                    x.setExpr("\"" + cleanIdentifier(name) + "\"");
                }
                return true;
            }
            
            @Override
            public boolean visit(SQLIdentifierExpr x) {
                String name = x.getName();
                if (name != null && !name.startsWith("\"") && !isValuePlaceholder(name)) {
                    String cleanName = cleanIdentifier(name);
                    String pgColName = mapping.getPostgresColumnName(cleanName);
                    x.setName("\"" + pgColName + "\"");
                }
                return true;
            }
        });
    }
    
    private void convertDeleteStatement(SQLStatement stmt, TableMapping mapping, ConversionContext context) {
        if (!(stmt instanceof MySqlDeleteStatement)) {
            return;
        }
        
        MySqlDeleteStatement delete = (MySqlDeleteStatement) stmt;
        
        delete.accept(new MySqlASTVisitorAdapter() {
            @Override
            public boolean visit(SQLExprTableSource x) {
                String name = x.getTableName();
                if (name != null && !name.startsWith("\"")) {
                    x.setExpr("\"" + cleanIdentifier(name) + "\"");
                }
                return true;
            }
            
            @Override
            public boolean visit(SQLIdentifierExpr x) {
                String name = x.getName();
                if (name != null && !name.startsWith("\"") && !isValuePlaceholder(name)) {
                    String cleanName = cleanIdentifier(name);
                    String pgColName = mapping.getPostgresColumnName(cleanName);
                    x.setName("\"" + pgColName + "\"");
                }
                return true;
            }
        });
    }
    
    private void convertSelectStatement(SQLStatement stmt, SqlAnalysisResult analysis,
                                        TableMapping mapping, ConversionContext context) {
        if (!(stmt instanceof SQLSelectStatement)) {
            return;
        }
        
        SQLSelectStatement select = (SQLSelectStatement) stmt;
        
        select.accept(new MySqlASTVisitorAdapter() {
            @Override
            public boolean visit(SQLExprTableSource x) {
                String name = x.getTableName();
                if (name != null && !name.startsWith("\"")) {
                    x.setExpr("\"" + cleanIdentifier(name) + "\"");
                }
                return true;
            }
            
            @Override
            public boolean visit(SQLIdentifierExpr x) {
                String name = x.getName();
                if (name != null && !name.startsWith("\"") && !isValuePlaceholder(name)) {
                    String cleanName = cleanIdentifier(name);
                    String pgColName = mapping.getPostgresColumnName(cleanName);
                    
                    // Add alias if column name is different
                    if (mapping.needsAlias(cleanName)) {
                        // For SELECT, we need to use AS alias
                        // This is a simplified approach - complex queries may need more handling
                        x.setName("\"" + pgColName + "\" AS \"" + cleanName + "\"");
                    } else {
                        x.setName("\"" + pgColName + "\"");
                    }
                }
                return true;
            }
        });
        
        // Handle LIMIT offset, count -> LIMIT count OFFSET offset
        // This is handled in the SQL output by Druid
    }
    
    /**
     * Quote identifiers without table mapping
     */
    private String quoteIdentifiers(String sql, ConversionContext.StatementType type) {
        try {
            List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
            
            for (SQLStatement stmt : statements) {
                stmt.accept(new MySqlASTVisitorAdapter() {
                    @Override
                    public boolean visit(SQLExprTableSource x) {
                        String name = x.getTableName();
                        if (name != null && !name.startsWith("\"")) {
                            x.setExpr("\"" + cleanIdentifier(name) + "\"");
                        }
                        return true;
                    }
                    
                    @Override
                    public boolean visit(SQLIdentifierExpr x) {
                        String name = x.getName();
                        if (name != null && !name.startsWith("\"") && !isValuePlaceholder(name)) {
                            x.setName("\"" + cleanIdentifier(name) + "\"");
                        }
                        return true;
                    }
                });
            }
            
            return statements.toString();
        } catch (Exception e) {
            return sql;
        }
    }
    
    private String extractTableName(SQLExpr expr) {
        if (expr instanceof SQLIdentifierExpr) {
            return cleanIdentifier(((SQLIdentifierExpr) expr).getName());
        }
        return null;
    }
    
    private String extractTableNameFromTableSource(SQLTableSource tableSource) {
        if (tableSource instanceof SQLExprTableSource) {
            SQLExprTableSource exprTable = (SQLExprTableSource) tableSource;
            return cleanIdentifier(exprTable.getTableName());
        }
        return null;
    }
    
    private String cleanIdentifier(String name) {
        if (name == null) return null;
        return name.replace("`", "").replace("\"", "");
    }
    
    private boolean isValuePlaceholder(String name) {
        return name.equals("?") || name.startsWith(":") || name.startsWith("#{") || name.startsWith("${");
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
