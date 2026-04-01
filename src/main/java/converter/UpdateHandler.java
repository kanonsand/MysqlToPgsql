package converter;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.visitor.SQLEvalVisitor;
import org.apache.commons.dbutils.QueryRunner;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for UPDATE statements
 */
public class UpdateHandler extends BaseHandler<MySqlUpdateStatement> {

    @Override
    protected Class<MySqlUpdateStatement> getStatementClass() {
        return MySqlUpdateStatement.class;
    }

    @Override
    public String getStatementTypeName() {
        return "update";
    }

    @Override
    protected String doConvert(MySqlUpdateStatement statement) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ");

        // Convert table name
        if (statement.getTableSource() != null) {
            sb.append(convertTableName(statement.getTableSource().toString()));
        }

        // Convert SET clause
        sb.append(" SET ");
        List<SQLUpdateSetItem> items = statement.getItems();
        String setClause = items.stream()
            .map(item -> {
                String column = convertColumnName(item.getColumn().toString());
                String value = "?"; // Use parameterized query
                return column + " = " + value;
            })
            .collect(Collectors.joining(", "));
        sb.append(setClause);

        // Convert WHERE clause
        if (statement.getWhere() != null) {
            sb.append(" WHERE ").append(convertWhereClause(statement.getWhere()));
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

    /**
     * Extract values from the UPDATE statement for parameterized execution
     */
    private List<Object> extractValues(MySqlUpdateStatement statement) {
        List<Object> values = new ArrayList<>();
        for (SQLUpdateSetItem item : statement.getItems()) {
            if (item.getValue() instanceof SQLValuableExpr) {
                Object value = ((SQLValuableExpr) item.getValue()).getValue();
                if (value == SQLEvalVisitor.EVAL_VALUE_NULL) {
                    values.add(null);
                } else {
                    values.add(value);
                }
            }
        }
        return values;
    }

    @Override
    public List<String> convert(MySqlUpdateStatement statement) {
        return Collections.singletonList(doConvert(statement));
    }

    @Override
    public int execute(QueryRunner queryRunner, MySqlUpdateStatement statement) throws SQLException {
        String sql = doConvert(statement);
        List<Object> values = extractValues(statement);

        logStatement(sql);

        if (!values.isEmpty()) {
            return queryRunner.execute(sql, values.toArray());
        }
        return queryRunner.execute(sql);
    }
}
