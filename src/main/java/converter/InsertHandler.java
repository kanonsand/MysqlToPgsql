package converter;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.visitor.SQLEvalVisitor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for INSERT statements with batch processing support
 */
public class InsertHandler extends BaseHandler<MySqlInsertStatement> {

    private static final int BATCH_SIZE = 500;
    private int totalCount = 0;

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

    @Override
    public int execute(QueryRunner queryRunner, MySqlInsertStatement statement) throws SQLException {
        // Validate column count matches value count
        List<SQLExpr> columns = statement.getColumns();
        int valueNum = statement.getValuesList().stream()
            .findFirst()
            .map(valuesClause -> valuesClause.getValues().size())
            .orElse(0);

        if (!columns.isEmpty() && columns.size() != valueNum) {
            System.out.println("insert sql param and field not match");
            return 0;
        }

        String insertSql = doConvert(statement);

        // Extract values from the statement
        List<SQLInsertStatement.ValuesClause> valuesList = statement.getValuesList();
        List<Object[]> valueArrays = valuesList.stream()
            .map(valuesClause -> valuesClause.getValues().stream()
                .map(SQLValuableExpr.class::cast)
                .map(SQLValuableExpr::getValue)
                .toArray())
            .collect(Collectors.toList());

        // Handle NULL values
        valueArrays.forEach(objects -> {
            for (int i = 0; i < objects.length; i++) {
                if (objects[i] == SQLEvalVisitor.EVAL_VALUE_NULL) {
                    objects[i] = null;
                }
            }
        });

        // Batch insert
        List<List<Object[]>> partitions = ListUtils.partition(valueArrays, BATCH_SIZE);
        int affectedRows = 0;

        for (List<Object[]> partition : partitions) {
            Object[][] params = partition.toArray(new Object[0][]);
            queryRunner.insertBatch(insertSql, new ScalarHandler<>(), params);
            affectedRows += params.length;
            totalCount += params.length;
            System.out.println("insert progress: " + totalCount);
        }

        return affectedRows;
    }

    /**
     * Get total count of inserted rows
     * @return total inserted rows
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * Reset the total count counter
     */
    public void resetCount() {
        totalCount = 0;
    }
}
