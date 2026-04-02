package converter;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for INSERT statements with batch processing support
 */
public class InsertHandler extends BaseHandler<MySqlInsertStatement> {

    @Override
    protected Class<MySqlInsertStatement> getStatementClass() {
        return MySqlInsertStatement.class;
    }

    @Override
    public String getStatementTypeName() {
        return "insert";
    }

    @Override
    protected String doConvert(MySqlInsertStatement statement) {
        // Build parameterized INSERT statement
        List<SQLExpr> columns = statement.getColumns();
        int valueNum = statement.getValuesList().stream()
            .findFirst()
            .map(valuesClause -> valuesClause.getValues().size())
            .orElse(0);

        int realValueNum = columns.isEmpty() ? valueNum : columns.size();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ");
        SQLName tableName = statement.getTableName();
        sb.append(convertTableName(tableName.getSimpleName()));

        if (!columns.isEmpty()) {
            sb.append(" (")
              .append(columns.stream()
                  .map(SQLIdentifierExpr.class::cast)
                  .map(SQLIdentifierExpr::getName)
                  .map(this::convertColumnName)
                  .collect(Collectors.joining(",")))
              .append(") ");
        }

        sb.append(" VALUES (");
        for (int i = 0; i < realValueNum - 1; i++) {
            sb.append("?,");
        }
        sb.append("?)");

        return sb.toString();
    }

    @Override
    public List<String> convert(MySqlInsertStatement statement) {
        return Collections.singletonList(doConvert(statement));
    }
}
