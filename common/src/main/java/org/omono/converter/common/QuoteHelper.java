package org.omono.converter.common;

import com.alibaba.druid.DbType;

/**
 * Utility class for database-specific identifier quoting.
 * Shared between sql-comparator and mybatistool modules.
 * 
 * Supports quoting for multiple database types:
 * - MySQL/MariaDB: `identifier` (backticks)
 * - PostgreSQL: "identifier" (double quotes)
 * - SQL Server: [identifier] (brackets)
 * - Oracle: "identifier" (double quotes)
 * - SQLite/H2: "identifier" (double quotes, also support backticks)
 */
public class QuoteHelper {
    
    /**
     * Quote an identifier for the specified database type.
     * Uses DbType to determine the appropriate quote style.
     * 
     * @param name the identifier to quote (should be clean, without quotes)
     * @param dbType the target database type
     * @return the quoted identifier
     */
    public static String quote(String name, DbType dbType) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        if (dbType == null) {
            return quotePostgres(name);
        }
        
        switch (dbType) {
            case mysql:
            case mariadb:
                return quoteMysql(name);
            case postgresql:
                return quotePostgres(name);
            case sqlserver:
                return quoteSqlServer(name);
            case oracle:
                return quoteOracle(name);
            case sqlite:
            case h2:
                return quotePostgres(name);  // Both support double quotes
            default:
                return quotePostgres(name);  // Safe default
        }
    }
    
    /**
     * Quote using a specific quote pair.
     * 
     * @param name the identifier to quote
     * @param quotePair the quote pair to use
     * @return the quoted identifier
     */
    public static String quote(String name, IdentifierQuotePair quotePair) {
        if (name == null || name.isEmpty() || quotePair == null) {
            return name;
        }
        
        char start = quotePair.getStart();
        char end = quotePair.getEnd();
        
        if (start == end) {
            // Same character for start and end: escape by doubling
            String escaped = name.replace(String.valueOf(start), String.valueOf(start) + start);
            return start + escaped + end;
        } else {
            // Different characters: no escaping needed for SQL Server brackets
            return start + name + end;
        }
    }
    
    /**
     * Clean an identifier by removing quotes from any database type.
     * Handles MySQL backticks, PostgreSQL/Oracle double quotes, SQL Server brackets.
     * 
     * @param name the identifier to clean
     * @return the cleaned identifier without quotes
     */
    public static String clean(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // Remove MySQL backticks
        name = name.replace("`", "");
        // Remove PostgreSQL/Oracle double quotes
        name = name.replace("\"", "");
        // Remove SQL Server brackets [name] -> name
        if (name.startsWith("[") && name.endsWith("]")) {
            name = name.substring(1, name.length() - 1);
        }
        
        return name;
    }
    
    /**
     * Get default quote pair for a database type.
     * 
     * @param dbType the database type
     * @return the default quote pair for this database type
     */
    public static IdentifierQuotePair getQuotePair(DbType dbType) {
        if (dbType == null) {
            return IdentifierQuotePair.of('"');  // Default to PostgreSQL style
        }
        
        switch (dbType) {
            case mysql:
            case mariadb:
                return IdentifierQuotePair.of('`');
            case postgresql:
            case oracle:
            case sqlite:
            case h2:
                return IdentifierQuotePair.of('"');
            case sqlserver:
                return IdentifierQuotePair.of('[', ']');
            default:
                return IdentifierQuotePair.of('"');
        }
    }
    
    // ====== Private helper methods ======
    
    private static String quoteMysql(String name) {
        // MySQL: `name` -> `` `na``me` `` (backticks, double to escape)
        String escaped = name.replace("`", "``");
        return "`" + escaped + "`";
    }
    
    private static String quotePostgres(String name) {
        // PostgreSQL: "name" -> "na""me" (double quotes, double to escape)
        String escaped = name.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
    
    private static String quoteSqlServer(String name) {
        // SQL Server: [name] (brackets, no escaping needed for internal brackets)
        return "[" + name + "]";
    }
    
    private static String quoteOracle(String name) {
        // Oracle: "name" -> "na""me" (same as PostgreSQL)
        String escaped = name.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}