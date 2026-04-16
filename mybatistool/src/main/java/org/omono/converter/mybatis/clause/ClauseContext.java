package org.omono.converter.mybatis.clause;

import com.alibaba.druid.DbType;
import org.omono.converter.common.QuoteHelper;
import org.omono.converter.mybatis.ConversionConfig;
import org.omono.converter.mybatis.SqlAnalysisResult;
import org.omono.converter.mybatis.clause.handler.TableNameConverter;
import org.omono.converter.schema.TableMapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Context object passed through clause handlers.
 * Contains all shared state and configuration needed for SQL processing.
 */
public class ClauseContext {
    
    // Single table mapping (for backward compatibility, may be null)
    private final TableMapping tableMapping;
    
    // Multi-table mappings (table name/alias -> TableMapping)
    private final Map<String, TableMapping> tableMappings;
    
    // Analysis result containing alias mappings
    private final SqlAnalysisResult analysisResult;
    private final Map<String, String> aliasToTable;
    
    // Name converters (flexible for different target databases)
    private ColumnNameConverter columnNameConverter;
    private TableNameConverter tableNameConverter;
    
    // Configuration (contains DbType and quoting settings)
    private final ConversionConfig config;
    
    // Processing flags
    private final boolean quoteIdentifiers;
    private final boolean convertLiterals;
    
    /**
     * Create context with TableMapping (backward compatibility).
     * Uses default ConversionConfig (MySQL → PostgreSQL).
     */
    public ClauseContext(TableMapping tableMapping) {
        this(tableMapping, ConversionConfig.mysqlToPostgres(), null);
    }
    
    /**
     * Create context with TableMapping and analysis result (backward compatibility).
     * Uses default ConversionConfig (MySQL → PostgreSQL).
     */
    public ClauseContext(TableMapping tableMapping, SqlAnalysisResult analysisResult) {
        this(tableMapping, ConversionConfig.mysqlToPostgres(), analysisResult);
    }
    
    /**
     * Create context with TableMapping and ConversionConfig.
     */
    public ClauseContext(TableMapping tableMapping, ConversionConfig config) {
        this(tableMapping, config, null);
    }
    
    /**
     * Create context with TableMapping, ConversionConfig, and analysis result.
     */
    public ClauseContext(TableMapping tableMapping, ConversionConfig config, 
                         SqlAnalysisResult analysisResult) {
        this.tableMapping = tableMapping;
        this.config = config != null ? config : ConversionConfig.mysqlToPostgres();
        this.quoteIdentifiers = this.config.isQuoteIdentifiers();
        this.convertLiterals = true;
        this.tableMappings = new HashMap<>();
        this.analysisResult = analysisResult;
        this.aliasToTable = analysisResult != null ? analysisResult.getAliasToTable() : null;
        
        // Set default converters (MySQL -> PostgreSQL)
        initDefaultConverters();
    }
    
    /**
     * Create context with multiple TableMappings for JOIN queries.
     * Uses default ConversionConfig (MySQL → PostgreSQL).
     * @param analysisResult analysis result
     * @param mappings map of table name -> TableMapping
     */
    public ClauseContext(SqlAnalysisResult analysisResult, Map<String, TableMapping> mappings) {
        this(analysisResult, ConversionConfig.mysqlToPostgres(), mappings);
    }
    
    /**
     * Create context with multiple TableMappings and ConversionConfig for JOIN queries.
     * @param analysisResult analysis result
     * @param config conversion configuration
     * @param mappings map of table name -> TableMapping
     */
    public ClauseContext(SqlAnalysisResult analysisResult, ConversionConfig config,
                         Map<String, TableMapping> mappings) {
        this.tableMapping = mappings != null && !mappings.isEmpty() 
            ? mappings.values().iterator().next() : null;
        this.config = config != null ? config : ConversionConfig.mysqlToPostgres();
        this.quoteIdentifiers = this.config.isQuoteIdentifiers();
        this.convertLiterals = true;
        this.tableMappings = mappings != null ? new HashMap<>(mappings) : new HashMap<>();
        this.analysisResult = analysisResult;
        this.aliasToTable = analysisResult != null ? analysisResult.getAliasToTable() : null;
        
        initDefaultConverters();
    }
    
    private void initDefaultConverters() {
        // Store reference to this context for use in inner class
        final ClauseContext self = this;
        
        // Default column name converter (MySQL -> PostgreSQL)
        this.columnNameConverter = new ColumnNameConverter() {
            @Override
            public String convert(String columnName, String tableAlias, ClauseContext ctx) {
                // Always clean input first
                String cleanName = ColumnNameConverter.cleanIdentifier(columnName);
                
                // Resolve mapping based on table alias or table name
                TableMapping mapping = self.resolveMappingFor(tableAlias);
                if (mapping == null) {
                    return cleanName;
                }
                return mapping.getPostgresColumnName(cleanName);
            }
            
            @Override
            public String convertAndQuote(String columnName, String tableAlias, ClauseContext ctx) {
                String converted = convert(columnName, tableAlias, ctx);
                return ctx.quoteIdentifier(converted);
            }
        };
        
        // Default table name converter (MySQL -> PostgreSQL)
        this.tableNameConverter = new TableNameConverter() {
            @Override
            public String convert(String tableName, String alias, ClauseContext ctx) {
                // Always clean input first, then return as-is (no table name mapping by default)
                return TableNameConverter.cleanIdentifier(tableName);
            }
            
            @Override
            public String convertAndQuote(String tableName, String alias, ClauseContext ctx) {
                String converted = convert(tableName, alias, ctx);
                return ctx.quoteIdentifier(converted);
            }
        };
    }
    
    /**
     * Resolve TableMapping based on alias or table name.
     * Used by column name converter.
     */
    private TableMapping resolveMappingFor(String aliasOrTableName) {
        if (aliasOrTableName == null) {
            // No alias provided, use single table mapping
            return tableMapping;
        }
        
        // First try to resolve alias to actual table name
        String actualTable = resolveTableAlias(aliasOrTableName);
        
        // Try to find mapping by resolved table name
        if (tableMappings != null && !tableMappings.isEmpty()) {
            TableMapping mapping = tableMappings.get(actualTable);
            if (mapping != null) {
                return mapping;
            }
            // Also try the original alias
            mapping = tableMappings.get(aliasOrTableName);
            if (mapping != null) {
                return mapping;
            }
        }
        
        // Fallback to single table mapping
        return tableMapping;
    }
    
    // Getters
    
    public TableMapping getTableMapping() {
        return tableMapping;
    }
    
    public Map<String, TableMapping> getTableMappings() {
        return Collections.unmodifiableMap(tableMappings);
    }
    
    public SqlAnalysisResult getAnalysisResult() {
        return analysisResult;
    }
    
    public Map<String, String> getAliasToTable() {
        return aliasToTable;
    }
    
    public ColumnNameConverter getColumnNameConverter() {
        return columnNameConverter;
    }
    
    public TableNameConverter getTableNameConverter() {
        return tableNameConverter;
    }
    
    // Setters for custom converters
    
    public void setColumnNameConverter(ColumnNameConverter converter) {
        this.columnNameConverter = converter;
    }
    
    public void setTableNameConverter(TableNameConverter converter) {
        this.tableNameConverter = converter;
    }
    
    // Configuration getters
    
    public boolean isQuoteIdentifiers() {
        return quoteIdentifiers;
    }
    
    public boolean isConvertLiterals() {
        return convertLiterals;
    }
    
    // Utility methods
    
    /**
     * Check if mapping is available.
     */
    public boolean hasMapping() {
        return tableMapping != null;
    }
    
    /**
     * Get TableMapping for a specific table name or alias.
     * @param tableNameOrAlias table name or alias
     * @return TableMapping or null if not found
     */
    public TableMapping getTableMappingFor(String tableNameOrAlias) {
        return resolveMappingFor(tableNameOrAlias);
    }
    
    // ========== Column Name Conversion ==========
    
    /**
     * Convert a simple column name (without table alias).
     * @param columnName the source column name
     * @return the converted column name (without quotes)
     */
    public String convertColumnName(String columnName) {
        return columnNameConverter.convert(columnName, null, this);
    }
    
    /**
     * Convert a qualified column name with table alias.
     * @param columnName the source column name
     * @param tableAlias the table alias
     * @return the converted column name (without quotes)
     */
    public String convertColumnName(String columnName, String tableAlias) {
        return columnNameConverter.convert(columnName, tableAlias, this);
    }
    
    /**
     * Convert and quote a simple column name (without table alias).
     * @param columnName the source column name
     * @return the converted and quoted column name
     */
    public String convertAndQuoteColumnName(String columnName) {
        return columnNameConverter.convertAndQuote(columnName, null, this);
    }
    
    /**
     * Convert and quote a qualified column name with table alias.
     * @param columnName the source column name
     * @param tableAlias the table alias
     * @return the converted and quoted column name
     */
    public String convertAndQuoteColumnName(String columnName, String tableAlias) {
        return columnNameConverter.convertAndQuote(columnName, tableAlias, this);
    }
    
    // ========== Table Name Conversion ==========
    
    /**
     * Convert a table name (without alias).
     * @param tableName the source table name
     * @return the converted table name (without quotes)
     */
    public String convertTableName(String tableName) {
        return tableNameConverter.convert(tableName, null, this);
    }
    
    /**
     * Convert a table name with alias context.
     * @param tableName the source table name
     * @param alias the table alias
     * @return the converted table name (without quotes)
     */
    public String convertTableName(String tableName, String alias) {
        return tableNameConverter.convert(tableName, alias, this);
    }
    
    /**
     * Convert and quote a table name (without alias).
     * @param tableName the source table name
     * @return the converted and quoted table name
     */
    public String convertAndQuoteTableName(String tableName) {
        return tableNameConverter.convertAndQuote(tableName, null, this);
    }
    
    /**
     * Convert and quote a table name with alias context.
     * @param tableName the source table name
     * @param alias the table alias
     * @return the converted and quoted table name
     */
    public String convertAndQuoteTableName(String tableName, String alias) {
        return tableNameConverter.convertAndQuote(tableName, alias, this);
    }
    
    /**
     * Quote an identifier for the target database.
     * Uses QuoteHelper from common module for dialect-specific quoting.
     * 
     * @param name the identifier to quote (should be clean, without surrounding quotes)
     * @return the quoted identifier
     */
    public String quoteIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        if (!quoteIdentifiers || config == null) {
            return name;
        }
        
        DbType targetDbType = config.getTargetDbType();
        return QuoteHelper.quote(name, targetDbType);
    }
    
    /**
     * Get the target database type.
     */
    public DbType getTargetDbType() {
        return config != null ? config.getTargetDbType() : DbType.postgresql;
    }
    
    /**
     * Get the source database type.
     */
    public DbType getSourceDbType() {
        return config != null ? config.getSourceDbType() : DbType.mysql;
    }
    
    /**
     * Get the configuration.
     */
    public ConversionConfig getConfig() {
        return config;
    }
    
    /**
     * Get target column type for a source column.
     * Default implementation uses TableMapping for MySQL->PostgreSQL.
     */
    public String getColumnType(String columnName) {
        if (tableMapping == null) {
            return null;
        }
        return tableMapping.getPostgresColumnType(columnName);
    }
    
    /**
     * Check if column needs alias (name changed).
     */
    public boolean needsAlias(String mysqlColumnName) {
        if (tableMapping == null) {
            return false;
        }
        return tableMapping.needsAlias(mysqlColumnName);
    }
    
    /**
     * Resolve actual table name from alias.
     */
    public String resolveTableAlias(String alias) {
        if (aliasToTable == null || alias == null) {
            return alias;
        }
        return aliasToTable.getOrDefault(alias, alias);
    }
    
    /**
     * Add alias mapping to analysis result.
     */
    public void addAlias(String alias, String tableName) {
        if (analysisResult != null) {
            analysisResult.addAlias(alias, tableName);
        }
    }
}
