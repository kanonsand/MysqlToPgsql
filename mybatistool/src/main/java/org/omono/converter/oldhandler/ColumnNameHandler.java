package org.omono.converter.oldhandler;

/**
 * Handler for column name conversion
 * Always wraps column names with double quotes to handle PostgreSQL reserved words
 */
public class ColumnNameHandler {

    /**
     * Convert MySQL column name to PostgreSQL format
     * Always wraps with double quotes to handle reserved words like "user", "order", "group"
     * 
     * @param columnName the MySQL column name (may contain backticks)
     * @return the PostgreSQL column name wrapped in double quotes
     */
    public static String convert(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return columnName;
        }
        
        // Remove existing backticks
        String name = columnName.replace("`", "");
        
        // Remove existing double quotes (in case already quoted)
        name = name.replace("\"", "");
        
        // Always wrap with double quotes
        return "\"" + name + "\"";
    }

    /**
     * Convert column name without adding quotes if already quoted
     * 
     * @param columnName the column name
     * @param alreadyQuoted whether the name is already quoted
     * @return the converted column name
     */
    public static String convert(String columnName, boolean alreadyQuoted) {
        if (alreadyQuoted) {
            return columnName;
        }
        return convert(columnName);
    }
}
