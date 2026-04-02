package converter;

import com.alibaba.druid.sql.ast.SQLStatement;

import java.util.List;

/**
 * SQL statement handler interface for converting MySQL statements to PostgreSQL
 * @param <T> the type of SQL statement this handler processes
 */
public interface StatementHandler<T extends SQLStatement> {

    /**
     * Check if this handler can handle the given statement
     * @param statement the SQL statement to check
     * @return true if this handler can process the statement
     */
    boolean canHandle(SQLStatement statement);

    /**
     * Parse and categorize statements from the raw SQL
     * @param statements the list to add parsed statements to
     * @param statement the statement to parse
     */
    void parseStatement(List<T> statements, SQLStatement statement);

    /**
     * Convert the MySQL statement to PostgreSQL compatible format
     * @param statement the MySQL statement to convert
     * @return the converted PostgreSQL SQL string(s)
     */
    List<String> convert(T statement);

    /**
     * Get the statement type name for logging
     * @return the statement type name
     */
    String getStatementTypeName();
}