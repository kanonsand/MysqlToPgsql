package org.omono.converter.comparator.context;

import com.alibaba.druid.DbType;
import org.omono.converter.common.TypeCategory;

import java.util.*;
import java.util.function.BiPredicate;

/**
 * Context for SQL AST comparison.
 * Defines what differences are allowed between source and target SQL.
 * 
 * Use {@link #builder()} to create instances:
 * <pre>
 * CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
 *     .sourceQuotes(CompareContext.MYSQL_QUOTES)
 *     .targetQuotes(CompareContext.POSTGRES_QUOTES)
 *     .withDefaultTypeMatchers()
 *     .columnMapping("user_name", "name")
 *     .build();
 * </pre>
 */
public class CompareContext {
    
    // ========== 预定义引号配置 ==========
    
    public static final List<IdentifierQuotePair> MYSQL_QUOTES = List.of(
        IdentifierQuotePair.of('`'),
        IdentifierQuotePair.of('"')
    );
    public static final List<IdentifierQuotePair> POSTGRES_QUOTES = List.of(
        IdentifierQuotePair.of('"')
    );
    public static final List<IdentifierQuotePair> SQLSERVER_QUOTES = List.of(
        IdentifierQuotePair.of('[', ']'),
        IdentifierQuotePair.of('"')
    );
    public static final List<IdentifierQuotePair> SQLITE_QUOTES = List.of(
        IdentifierQuotePair.of('[', ']'),
        IdentifierQuotePair.of('`'),
        IdentifierQuotePair.of('"')
    );
    public static final List<IdentifierQuotePair> ORACLE_QUOTES = List.of(
        IdentifierQuotePair.of('"')
    );
    public static final List<IdentifierQuotePair> DEFAULT_QUOTES = List.of(
        IdentifierQuotePair.of('"')
    );
    
    /**
     * Get quote pairs for a specific database type.
     */
    public static List<IdentifierQuotePair> getQuotesByDbType(DbType dbType) {
        if (dbType == null) {
            return DEFAULT_QUOTES;
        }
        switch (dbType) {
            case mysql:
                return MYSQL_QUOTES;
            case postgresql:
                return POSTGRES_QUOTES;
            case sqlserver:
                return SQLSERVER_QUOTES;
            case sqlite:
                return SQLITE_QUOTES;
            case oracle:
                return ORACLE_QUOTES;
            default:
                return DEFAULT_QUOTES;
        }
    }
    
    // ========== TypePair key for matcher map ==========
    
    /**
     * Key for type matcher map, holds a pair of TypeCategories.
     */
    public static final class TypePair {
        private final TypeCategory source;
        private final TypeCategory target;
        
        public TypePair(TypeCategory source, TypeCategory target) {
            this.source = source;
            this.target = target;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TypePair typePair = (TypePair) o;
            return source == typePair.source && target == typePair.target;
        }
        
        @Override
        public int hashCode() {
            return 31 * source.hashCode() + target.hashCode();
        }
    }
    
    // ========== 实例字段 ==========
    
    private final DbType sourceDbType;
    private final DbType targetDbType;
    private final List<IdentifierQuotePair> sourceQuotePairs;
    private final List<IdentifierQuotePair> targetQuotePairs;
    private final Map<String, String> columnMappings;
    private final Map<String, String> tableMappings;
    private final Map<TypePair, BiPredicate<Object, Object>> typeMatchers;
    
    // ========== 构造方法 ==========
    
    private CompareContext(Builder builder) {
        this.sourceDbType = builder.sourceDbType;
        this.targetDbType = builder.targetDbType;
        this.sourceQuotePairs = Collections.unmodifiableList(new ArrayList<>(builder.sourceQuotePairs));
        this.targetQuotePairs = Collections.unmodifiableList(new ArrayList<>(builder.targetQuotePairs));
        this.columnMappings = Collections.unmodifiableMap(new HashMap<>(builder.columnMappings));
        this.tableMappings = Collections.unmodifiableMap(new HashMap<>(builder.tableMappings));
        this.typeMatchers = Collections.unmodifiableMap(new HashMap<>(builder.typeMatchers));
    }
    
    // ========== 静态工厂方法 ==========
    
    /**
     * Create a new Builder for constructing CompareContext.
     * @param sourceDbType source database type
     * @param targetDbType target database type
     */
    public static Builder builder(DbType sourceDbType, DbType targetDbType) {
        return new Builder(sourceDbType, targetDbType);
    }
    
    /**
     * Create a context for MySQL → PostgreSQL comparison with default type matchers.
     */
    public static CompareContext mySqlToPostgres() {
        return builder(DbType.mysql, DbType.postgresql)
            .sourceQuotes(MYSQL_QUOTES)
            .targetQuotes(POSTGRES_QUOTES)
            .withDefaultTypeMatchers()
            .build();
    }
    
    /**
     * Create a context for comparison between specified database types with default type matchers.
     */
    public static CompareContext forDbTypes(DbType sourceDbType, DbType targetDbType) {
        return builder(sourceDbType, targetDbType)
            .sourceQuotes(getQuotesByDbType(sourceDbType))
            .targetQuotes(getQuotesByDbType(targetDbType))
            .withDefaultTypeMatchers()
            .build();
    }
    
    // ========== 查询方法 ==========
    
    public DbType getSourceDbType() {
        return sourceDbType;
    }
    
    public DbType getTargetDbType() {
        return targetDbType;
    }
    
    public String getMappedColumnName(String sourceCol) {
        return columnMappings.get(sourceCol);
    }
    
    public String getMappedTableName(String sourceTable) {
        return tableMappings.get(sourceTable);
    }
    
    /**
     * Get the type matcher for comparing values of different types.
     */
    public BiPredicate<Object, Object> getTypeMatcher(TypeCategory source, TypeCategory target) {
        return typeMatchers.get(new TypePair(source, target));
    }
    
    // ========== 引号处理 ==========
    
    public String stripSourceQuotes(String identifier) {
        for (IdentifierQuotePair pair : sourceQuotePairs) {
            if (pair.matches(identifier)) {
                return pair.strip(identifier);
            }
        }
        return identifier;
    }
    
    public String stripTargetQuotes(String identifier) {
        for (IdentifierQuotePair pair : targetQuotePairs) {
            if (pair.matches(identifier)) {
                return pair.strip(identifier);
            }
        }
        return identifier;
    }
    
    // ========== 标识符比较 ==========
    
    public String normalizeColumnName(String name, boolean isSource) {
        String stripped = isSource ? stripSourceQuotes(name) : stripTargetQuotes(name);
        String mapped = getMappedColumnName(stripped);
        return mapped != null ? mapped : stripped;
    }
    
    public String normalizeTableName(String name, boolean isSource) {
        String stripped = isSource ? stripSourceQuotes(name) : stripTargetQuotes(name);
        String mapped = getMappedTableName(stripped);
        return mapped != null ? mapped : stripped;
    }
    
    public boolean isEquivalentIdentifier(String source, String target, boolean isColumn) {
        String normSource = isColumn ? normalizeColumnName(source, true) : normalizeTableName(source, true);
        String normTarget = isColumn ? normalizeColumnName(target, false) : normalizeTableName(target, false);
        return normSource.equalsIgnoreCase(normTarget);
    }
    
    // ========== Builder 类 ==========
    
    public static class Builder {
        private final DbType sourceDbType;
        private final DbType targetDbType;
        private final List<IdentifierQuotePair> sourceQuotePairs = new ArrayList<>();
        private final List<IdentifierQuotePair> targetQuotePairs = new ArrayList<>();
        private final Map<String, String> columnMappings = new HashMap<>();
        private final Map<String, String> tableMappings = new HashMap<>();
        private final Map<TypePair, BiPredicate<Object, Object>> typeMatchers = new HashMap<>();
        
        private Builder(DbType sourceDbType, DbType targetDbType) {
            this.sourceDbType = sourceDbType;
            this.targetDbType = targetDbType;
            // 不设置默认引号，由用户显式提供
            // 如果不提供，则直接比较完整字符串
        }
        
        /**
         * Set source quote pairs.
         */
        public Builder sourceQuotes(List<IdentifierQuotePair> pairs) {
            sourceQuotePairs.clear();
            sourceQuotePairs.addAll(pairs);
            return this;
        }
        
        /**
         * Set target quote pairs.
         */
        public Builder targetQuotes(List<IdentifierQuotePair> pairs) {
            targetQuotePairs.clear();
            targetQuotePairs.addAll(pairs);
            return this;
        }
        
        /**
         * Add a column name mapping.
         */
        public Builder columnMapping(String sourceCol, String targetCol) {
            columnMappings.put(sourceCol, targetCol);
            return this;
        }
        
        /**
         * Add multiple column name mappings.
         */
        public Builder columnMappings(Map<String, String> mappings) {
            columnMappings.putAll(mappings);
            return this;
        }
        
        /**
         * Add a table name mapping.
         */
        public Builder tableMapping(String sourceTable, String targetTable) {
            tableMappings.put(sourceTable, targetTable);
            return this;
        }
        
        /**
         * Add multiple table name mappings.
         */
        public Builder tableMappings(Map<String, String> mappings) {
            tableMappings.putAll(mappings);
            return this;
        }
        
        /**
         * Add a type matcher for comparing values of different types.
         */
        public Builder typeMatcher(TypeCategory source, TypeCategory target, 
                                   BiPredicate<Object, Object> matcher) {
            typeMatchers.put(new TypePair(source, target), matcher);
            return this;
        }
        
        /**
         * Add default type matchers for common MySQL → PostgreSQL conversions.
         */
        public Builder withDefaultTypeMatchers() {
            // LONG ↔ BOOLEAN (MySQL 0/1 ↔ PostgreSQL true/false)
            typeMatcher(TypeCategory.LONG, TypeCategory.BOOLEAN, TypeMatchers.longToBoolean());
            typeMatcher(TypeCategory.BOOLEAN, TypeCategory.LONG, TypeMatchers.booleanToLong());
            
            // LONG ↔ DOUBLE
            typeMatcher(TypeCategory.LONG, TypeCategory.DOUBLE, TypeMatchers.longToDouble());
            typeMatcher(TypeCategory.DOUBLE, TypeCategory.LONG, TypeMatchers.doubleToLong());
            
            // LONG ↔ STRING
            typeMatcher(TypeCategory.LONG, TypeCategory.STRING, TypeMatchers.longToString());
            typeMatcher(TypeCategory.STRING, TypeCategory.LONG, TypeMatchers.stringToLong());
            
            // DOUBLE ↔ STRING
            typeMatcher(TypeCategory.DOUBLE, TypeCategory.STRING, TypeMatchers.doubleToString());
            typeMatcher(TypeCategory.STRING, TypeCategory.DOUBLE, TypeMatchers.stringToDouble());
            
            // BOOLEAN ↔ STRING
            typeMatcher(TypeCategory.BOOLEAN, TypeCategory.STRING, TypeMatchers.booleanToString());
            typeMatcher(TypeCategory.STRING, TypeCategory.BOOLEAN, TypeMatchers.stringToBoolean());
            
            return this;
        }
        
        public CompareContext build() {
            return new CompareContext(this);
        }
    }
}