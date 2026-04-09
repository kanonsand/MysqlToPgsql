package org.omono.converter.comparator.stmt;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.*;
import org.omono.converter.comparator.context.CompareContext;
import org.omono.converter.comparator.expr.ExpressionComparator;

import java.util.List;

/**
 * Comparator for SQL SELECT statements.
 */
public class SelectComparator extends AbstractStatementComparator<SQLSelectStatement> {
    
    private final ExpressionComparator exprComparator;
    
    public SelectComparator(CompareContext ctx, String path) {
        super(ctx, path);
        this.exprComparator = new ExpressionComparator(ctx);
    }
    
    @Override
    public SelectComparator withContext(CompareContext ctx, String path) {
        return new SelectComparator(ctx, path);
    }
    
    @Override
    public boolean supports(SQLStatement stmt) {
        return stmt instanceof SQLSelectStatement;
    }
    
    @Override
    protected void compare(SQLSelectStatement a, SQLSelectStatement b) {
        compareSelect(a.getSelect(), b.getSelect(), "");
    }
    
    // ========== SELECT Comparison ==========
    
    private void compareSelect(SQLSelect a, SQLSelect b, String subPath) {
        if (a == null && b == null) return;
        if (a == null || b == null) {
            throw new AssertionError(fullPath(subPath) + ": One SELECT is null");
        }
        compareSelectQuery(a.getQuery(), b.getQuery(), subPath + ".query");
    }
    
    private void compareSelectQuery(SQLSelectQuery a, SQLSelectQuery b, String subPath) {
        if (a instanceof SQLSelectQueryBlock && b instanceof SQLSelectQueryBlock) {
            compareSelectQueryBlock((SQLSelectQueryBlock) a, (SQLSelectQueryBlock) b, subPath);
        } else if (a instanceof SQLUnionQuery && b instanceof SQLUnionQuery) {
            compareUnionQuery((SQLUnionQuery) a, (SQLUnionQuery) b, subPath);
        } else {
            throw new AssertionError(fullPath(subPath) + ": Query type mismatch");
        }
    }
    
    private void compareSelectQueryBlock(SQLSelectQueryBlock a, SQLSelectQueryBlock b, String subPath) {
        // SELECT clause
        compareSelectList(a.getSelectList(), b.getSelectList(), subPath + ".select");
        // FROM clause
        compareFrom(a.getFrom(), b.getFrom(), subPath + ".from");
        // WHERE clause
        compareExpr(a.getWhere(), b.getWhere(), subPath + ".where");
        // GROUP BY
        compareGroupBy(a.getGroupBy(), b.getGroupBy(), subPath + ".groupBy");
        // ORDER BY
        compareOrderBy(a.getOrderBy(), b.getOrderBy(), subPath + ".orderBy");
        // LIMIT
        compareLimit(a.getLimit(), b.getLimit(), subPath + ".limit");
    }
    
    private void compareUnionQuery(SQLUnionQuery a, SQLUnionQuery b, String subPath) {
        if (a.getOperator() != b.getOperator()) {
            throw new AssertionError(fullPath(subPath) + ": Union operator mismatch");
        }
        compareSelectQuery(a.getLeft(), b.getLeft(), subPath + ".left");
        compareSelectQuery(a.getRight(), b.getRight(), subPath + ".right");
    }
    
    private void compareSelectList(List<SQLSelectItem> a, List<SQLSelectItem> b, String subPath) {
        if (a == null && b == null) return;
        if (a == null || b == null) {
            throw new AssertionError(fullPath(subPath) + ": One select list is null");
        }
        if (a.size() != b.size()) {
            throw new AssertionError(fullPath(subPath) + ": Select item count mismatch: " + a.size() + " vs " + b.size());
        }
        for (int i = 0; i < a.size(); i++) {
            compareSelectItem(a.get(i), b.get(i), subPath + "[" + i + "]");
        }
    }
    
    private void compareSelectItem(SQLSelectItem a, SQLSelectItem b, String subPath) {
        // 1. 比较原始表达式（验证列名正确性，通过 columnMapping）
        compareExpr(a.getExpr(), b.getExpr(), subPath);
        
        // 2. 比较 alias（验证结果列名）
        // 特殊处理：当列名被映射时，转换器可能会添加原始列名作为别名
        // 例如：MySQL `user_name` -> PG `"name" AS "user_name"`
        String aliasA = a.getAlias();
        String aliasB = b.getAlias();
        
        if (aliasA == null && aliasB != null) {
            // Source has no alias, target has alias
            // Check if target alias equals source column name (mapping pattern)
            if (a.getExpr() instanceof SQLIdentifierExpr) {
                String sourceCol = ((SQLIdentifierExpr) a.getExpr()).getName();
                String targetAlias = ctx.stripTargetQuotes(aliasB);
                if (!sourceCol.equalsIgnoreCase(targetAlias)) {
                    throw new AssertionError(fullPath(subPath + ".alias") + 
                        ": Alias mismatch - '" + aliasA + "' vs '" + aliasB + "'");
                }
                // Target alias equals source column name - valid mapping pattern
            } else if (a.getExpr() instanceof SQLPropertyExpr) {
                String sourceCol = ((SQLPropertyExpr) a.getExpr()).getName();
                String targetAlias = ctx.stripTargetQuotes(aliasB);
                if (!sourceCol.equalsIgnoreCase(targetAlias)) {
                    throw new AssertionError(fullPath(subPath + ".alias") + 
                        ": Alias mismatch - '" + aliasA + "' vs '" + aliasB + "'");
                }
                // Target alias equals source column name - valid mapping pattern
            } else {
                throw new AssertionError(fullPath(subPath + ".alias") + 
                    ": Alias mismatch - '" + aliasA + "' vs '" + aliasB + "'");
            }
        } else {
            compareString(aliasA, aliasB, subPath + ".alias");
        }
    }
    
    private void compareFrom(SQLTableSource a, SQLTableSource b, String subPath) {
        if (a == null && b == null) return;
        if (a == null || b == null) {
            throw new AssertionError(fullPath(subPath) + ": One FROM is null");
        }
        
        if (a instanceof SQLExprTableSource && b instanceof SQLExprTableSource) {
            compareExpr(((SQLExprTableSource) a).getExpr(),
                ((SQLExprTableSource) b).getExpr(), subPath);
        } else if (a instanceof SQLJoinTableSource && b instanceof SQLJoinTableSource) {
            compareJoin((SQLJoinTableSource) a, (SQLJoinTableSource) b, subPath);
        } else if (a instanceof SQLSubqueryTableSource && b instanceof SQLSubqueryTableSource) {
            compareSelect(((SQLSubqueryTableSource) a).getSelect(),
                ((SQLSubqueryTableSource) b).getSelect(), subPath);
        } else {
            throw new AssertionError(fullPath(subPath) + ": FROM type mismatch - " + a.getClass() + " vs " + b.getClass());
        }
        
        compareString(a.getAlias(), b.getAlias(), subPath + ".alias");
    }
    
    private void compareJoin(SQLJoinTableSource a, SQLJoinTableSource b, String subPath) {
        if (a.getJoinType() != b.getJoinType()) {
            throw new AssertionError(fullPath(subPath) + ": Join type mismatch: " + a.getJoinType() + " vs " + b.getJoinType());
        }
        compareFrom(a.getLeft(), b.getLeft(), subPath + ".left");
        compareFrom(a.getRight(), b.getRight(), subPath + ".right");
        compareExpr(a.getCondition(), b.getCondition(), subPath + ".condition");
    }
    
    private void compareGroupBy(SQLSelectGroupByClause a, SQLSelectGroupByClause b, String subPath) {
        if (a == null && b == null) return;
        if (a == null || b == null) {
            throw new AssertionError(fullPath(subPath) + ": One GROUP BY is null");
        }
        exprComparator.compareExprList(a.getItems(), b.getItems(), fullPath(subPath + ".items"));
    }
    
    private void compareOrderBy(SQLOrderBy a, SQLOrderBy b, String subPath) {
        if (a == null && b == null) return;
        if (a == null || b == null) {
            throw new AssertionError(fullPath(subPath) + ": One ORDER BY is null");
        }
        if (a.getItems().size() != b.getItems().size()) {
            throw new AssertionError(fullPath(subPath) + ": ORDER BY item count mismatch");
        }
        for (int i = 0; i < a.getItems().size(); i++) {
            compareExpr(a.getItems().get(i).getExpr(),
                b.getItems().get(i).getExpr(), subPath + "[" + i + "]");
        }
    }
    
    private void compareLimit(SQLLimit a, SQLLimit b, String subPath) {
        if (a == null && b == null) return;
        if (a == null || b == null) {
            throw new AssertionError(fullPath(subPath) + ": One LIMIT is null");
        }
        compareExpr(a.getRowCount(), b.getRowCount(), subPath);
    }
    
    @Override
    protected void compareExpr(SQLExpr a, SQLExpr b, String subPath) {
        exprComparator.compare(a, b, fullPath(subPath));
    }
}
