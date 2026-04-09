package org.omono.converter.comparator.stmt;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import org.omono.converter.comparator.context.CompareContext;
import org.omono.converter.comparator.expr.ExpressionComparator;

import java.util.List;

/**
 * Comparator for SQL INSERT statements.
 */
public class InsertComparator extends AbstractStatementComparator<SQLInsertStatement> {
    
    private final ExpressionComparator exprComparator;
    
    public InsertComparator(CompareContext ctx, String path) {
        super(ctx, path);
        this.exprComparator = new ExpressionComparator(ctx);
    }
    
    @Override
    public InsertComparator withContext(CompareContext ctx, String path) {
        return new InsertComparator(ctx, path);
    }
    
    @Override
    public boolean supports(SQLStatement stmt) {
        return stmt instanceof SQLInsertStatement;
    }
    
    @Override
    protected void compare(SQLInsertStatement a, SQLInsertStatement b) {
        // Table name
        compareTableName(a.getTableName(), b.getTableName(), ".table");
        // Columns
        compareExprList(a.getColumns(), b.getColumns(), ".columns");
        // Values
        compareValuesList(a.getValuesList(), b.getValuesList(), ".values");
    }
    
    private void compareValuesList(List<SQLInsertStatement.ValuesClause> a,
                                   List<SQLInsertStatement.ValuesClause> b,
                                   String subPath) {
        if (a.size() != b.size()) {
            throw new AssertionError(fullPath(subPath) + ": Values count mismatch: " + a.size() + " vs " + b.size());
        }
        for (int i = 0; i < a.size(); i++) {
            compareExprList(a.get(i).getValues(), b.get(i).getValues(), subPath + "[" + i + "]");
        }
    }
    
    @Override
    protected void compareExpr(SQLExpr a, SQLExpr b, String subPath) {
        exprComparator.compare(a, b, fullPath(subPath));
    }
}
