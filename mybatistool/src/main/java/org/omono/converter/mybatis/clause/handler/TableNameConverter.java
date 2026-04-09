package org.omono.converter.mybatis.clause.handler;

import org.omono.converter.mybatis.clause.ClauseContext;

/**
 * Converts table names from source database to target database format.
 * Implementations can handle different database dialects including name conversion
 * and identifier quoting style.
 */
public interface TableNameConverter {
    
    /**
     * Convert a table name from source to target format.
     * Input is automatically cleaned of any quotes (`, ", []).
     * 
     * @param tableName the source table name (may contain quotes, will be cleaned)
     * @param alias the table alias (may be null if no alias)
     * @param ctx the clause context with additional information
     * @return the converted table name for target database (without quotes)
     */
    String convert(String tableName, String alias, ClauseContext ctx);
    
    /**
     * Convert a table name and apply quoting for the target database.
     * This combines convert() with database-specific quoting.
     * Input is automatically cleaned of any quotes first.
     * 
     * @param tableName the source table name (may contain quotes, will be cleaned)
     * @param alias the table alias (may be null if no alias)
     * @param ctx the clause context with additional information
     * @return the converted and quoted table name (e.g., "table_name" for PostgreSQL)
     */
    String convertAndQuote(String tableName, String alias, ClauseContext ctx);
    
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
