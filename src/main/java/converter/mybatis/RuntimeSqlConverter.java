package converter.mybatis;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import converter.schema.SchemaRegistry;
import converter.schema.TableMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Runtime SQL converter for MySQL to PostgreSQL
 */
public class RuntimeSqlConverter {
    
    private final ConversionConfig config;
    private final SchemaRegistry schemaRegistry;
    private final Map<String, String> sqlCache;
    private List<FunctionConverter> functionConverters = new ArrayList<>();
    
    public RuntimeSqlConverter(ConversionConfig config, SchemaRegistry schemaRegistry) {
        this.config = config;
        this.schemaRegistry = schemaRegistry;
        this.sqlCache = config.isCacheEnabled() ? new ConcurrentHashMap<>() : null;
    }
    
    public void addFunctionConverter(FunctionConverter converter) {
        this.functionConverters.add(converter);
    }
    
    public String convert(String sql, String commandType) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }
        
        if (config.isCacheEnabled() && sqlCache != null) {
            String cached = sqlCache.get(sql.trim());
            if (cached != null) {
                return cached;
            }
        }
        
        String converted = sql;
        
        try {
            switch (commandType.toUpperCase()) {
                case "INSERT":
                    if (config.isInsertEnabled()) {
                        converted = convertInsert(sql);
                    }
                    break;
                case "UPDATE":
                    if (config.isUpdateEnabled()) {
                        converted = convertUpdate(sql);
                    }
                    break;
                case "DELETE":
                    if (config.isDeleteEnabled()) {
                        converted = convertDelete(sql);
                    }
                    break;
                case "SELECT":
                    if (config.isSelectEnabled()) {
                        converted = convertSelect(sql);
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("SQL conversion failed: " + e.getMessage());
            return sql;
        }
        
        if (config.isCacheEnabled() && sqlCache != null && sqlCache.size() < config.getCacheMaxSize()) {
            sqlCache.put(sql.trim(), converted);
        }
        
        return converted;
    }
    
    private String convertInsert(String sql) {
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        
        for (SQLStatement stmt : statements) {
            if (stmt instanceof MySqlInsertStatement) {
                MySqlInsertStatement insert = (MySqlInsertStatement) stmt;
                final String tableName = extractTableName(insert.getTableName());
                
                if (config.isTableExcluded(tableName)) {
                    continue;
                }
                
                final TableMapping mapping = schemaRegistry.getTableMapping(tableName);
                
                insert.accept(new MySqlASTVisitorAdapter() {
                    @Override
                    public boolean visit(SQLExprTableSource x) {
                        String name = x.getTableName();
                        if (name != null) {
                            x.setExpr("\"" + name.replace("`", "").replace("\"", "") + "\"");
                        }
                        return true;
                    }
                    
                    @Override
                    public boolean visit(SQLIdentifierExpr x) {
                        String name = x.getName();
                        if (name != null && !name.startsWith("\"")) {
                            String cleanName = name.replace("`", "").replace("\"", "");
                            if (mapping != null) {
                                String pgColName = mapping.getPostgresColumnName(cleanName);
                                x.setName("\"" + pgColName + "\"");
                            } else {
                                x.setName("\"" + cleanName + "\"");
                            }
                        }
                        return true;
                    }
                });
            }
        }
        
        return statements.toString();
    }
    
    private String convertUpdate(String sql) {
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        
        for (SQLStatement stmt : statements) {
            if (stmt instanceof MySqlUpdateStatement) {
                MySqlUpdateStatement update = (MySqlUpdateStatement) stmt;
                final String tableName = extractTableName(update.getTableSource());
                
                if (config.isTableExcluded(tableName)) {
                    continue;
                }
                
                final TableMapping mapping = schemaRegistry.getTableMapping(tableName);
                
                update.accept(new MySqlASTVisitorAdapter() {
                    @Override
                    public boolean visit(SQLExprTableSource x) {
                        String name = x.getTableName();
                        if (name != null) {
                            x.setExpr("\"" + name.replace("`", "").replace("\"", "") + "\"");
                        }
                        return true;
                    }
                    
                    @Override
                    public boolean visit(SQLIdentifierExpr x) {
                        String name = x.getName();
                        if (name != null && !name.startsWith("\"")) {
                            String cleanName = name.replace("`", "").replace("\"", "");
                            if (mapping != null) {
                                String pgColName = mapping.getPostgresColumnName(cleanName);
                                x.setName("\"" + pgColName + "\"");
                            } else {
                                x.setName("\"" + cleanName + "\"");
                            }
                        }
                        return true;
                    }
                });
            }
        }
        
        return statements.toString();
    }
    
    private String convertDelete(String sql) {
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        
        for (SQLStatement stmt : statements) {
            if (stmt instanceof MySqlDeleteStatement) {
                MySqlDeleteStatement delete = (MySqlDeleteStatement) stmt;
                
                delete.accept(new MySqlASTVisitorAdapter() {
                    @Override
                    public boolean visit(SQLExprTableSource x) {
                        String name = x.getTableName();
                        if (name != null) {
                            x.setExpr("\"" + name.replace("`", "").replace("\"", "") + "\"");
                        }
                        return true;
                    }
                });
            }
        }
        
        return statements.toString();
    }
    
    private String convertSelect(String sql) {
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        
        for (SQLStatement stmt : statements) {
            if (stmt instanceof SQLSelectStatement) {
                SQLSelectStatement select = (SQLSelectStatement) stmt;
                
                select.accept(new MySqlASTVisitorAdapter() {
                    @Override
                    public boolean visit(SQLExprTableSource x) {
                        String name = x.getTableName();
                        if (name != null && !name.startsWith("\"")) {
                            x.setExpr("\"" + name.replace("`", "").replace("\"", "") + "\"");
                        }
                        return true;
                    }
                });
            }
        }
        
        return statements.toString();
    }
    
    private String extractTableName(SQLExpr expr) {
        if (expr instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) expr).getName().replace("`", "").replace("\"", "");
        }
        return null;
    }
    
    private String extractTableName(SQLTableSource tableSource) {
        if (tableSource instanceof SQLExprTableSource) {
            SQLExprTableSource exprTable = (SQLExprTableSource) tableSource;
            return exprTable.getTableName().replace("`", "").replace("\"", "");
        }
        return null;
    }
    
    public void clearCache() {
        if (sqlCache != null) {
            sqlCache.clear();
        }
    }
    
    public int getCacheSize() {
        return sqlCache != null ? sqlCache.size() : 0;
    }
}
