package converter;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import org.apache.commons.dbutils.QueryRunner;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Handler for DELETE statements
 */
public class DeleteHandler extends BaseHandler<MySqlDeleteStatement> {

    @Override
    protected Class<MySqlDeleteStatement> getStatementClass() {
        return MySqlDeleteStatement.class;
    }

    @Override
    public String getStatementTypeName() {
        return "delete";
    }

    @Override
    protected String doConvert(MySqlDeleteStatement statement) {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ");

        // Convert table name
        if (statement.getTableSource() != null) {
            sb.append(convertTableName(statement.getTableSource().toString()));
        }

        // Convert WHERE clause
        if (statement.getWhere() != null) {
            sb.append(" WHERE ").append(convertWhereClause(statement.getWhere()));
        }

        // Handle ORDER BY and LIMIT (MySQL specific, may need adjustment for PostgreSQL)
        if (statement.getOrderBy() != null) {
            sb.append(" ORDER BY ").append(statement.getOrderBy().toString());
        }

        if (statement.getLimit() != null) {
            // PostgreSQL uses different syntax for limiting deletes
            // For now, log a warning
            System.out.println("Warning: DELETE with LIMIT may need manual adjustment for PostgreSQL");
        }

        return sb.toString();
    }

    /**
     * Convert WHERE clause, handling identifiers with proper quoting
     */
    private String convertWhereClause(SQLExpr where) {
        String whereStr = where.toString();
        // Replace backticks with double quotes for identifiers in WHERE clause
        return whereStr.replace("`", "\"");
    }

    @Override
    public List<String> convert(MySqlDeleteStatement statement) {
        return Collections.singletonList(doConvert(statement));
    }

    @Override
    public int execute(QueryRunner queryRunner, MySqlDeleteStatement statement) throws SQLException {
        String sql = doConvert(statement);
        logStatement(sql);
        return queryRunner.execute(sql);
    }
}
