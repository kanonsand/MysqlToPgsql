package org.omono.converter.oldhandler;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.visitor.SQLEvalVisitor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class for executing converted SQL statements
 */
public class SqlExecutor {
    
    private static final int BATCH_SIZE = 500;
    
    private final QueryRunner queryRunner;
    private int totalInsertCount = 0;
    
    public SqlExecutor(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }
    
    /**
     * Execute a single SQL statement
     * @param sql the SQL to execute
     * @return number of rows affected
     */
    public int execute(String sql) throws SQLException {
        return queryRunner.execute(sql);
    }
    
    /**
     * Execute multiple SQL statements
     * @param sqls list of SQL statements
     * @return total rows affected
     */
    public int executeBatch(List<String> sqls) throws SQLException {
        int total = 0;
        for (String sql : sqls) {
            total += execute(sql);
        }
        return total;
    }
    
    /**
     * Execute batch insert with parameters
     * @param sql the parameterized SQL
     * @param params array of parameter arrays
     * @return total rows inserted
     */
    public int executeBatchInsert(String sql, Object[][] params) throws SQLException {
        int total = 0;
        for (Object[] param : params) {
            queryRunner.insert(sql, new ScalarHandler<>(), param);
            total++;
            totalInsertCount++;
            if (totalInsertCount % 100 == 0) {
                System.out.println("insert progress: " + totalInsertCount);
            }
        }
        return total;
    }
    
    /**
     * Execute batch insert from MySQL INSERT statement
     * @param statement the MySQL INSERT statement
     * @return total rows inserted
     */
    public int executeInsert(MySqlInsertStatement statement) throws SQLException {
        List<SQLExpr> columns = statement.getColumns();
        int valueNum = statement.getValuesList().stream()
            .findFirst()
            .map(valuesClause -> valuesClause.getValues().size())
            .orElse(0);
        
        if (!columns.isEmpty() && columns.size() != valueNum) {
            System.out.println("insert sql param and field not match");
            return 0;
        }
        
        String insertSql = new InsertHandler().convert(statement).get(0);
        
        List<Object[]> valueArrays = extractInsertValues(statement);
        
        List<List<Object[]>> partitions = ListUtils.partition(valueArrays, BATCH_SIZE);
        int affectedRows = 0;
        
        for (List<Object[]> partition : partitions) {
            Object[][] params = partition.toArray(new Object[0][]);
            affectedRows += executeBatchInsert(insertSql, params);
        }
        
        return affectedRows;
    }
    
    /**
     * Extract values from INSERT statement
     * @param statement the INSERT statement
     * @return list of value arrays
     */
    public List<Object[]> extractInsertValues(MySqlInsertStatement statement) {
        List<SQLInsertStatement.ValuesClause> valuesList = statement.getValuesList();
        return valuesList.stream()
            .map(valuesClause -> valuesClause.getValues().stream()
                .map(SQLValuableExpr.class::cast)
                .map(SQLValuableExpr::getValue)
                .map(value -> {
                    if (value == SQLEvalVisitor.EVAL_VALUE_NULL) {
                        return null;
                    }
                    return value;
                })
                .toArray())
            .collect(Collectors.toList());
    }
    
    /**
     * Get total inserted count
     * @return total count
     */
    public int getTotalInsertCount() {
        return totalInsertCount;
    }
    
    /**
     * Reset the counter
     */
    public void resetCount() {
        totalInsertCount = 0;
    }
    
    /**
     * Log SQL statement
     * @param statementType the statement type (e.g., "insert", "update")
     * @param sql the SQL to log
     */
    public void log(String statementType, String sql) {
        System.out.println(statementType + " statement: " + sql);
    }
}
