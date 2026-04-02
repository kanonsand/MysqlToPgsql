package converter.mybatis;

import converter.schema.SqlTypeMapper;
import converter.schema.TableMapping;
import org.apache.ibatis.mapping.BoundSql;

/**
 * Conversion context for passing information between interceptors.
 * Stored in BoundSql.additionalParameters with key "__conversion_context__".
 */
public class ConversionContext {
    
    private static final String CONTEXT_KEY = "__conversion_context__";
    
    private String tableName;                           // Table name
    private StatementType statementType;                // Statement type
    private int[] parameterMapping;                     // Parameter position mapping (original index -> new index, 0-based)
    private SqlTypeMapper.TypeCategory[] parameterCategories;  // Parameter type categories
    private boolean needsParameterRemapping;            // Whether parameter remapping is needed
    private boolean needsTypeConversion;                // Whether type conversion is needed
    
    public enum StatementType {
        INSERT,
        UPDATE,
        DELETE,
        SELECT
    }
    
    public ConversionContext() {
        this.needsParameterRemapping = false;
        this.needsTypeConversion = false;
    }
    
    /**
     * Get parameter type category
     * @param index Parameter index (1-based)
     * @return TypeCategory
     */
    public SqlTypeMapper.TypeCategory getParameterCategory(int index) {
        if (parameterCategories == null || index < 1 || index > parameterCategories.length) {
            return SqlTypeMapper.TypeCategory.OTHER;
        }
        return parameterCategories[index - 1];
    }
    
    /**
     * Remap parameter index
     * @param originalIndex Original index (1-based)
     * @return New index (1-based)
     */
    public int remapParameterIndex(int originalIndex) {
        if (parameterMapping == null || !needsParameterRemapping) {
            return originalIndex;
        }
        if (originalIndex < 1 || originalIndex > parameterMapping.length) {
            return originalIndex;
        }
        return parameterMapping[originalIndex - 1] + 1;
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
        return needsParameterRemapping || needsTypeConversion;
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
    
    public int[] getParameterMapping() {
        return parameterMapping;
    }
    
    public void setParameterMapping(int[] parameterMapping) {
        this.parameterMapping = parameterMapping;
        this.needsParameterRemapping = parameterMapping != null && parameterMapping.length > 0;
    }
    
    public SqlTypeMapper.TypeCategory[] getParameterCategories() {
        return parameterCategories;
    }
    
    public void setParameterCategories(SqlTypeMapper.TypeCategory[] parameterCategories) {
        this.parameterCategories = parameterCategories;
        if (parameterCategories != null) {
            for (SqlTypeMapper.TypeCategory category : parameterCategories) {
                if (category != null && category != SqlTypeMapper.TypeCategory.OTHER) {
                    this.needsTypeConversion = true;
                    break;
                }
            }
        }
    }
    
    public boolean isNeedsParameterRemapping() {
        return needsParameterRemapping;
    }
    
    public boolean isNeedsTypeConversion() {
        return needsTypeConversion;
    }
    
    public int getParameterCount() {
        return parameterMapping != null ? parameterMapping.length : 0;
    }
    
    @Override
    public String toString() {
        return "ConversionContext{" +
                "tableName='" + tableName + '\'' +
                ", statementType=" + statementType +
                ", parameterCount=" + getParameterCount() +
                ", needsParameterRemapping=" + needsParameterRemapping +
                ", needsTypeConversion=" + needsTypeConversion +
                '}';
    }
}
