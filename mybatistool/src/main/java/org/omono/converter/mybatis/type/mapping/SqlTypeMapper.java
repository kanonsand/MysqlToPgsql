package org.omono.converter.mybatis.type.mapping;

import org.omono.converter.common.TypeCategory;

/**
 * Utility class for mapping SQL types to Java types
 */
public class SqlTypeMapper {
    
    /**
     * Get Java type from SQL type
     * @param sqlType SQL type string (e.g., "VARCHAR", "INT")
     * @return Java type string (e.g., "String", "Integer")
     */
    public static String toJavaType(String sqlType) {
        if (sqlType == null) {
            return "Object";
        }
        
        String type = sqlType.toUpperCase();
        
        // Long types (integers)
        if (isLongType(type)) {
            if (type.contains("BIGINT") || type.equals("BIGSERIAL")) {
                return "Long";
            } else if (type.contains("SMALLINT")) {
                return "Short";
            } else if (type.contains("TINYINT")) {
                return "Byte";
            }
            return "Integer";
        }
        
        // Double types (floating point)
        if (isDoubleType(type)) {
            return "Double";
        }
        
        // Decimal types
        if (isDecimalType(type)) {
            return "java.math.BigDecimal";
        }
        
        // String types
        if (type.contains("CHAR") || type.contains("TEXT") || type.contains("CLOB")) {
            return "String";
        }
        
        // Date/Time types
        if (type.contains("DATE") || type.contains("TIME") || type.contains("TIMESTAMP")) {
            return "java.sql.Timestamp";
        }
        
        // Boolean
        if (type.contains("BOOLEAN") || type.equals("BIT")) {
            return "Boolean";
        }
        
        // Binary types
        if (type.contains("BINARY") || type.contains("BLOB") || type.contains("BYTEA")) {
            return "byte[]";
        }
        
        return "Object";
    }
    
    /**
     * Get type category from SQL type
     * @param sqlType SQL type string
     * @return TypeCategory
     */
    public static TypeCategory getCategory(String sqlType) {
        if (sqlType == null) {
            return TypeCategory.OTHER;
        }
        
        String type = sqlType.toUpperCase();
        
        // Long types (integers)
        if (isLongType(type)) {
            return TypeCategory.LONG;
        }
        
        // Double types (floating point)
        if (isDoubleType(type)) {
            return TypeCategory.DOUBLE;
        }
        
        // Decimal types
        if (isDecimalType(type)) {
            return TypeCategory.DECIMAL;
        }
        
        // String types
        if (type.contains("CHAR") || type.contains("TEXT") || type.contains("CLOB")) {
            return TypeCategory.STRING;
        }
        
        // Date/Time types
        if (type.contains("DATE") || type.contains("TIME") || type.contains("TIMESTAMP")) {
            return TypeCategory.DATE;
        }
        
        // Boolean
        if (type.contains("BOOLEAN") || type.equals("BIT")) {
            return TypeCategory.BOOLEAN;
        }
        
        // Binary types
        if (type.contains("BINARY") || type.contains("BLOB") || type.contains("BYTEA")) {
            return TypeCategory.BINARY;
        }
        
        return TypeCategory.OTHER;
    }
    
    /**
     * Check if SQL type is a long/integer type
     */
    private static boolean isLongType(String type) {
        return (type.contains("INT") && !type.contains("BIGINT")) || 
               type.equals("SERIAL") ||
               type.contains("BIGINT") ||
               type.equals("BIGSERIAL") ||
               type.contains("SMALLINT") ||
               type.contains("TINYINT");
    }
    
    /**
     * Check if SQL type is a double/float type
     */
    private static boolean isDoubleType(String type) {
        return type.contains("FLOAT") || 
               type.contains("DOUBLE") || 
               type.contains("REAL");
    }
    
    /**
     * Check if SQL type is a decimal/numeric type
     */
    private static boolean isDecimalType(String type) {
        return type.contains("DECIMAL") || 
               type.contains("NUMERIC") || 
               type.contains("NUMBER");
    }
    
    /**
     * Check if SQL type is numeric (LONG, DOUBLE, or DECIMAL)
     * @param sqlType SQL type string
     * @return true if numeric
     */
    public static boolean isNumeric(String sqlType) {
        TypeCategory category = getCategory(sqlType);
        return category == TypeCategory.LONG || 
               category == TypeCategory.DOUBLE || 
               category == TypeCategory.DECIMAL;
    }
    
    /**
     * Check if SQL type is string
     * @param sqlType SQL type string
     * @return true if string
     */
    public static boolean isString(String sqlType) {
        return getCategory(sqlType) == TypeCategory.STRING;
    }
    
    /**
     * Check if SQL type is date/time
     * @param sqlType SQL type string
     * @return true if date/time
     */
    public static boolean isDate(String sqlType) {
        return getCategory(sqlType) == TypeCategory.DATE;
    }
}
