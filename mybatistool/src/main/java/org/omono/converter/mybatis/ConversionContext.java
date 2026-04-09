package org.omono.converter.mybatis;

import org.omono.converter.common.TypeCategory;
import org.apache.ibatis.mapping.BoundSql;

/**
 * Conversion context for passing information between interceptors.
 * Stored in BoundSql.additionalParameters with key "__conversion_context__".
 * 
 * Note: Parameter remapping is NOT needed because:
 * 1. INSERT with column names: Database maps by column name automatically
 * 2. INSERT without columns: We add column names in MySQL order, so parameters match
 */
public class ConversionContext {
    
    private static final String CONTEXT_KEY = "__conversion_context__";
    
    private String tableName;                           // Table name
    private StatementType statementType;                // Statement type
    private TypeCategory[] parameterCategories;  // Parameter type categories
    private boolean needsTypeConversion;                // Whether type conversion is needed
    
    public enum StatementType {
        INSERT,
        UPDATE,
        DELETE,
        SELECT
    }
    
    public ConversionContext() {
        this.needsTypeConversion = false;
    }
    
    /**
     * Get parameter type category
     * @param index Parameter index (1-based)
     * @return TypeCategory
     */
    public TypeCategory getParameterCategory(int index) {
        if (parameterCategories == null || index < 1 || index > parameterCategories.length) {
            return TypeCategory.OTHER;
        }
        return parameterCategories[index - 1];
    }
    
    /**
     * Attach context to BoundSql
     */
    public void attachTo(BoundSql boundSql) {
        boundSql.setAdditionalParameter(CONTEXT_KEY, this);
    }
    
    /**
     * Get context from BoundSql
     */
    public static ConversionContext from(BoundSql boundSql) {
        Object obj = boundSql.getAdditionalParameter(CONTEXT_KEY);
        return obj instanceof ConversionContext ? (ConversionContext) obj : null;
    }
    
    /**
     * Check if conversion is needed
     */
    public boolean needsConversion() {
        return needsTypeConversion;
    }
    
    // Getters and Setters
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public StatementType getStatementType() {
        return statementType;
    }
    
    public void setStatementType(StatementType statementType) {
        this.statementType = statementType;
    }
    
    public TypeCategory[] getParameterCategories() {
        return parameterCategories;
    }
    
    public void setParameterCategories(TypeCategory[] parameterCategories) {
        this.parameterCategories = parameterCategories;
        if (parameterCategories != null) {
            for (TypeCategory category : parameterCategories) {
                if (category != null && category != TypeCategory.OTHER) {
                    this.needsTypeConversion = true;
                    break;
                }
            }
        }
    }
    
    public boolean isNeedsTypeConversion() {
        return needsTypeConversion;
    }
    
    public int getParameterCount() {
        return parameterCategories != null ? parameterCategories.length : 0;
    }
    
    @Override
    public String toString() {
        return "ConversionContext{" +
                "tableName='" + tableName + '\'' +
                ", statementType=" + statementType +
                ", parameterCount=" + getParameterCount() +
                ", needsTypeConversion=" + needsTypeConversion +
                '}';
    }
}