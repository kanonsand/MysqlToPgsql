package org.omono.converter.mybatis.clause;

/**
 * Converts column names from source database to target database format.
 * Implementations can handle different database dialects including name conversion
 * and identifier quoting style.
 */
public interface ColumnNameConverter {
    
    /**
     * Convert a column name from source to target format.
     * Input is automatically cleaned of any quotes (`, ", []).
     * 
     * @param columnName the source column name (may contain quotes, will be cleaned)
     * @param tableAlias the table alias or name for context (may help resolve which table)
     * @param ctx the clause context with additional information
     * @return the converted column name for target database (without quotes)
     */
    String convert(String columnName, String tableAlias, ClauseContext ctx);
    
    /**
     * Convert a column name and apply quoting for the target database.
     * This combines convert() with database-specific quoting.
     * Input is automatically cleaned of any quotes first.
     * 
     * @param columnName the source column name (may contain quotes, will be cleaned)
     * @param tableAlias the table alias or name for context
     * @param ctx the clause context with additional information
     * @return the converted and quoted column name (e.g., "col_name" for PostgreSQL)
     */
    String convertAndQuote(String columnName, String tableAlias, ClauseContext ctx);
    
    /**
     * Clean an identifier by removing common quote characters.
     * Handles MySQL backticks, PostgreSQL double quotes, and SQL Server brackets.
     * 
     * @param name the identifier to clean
     * @return the cleaned identifier without quotes
     */
    static String cleanIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        // Remove MySQL backticks
        name = name.replace("`", "");
        // Remove PostgreSQL double quotes
        name = name.replace("\"", "");
        // Remove SQL Server brackets [name] -> name
        if (name.startsWith("[") && name.endsWith("]")) {
            name = name.substring(1, name.length() - 1);
        }
        return name;
    }
}
