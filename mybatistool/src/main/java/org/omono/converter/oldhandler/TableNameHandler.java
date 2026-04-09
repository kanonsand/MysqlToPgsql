package org.omono.converter.oldhandler;

/**
 * Handler for table name conversion
 * Always wraps table names with double quotes to handle PostgreSQL reserved words
 */
public class TableNameHandler {

    /**
     * Convert MySQL table name to PostgreSQL format
     * Always wraps with double quotes to handle reserved words like "user", "order", "group"
     * 
     * @param tableName the MySQL table name (may contain backticks)
     * @return the PostgreSQL table name wrapped in double quotes
     */
    public static String convert(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return tableName;
        }
        
        // Remove existing backticks
        String name = tableName.replace("`", "");
        
        // Remove existing double quotes (in case already quoted)
        name = name.replace("\"", "");
        
        // Always wrap with double quotes
        return "\"" + name + "\"";
    }

    /**
     * Convert table name without adding quotes if already quoted
     * 
     * @param tableName the table name
     * @param alreadyQuoted whether the name is already quoted
     * @return the converted table name
     */
    public static String convert(String tableName, boolean alreadyQuoted) {
        if (alreadyQuoted) {
            return tableName;
        }
        return convert(tableName);
    }
}