package org.omono.converter.comparator.stmt;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import org.omono.converter.comparator.context.CompareContext;
import org.omono.converter.comparator.expr.ExpressionComparator;

import java.util.List;

/**
 * Comparator for SQL UPDATE statements.
 */
public class UpdateComparator extends AbstractStatementComparator<SQLUpdateStatement> {
    
    private final ExpressionComparator exprComparator;
    
    public UpdateComparator(CompareContext ctx, String path) {
        super(ctx, path);
        this.exprComparator = new ExpressionComparator(ctx);
    }
    
    @Override
    public UpdateComparator withContext(CompareContext ctx, String path) {
        return new UpdateComparator(ctx, path);
    }
    
    @Override
    public boolean supports(SQLStatement stmt) {
        return stmt instanceof SQLUpdateStatement;
    }
    
    @Override
    protected void compare(SQLUpdateStatement a, SQLUpdateStatement b) {
        // Table name
        compareTableName(a.getTableName(), b.getTableName(), ".table");
        // SET clause
        compareUpdateItems(a.getItems(), b.getItems(), ".set");
        // WHERE clause
        compareExpr(a.getWhere(), b.getWhere(), ".where");
    }
    
    private void compareUpdateItems(List<SQLUpdateSetItem> a, List<SQLUpdateSetItem> b, String subPath) {
        if (a.size() != b.size()) {
            throw new AssertionError(fullPath(subPath) + ": SET item count mismatch: " + a.size() + " vs " + b.size());
        }
        for (int i = 0; i < a.size(); i++) {
            compareExpr(a.get(i).getColumn(), b.get(i).getColumn(), subPath + "[" + i + "].column");
            compareExpr(a.get(i).getValue(), b.get(i).getValue(), subPath + "[" + i + "].value");
        }
    }
    
    @Override
    protected void compareExpr(SQLExpr a, SQLExpr b, String subPath) {
        exprComparator.compare(a, b, fullPath(subPath));
    }
}
