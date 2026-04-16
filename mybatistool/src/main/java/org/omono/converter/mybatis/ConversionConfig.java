package org.omono.converter.mybatis;

import com.alibaba.druid.DbType;
import org.omono.converter.mybatis.function.DefaultFunctionConverter;
import org.omono.converter.mybatis.function.FunctionConverter;

import java.util.*;

/**
 * SQL conversion configuration with Builder pattern.
 * 
 * Use {@link #builder()} for custom configuration:
 * <pre>
 * ConversionConfig config = ConversionConfig.builder(DbType.mysql, DbType.postgresql)
 *     .quoteIdentifiers(true)
 *     .addFunctionConverter(new DefaultFunctionConverter())
 *     .build();
 * </pre>
 * 
 * Or use factory methods for common scenarios:
 * <pre>
 * ConversionConfig config = ConversionConfig.mysqlToPostgres();
 * </pre>
 */
public class ConversionConfig {
    
    // ========== Instance fields ==========
    
    private final DbType sourceDbType;
    private final DbType targetDbType;
    private final boolean quoteIdentifiers;
    private final boolean enabled;
    private final boolean strictMode;
    private final Set<String> excludedTables;
    private final Set<String> excludedSqlIds;
    private final boolean insertEnabled;
    private final boolean updateEnabled;
    private final boolean deleteEnabled;
    private final boolean selectEnabled;
    private final boolean cacheEnabled;
    private final int cacheMaxSize;
    private final List<FunctionConverter> functionConverters;
    
    // ========== Constructor ==========
    
    private ConversionConfig(Builder builder) {
        this.sourceDbType = builder.sourceDbType;
        this.targetDbType = builder.targetDbType;
        this.quoteIdentifiers = builder.quoteIdentifiers;
        this.enabled = builder.enabled;
        this.strictMode = builder.strictMode;
        this.excludedTables = Collections.unmodifiableSet(new HashSet<>(builder.excludedTables));
        this.excludedSqlIds = Collections.unmodifiableSet(new HashSet<>(builder.excludedSqlIds));
        this.insertEnabled = builder.insertEnabled;
        this.updateEnabled = builder.updateEnabled;
        this.deleteEnabled = builder.deleteEnabled;
        this.selectEnabled = builder.selectEnabled;
        this.cacheEnabled = builder.cacheEnabled;
        this.cacheMaxSize = builder.cacheMaxSize;
        this.functionConverters = Collections.unmodifiableList(new ArrayList<>(builder.functionConverters));
    }
    
    // ========== Factory methods ==========
    
    /**
     * Create configuration for MySQL → PostgreSQL conversion.
     */
    public static ConversionConfig mysqlToPostgres() {
        return builder(DbType.mysql, DbType.postgresql)
            .addFunctionConverter(new DefaultFunctionConverter())
            .build();
    }
    
    /**
     * Create configuration for MySQL → Oracle conversion.
     */
    public static ConversionConfig mysqlToOracle() {
        return builder(DbType.mysql, DbType.oracle)
            .addFunctionConverter(new DefaultFunctionConverter())
            .build();
    }
    
    /**
     * Create configuration for MySQL → SQL Server conversion.
     */
    public static ConversionConfig mysqlToSqlServer() {
        return builder(DbType.mysql, DbType.sqlserver)
            .addFunctionConverter(new DefaultFunctionConverter())
            .build();
    }
    
    /**
     * Create configuration for arbitrary source → target conversion.
     */
    public static ConversionConfig forDbTypes(DbType sourceDbType, DbType targetDbType) {
        return builder(sourceDbType, targetDbType)
            .addFunctionConverter(new DefaultFunctionConverter())
            .build();
    }
    
    /**
     * Create a new Builder for constructing ConversionConfig.
     * 
     * @param sourceDbType source database type
     * @param targetDbType target database type
     */
    public static Builder builder(DbType sourceDbType, DbType targetDbType) {
        return new Builder(sourceDbType, targetDbType);
    }
    
    /**
     * Create a new Builder with default MySQL → PostgreSQL settings.
     */
    public static Builder builder() {
        return new Builder(DbType.mysql, DbType.postgresql);
    }
    
    // ========== Getters ==========
    
    public DbType getSourceDbType() { return sourceDbType; }
    public DbType getTargetDbType() { return targetDbType; }
    public boolean isQuoteIdentifiers() { return quoteIdentifiers; }
    public boolean isEnabled() { return enabled; }
    public boolean isStrictMode() { return strictMode; }
    public Set<String> getExcludedTables() { return excludedTables; }
    public Set<String> getExcludedSqlIds() { return excludedSqlIds; }
    public boolean isInsertEnabled() { return insertEnabled; }
    public boolean isUpdateEnabled() { return updateEnabled; }
    public boolean isDeleteEnabled() { return deleteEnabled; }
    public boolean isSelectEnabled() { return selectEnabled; }
    public boolean isCacheEnabled() { return cacheEnabled; }
    public int getCacheMaxSize() { return cacheMaxSize; }
    public List<FunctionConverter> getFunctionConverters() { return functionConverters; }
    
    public boolean isTableExcluded(String tableName) {
        return tableName != null && excludedTables.contains(tableName.toLowerCase());
    }
    
    public boolean isSqlIdExcluded(String sqlId) {
        return sqlId != null && excludedSqlIds.contains(sqlId);
    }
    
    /**
     * Convert function using registered converters
     * @param functionName function name (uppercase)
     * @param args function arguments string
     * @return converted expression, null if no converter handles it
     */
    public String convertFunction(String functionName, String args) {
        for (FunctionConverter converter : functionConverters) {
            String result = converter.convert(functionName, args);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
    
    // ========== Builder class ==========
    
    public static class Builder {
        private final DbType sourceDbType;
        private final DbType targetDbType;
        private boolean quoteIdentifiers = true;
        private boolean enabled = true;
        private boolean strictMode = false;
        private final Set<String> excludedTables = new HashSet<>();
        private final Set<String> excludedSqlIds = new HashSet<>();
        private boolean insertEnabled = true;
        private boolean updateEnabled = true;
        private boolean deleteEnabled = true;
        private boolean selectEnabled = true;
        private boolean cacheEnabled = true;
        private int cacheMaxSize = 1000;
        private final List<FunctionConverter> functionConverters = new ArrayList<>();
        
        private Builder(DbType sourceDbType, DbType targetDbType) {
            this.sourceDbType = sourceDbType;
            this.targetDbType = targetDbType;
        }
        
        public Builder quoteIdentifiers(boolean quoteIdentifiers) {
            this.quoteIdentifiers = quoteIdentifiers;
            return this;
        }
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder strictMode(boolean strictMode) {
            this.strictMode = strictMode;
            return this;
        }
        
        public Builder addExcludedTable(String tableName) {
            this.excludedTables.add(tableName.toLowerCase());
            return this;
        }
        
        public Builder excludedTables(Set<String> tables) {
            this.excludedTables.clear();
            if (tables != null) {
                for (String table : tables) {
                    this.excludedTables.add(table.toLowerCase());
                }
            }
            return this;
        }
        
        public Builder addExcludedSqlId(String sqlId) {
            this.excludedSqlIds.add(sqlId);
            return this;
        }
        
        public Builder excludedSqlIds(Set<String> sqlIds) {
            this.excludedSqlIds.clear();
            if (sqlIds != null) {
                this.excludedSqlIds.addAll(sqlIds);
            }
            return this;
        }
        
        public Builder insertEnabled(boolean enabled) {
            this.insertEnabled = enabled;
            return this;
        }
        
        public Builder updateEnabled(boolean enabled) {
            this.updateEnabled = enabled;
            return this;
        }
        
        public Builder deleteEnabled(boolean enabled) {
            this.deleteEnabled = enabled;
            return this;
        }
        
        public Builder selectEnabled(boolean enabled) {
            this.selectEnabled = enabled;
            return this;
        }
        
        public Builder cacheEnabled(boolean enabled) {
            this.cacheEnabled = enabled;
            return this;
        }
        
        public Builder cacheMaxSize(int maxSize) {
            this.cacheMaxSize = maxSize;
            return this;
        }
        
        public Builder addFunctionConverter(FunctionConverter converter) {
            if (converter != null) {
                this.functionConverters.add(converter);
            }
            return this;
        }
        
        public Builder functionConverters(List<FunctionConverter> converters) {
            this.functionConverters.clear();
            if (converters != null) {
                this.functionConverters.addAll(converters);
            }
            return this;
        }
        
        public ConversionConfig build() {
            return new ConversionConfig(this);
        }
    }
}