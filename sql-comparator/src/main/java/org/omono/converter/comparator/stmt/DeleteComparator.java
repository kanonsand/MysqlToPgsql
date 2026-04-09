package org.omono.converter.comparator.stmt;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import org.omono.converter.comparator.context.CompareContext;
import org.omono.converter.comparator.expr.ExpressionComparator;

/**
 * Comparator for SQL DELETE statements.
 */
public class DeleteComparator extends AbstractStatementComparator<SQLDeleteStatement> {
    
    private final ExpressionComparator exprComparator;
    
    public DeleteComparator(CompareContext ctx, String path) {
        super(ctx, path);
        this.exprComparator = new ExpressionComparator(ctx);
    }
    
    @Override
    public DeleteComparator withContext(CompareContext ctx, String path) {
        return new DeleteComparator(ctx, path);
    }
    
    @Override
    public boolean supports(SQLStatement stmt) {
        return stmt instanceof SQLDeleteStatement;
    }
    
    @Override
    protected void compare(SQLDeleteStatement a, SQLDeleteStatement b) {
        // Table name
        compareTableName(a.getTableName(), b.getTableName(), ".table");
        // WHERE clause
        compareExpr(a.getWhere(), b.getWhere(), ".where");
    }
    
    @Override
    protected void compareExpr(SQLExpr a, SQLExpr b, String subPath) {
        exprComparator.compare(a, b, fullPath(subPath));
    }
}
