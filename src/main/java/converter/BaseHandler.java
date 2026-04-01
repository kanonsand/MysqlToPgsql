package converter;

import com.alibaba.druid.sql.ast.SQLStatement;
import org.apache.commons.dbutils.QueryRunner;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Base class for statement handlers providing common utility methods
 * @param <T> the type of SQL statement this handler processes
 */
public abstract class BaseHandler<T extends SQLStatement> implements StatementHandler<T> {

    @Override
    public boolean canHandle(SQLStatement statement) {
        return getStatementClass().isInstance(statement);
    }

    @Override
    public void parseStatement(List<T> statements, SQLStatement statement) {
        if (canHandle(statement)) {
            statements.add(getStatementClass().cast(statement));
        }
    }

    @Override
    public List<String> convert(T statement) {
        return Collections.singletonList(doConvert(statement));
    }

    @Override
    public int execute(QueryRunner queryRunner, T statement) throws SQLException {
        List<String> convertedSqls = convert(statement);
        int totalAffected = 0;
        for (String sql : convertedSqls) {
            logStatement(sql);
            totalAffected += doExecute(queryRunner, statement, sql);
        }
        return totalAffected;
    }

    /**
     * Get the class of the statement this handler processes
     * @return the statement class
     */
    protected abstract Class<T> getStatementClass();

    /**
     * Convert the statement to PostgreSQL SQL string
     * @param statement the statement to convert
     * @return the converted SQL string
     */
    protected abstract String doConvert(T statement);

    /**
     * Execute the statement against the database
     * @param queryRunner the database query runner
     * @param statement the original statement
     * @param sql the converted SQL
     * @return number of rows affected
     */
    protected int doExecute(QueryRunner queryRunner, T statement, String sql) throws SQLException {
        return queryRunner.execute(sql);
    }

    /**
     * Log the statement being executed
     * @param sql the SQL to log
     */
    protected void logStatement(String sql) {
        System.out.println(getStatementTypeName() + " statement: " + sql);
    }

    /**
     * Convert MySQL backticks to PostgreSQL double quotes (legacy method)
     * @param identifier the identifier to convert
     * @return the converted identifier
     * @deprecated Use {@link #convertTableName(String)} or {@link #convertColumnName(String)} instead
     */
    @Deprecated
    protected String convertIdentifier(String identifier) {
        return identifier.replace("`", "\"");
    }

    /**
     * Convert table name using TableNameHandler
     * Always wraps with double quotes to handle reserved words
     * @param tableName the table name to convert
     * @return the converted table name
     */
    protected String convertTableName(String tableName) {
        return TableNameHandler.convert(tableName);
    }

    /**
     * Convert column name using ColumnNameHandler
     * Always wraps with double quotes to handle reserved words
     * @param columnName the column name to convert
     * @return the converted column name
     */
    protected String convertColumnName(String columnName) {
        return ColumnNameHandler.convert(columnName);
    }
}