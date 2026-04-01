package converter.schema;

/**
 * Utility class for mapping SQL types to Java types
 */
public class SqlTypeMapper {
    
    /**
     * Type category for classification
     */
    public enum TypeCategory {
        NUMERIC,
        STRING,
        DATE,
        BOOLEAN,
        BINARY,
        OTHER
    }
    
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
        
        // Numeric types
        if (type.contains("INT") || type.equals("SERIAL") || type.equals("BIGSERIAL")) {
            if (type.contains("BIGINT") || type.equals("BIGSERIAL")) {
                return "Long";
            } else if (type.contains("SMALLINT")) {
                return "Short";
            } else if (type.contains("TINYINT")) {
                return "Byte";
            }
            return "Integer";
        }
        
        if (type.contains("FLOAT") || type.contains("DOUBLE") || type.contains("DECIMAL") || type.contains("NUMERIC") || type.contains("NUMBER")) {
            if (type.contains("DECIMAL") || type.contains("NUMERIC")) {
                return "java.math.BigDecimal";
            }
            return "Double";
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
        
        // Numeric
        if (type.contains("INT") || type.contains("FLOAT") || type.contains("DOUBLE") || 
            type.contains("DECIMAL") || type.contains("NUMERIC") || type.contains("NUMBER") ||
            type.equals("SERIAL") || type.equals("BIGSERIAL")) {
            return TypeCategory.NUMERIC;
        }
        
        // String
        if (type.contains("CHAR") || type.contains("TEXT") || type.contains("CLOB")) {
            return TypeCategory.STRING;
        }
        
        // Date/Time
        if (type.contains("DATE") || type.contains("TIME") || type.contains("TIMESTAMP")) {
            return TypeCategory.DATE;
        }
        
        // Boolean
        if (type.contains("BOOLEAN") || type.equals("BIT")) {
            return TypeCategory.BOOLEAN;
        }
        
        // Binary
        if (type.contains("BINARY") || type.contains("BLOB") || type.contains("BYTEA")) {
            return TypeCategory.BINARY;
        }
        
        return TypeCategory.OTHER;
    }
    
    /**
     * Check if SQL type is numeric
     * @param sqlType SQL type string
     * @return true if numeric
     */
    public static boolean isNumeric(String sqlType) {
        return getCategory(sqlType) == TypeCategory.NUMERIC;
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
