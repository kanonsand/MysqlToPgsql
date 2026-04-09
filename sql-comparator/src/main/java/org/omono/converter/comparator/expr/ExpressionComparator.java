package org.omono.converter.comparator.expr;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import org.omono.converter.comparator.context.CompareContext;

import java.util.List;

/**
 * Comparator for SQL expressions.
 * Delegates literal comparison to LiteralComparator.
 */
public class ExpressionComparator {
    
    private final CompareContext ctx;
    private final LiteralComparator literalComparator;
    
    public ExpressionComparator(CompareContext ctx) {
        this.ctx = ctx;
        this.literalComparator = new LiteralComparator(ctx);
    }
    
    /**
     * Compare two SQL expressions.
     */
    public void compare(SQLExpr a, SQLExpr b, String path) {
        if (a == null && b == null) return;
        if (a == null || b == null) {
            throw new AssertionError(path + ": One expr is null - " + a + " vs " + b);
        }
        
        // Identifier
        if (a instanceof SQLIdentifierExpr && b instanceof SQLIdentifierExpr) {
            compareIdentifier((SQLIdentifierExpr) a, (SQLIdentifierExpr) b, path);
        }
        // Property expression (table.column)
        else if (a instanceof SQLPropertyExpr && b instanceof SQLPropertyExpr) {
            comparePropertyExpr((SQLPropertyExpr) a, (SQLPropertyExpr) b, path);
        }
        // Property vs Identifier (one has alias, one doesn't)
        else if (a instanceof SQLPropertyExpr && b instanceof SQLIdentifierExpr) {
            comparePropertyVsIdentifier((SQLPropertyExpr) a, (SQLIdentifierExpr) b, path);
        }
        else if (a instanceof SQLIdentifierExpr && b instanceof SQLPropertyExpr) {
            comparePropertyVsIdentifier((SQLPropertyExpr) b, (SQLIdentifierExpr) a, path);
        }
        // Literal
        else if (a instanceof SQLLiteralExpr && b instanceof SQLLiteralExpr) {
            literalComparator.compare((SQLLiteralExpr) a, (SQLLiteralExpr) b, path);
        }
        // Binary operation
        else if (a instanceof SQLBinaryOpExpr && b instanceof SQLBinaryOpExpr) {
            compareBinaryOp((SQLBinaryOpExpr) a, (SQLBinaryOpExpr) b, path);
        }
        // IN list
        else if (a instanceof SQLInListExpr && b instanceof SQLInListExpr) {
            compareInList((SQLInListExpr) a, (SQLInListExpr) b, path);
        }
        // IN subquery
        else if (a instanceof SQLInSubQueryExpr && b instanceof SQLInSubQueryExpr) {
            compareInSubQuery((SQLInSubQueryExpr) a, (SQLInSubQueryExpr) b, path);
        }
        // BETWEEN
        else if (a instanceof SQLBetweenExpr && b instanceof SQLBetweenExpr) {
            compareBetween((SQLBetweenExpr) a, (SQLBetweenExpr) b, path);
        }
        // All columns (*)
        else if (a instanceof SQLAllColumnExpr && b instanceof SQLAllColumnExpr) {
            // * matches *
        }
        // Type mismatch
        else if (a.getClass() != b.getClass()) {
            throw new AssertionError(path + ": Expression type mismatch - "
                + a.getClass().getSimpleName() + " vs " + b.getClass().getSimpleName());
        }
        // Same unknown type - compare as string
        else {
            if (!a.toString().equalsIgnoreCase(b.toString())) {
                throw new AssertionError(path + ": Expression mismatch - '" + a + "' vs '" + b + "'");
            }
        }
    }
    
    // ========== Identifier Comparison ==========
    
    private void compareIdentifier(SQLIdentifierExpr a, SQLIdentifierExpr b, String path) {
        String nameA = a.getName();
        String nameB = b.getName();
        
        // Try column name match
        if (!ctx.isEquivalentIdentifier(nameA, nameB, true)) {
            // Try table name match
            if (!ctx.isEquivalentIdentifier(nameA, nameB, false)) {
                throw new AssertionError(path + ": Identifier mismatch - '" + nameA + "' vs '" + nameB + "'");
            }
        }
    }
    
    // ========== Property Expression Comparison ==========
    
    private void comparePropertyExpr(SQLPropertyExpr a, SQLPropertyExpr b, String path) {
        // Compare column name
        String colA = a.getName();
        String colB = b.getName();
        
        if (!ctx.isEquivalentIdentifier(colA, colB, true)) {
            throw new AssertionError(path + ": Column mismatch - '" + colA + "' vs '" + colB + "'");
        }
        
        // Compare table alias/name
        if (a.getOwner() instanceof SQLIdentifierExpr && b.getOwner() instanceof SQLIdentifierExpr) {
            String ownerA = ((SQLIdentifierExpr) a.getOwner()).getName();
            String ownerB = ((SQLIdentifierExpr) b.getOwner()).getName();
            // Alias usually stays the same
            if (!ownerA.equalsIgnoreCase(ownerB) && !ownerA.equalsIgnoreCase(ctx.stripSourceQuotes(ownerB))) {
                // May be table name difference, ignore
            }
        }
    }
    
    /**
     * Compare table.column vs column (one has alias, one doesn't).
     * This happens when converter strips table alias from SET clause.
     */
    private void comparePropertyVsIdentifier(SQLPropertyExpr prop, SQLIdentifierExpr ident, String path) {
        String propCol = prop.getName();
        String identCol = ident.getName();
        
        // Compare column names
        if (!ctx.isEquivalentIdentifier(propCol, identCol, true)) {
            throw new AssertionError(path + ": Column mismatch - '" + propCol + "' vs '" + identCol + "'");
        }
        // Table alias from property is ignored - valid conversion pattern
    }
    
    // ========== Operator Comparison ==========
    
    private void compareBinaryOp(SQLBinaryOpExpr a, SQLBinaryOpExpr b, String path) {
        if (a.getOperator() != b.getOperator()) {
            throw new AssertionError(path + ": Operator mismatch - " + a.getOperator() + " vs " + b.getOperator());
        }
        compare(a.getLeft(), b.getLeft(), path + ".left");
        compare(a.getRight(), b.getRight(), path + ".right");
    }
    
    private void compareInList(SQLInListExpr a, SQLInListExpr b, String path) {
        compare(a.getExpr(), b.getExpr(), path + ".expr");
        compareExprList(a.getTargetList(), b.getTargetList(), path + ".values");
    }
    
    private void compareInSubQuery(SQLInSubQueryExpr a, SQLInSubQueryExpr b, String path) {
        // Compare the expression before IN
        compare(a.getExpr(), b.getExpr(), path + ".expr");
        // Compare the subquery by comparing the SQLSelectQuery
        SQLSelect selectA = a.getSubQuery();
        SQLSelect selectB = b.getSubQuery();
        if (selectA == null && selectB == null) return;
        if (selectA == null || selectB == null) {
            throw new AssertionError(path + ": One subquery is null");
        }
        // Compare the select query
        compareSelectQuery(selectA.getQuery(), selectB.getQuery(), path + ".subquery");
    }
    
    private void compareSelectQuery(SQLSelectQuery a, SQLSelectQuery b, String path) {
        if (a == null && b == null) return;
        if (a == null || b == null) {
            throw new AssertionError(path + ": One query is null");
        }
        
        if (a instanceof SQLSelectQueryBlock && b instanceof SQLSelectQueryBlock) {
            compareSelectQueryBlock((SQLSelectQueryBlock) a, (SQLSelectQueryBlock) b, path);
        } else {
            // Fallback to string comparison for complex queries (union, etc.)
            if (!a.toString().equalsIgnoreCase(b.toString())) {
                throw new AssertionError(path + ": Query mismatch - '" + a + "' vs '" + b + "'");
            }
        }
    }
    
    private void compareSelectQueryBlock(SQLSelectQueryBlock a, SQLSelectQueryBlock b, String path) {
        // Compare FROM
        if (a.getFrom() != null || b.getFrom() != null) {
            compareTableSource(a.getFrom(), b.getFrom(), path + ".from");
        }
        
        // Compare SELECT list
        List<SQLSelectItem> selectListA = a.getSelectList();
        List<SQLSelectItem> selectListB = b.getSelectList();
        if (selectListA.size() != selectListB.size()) {
            throw new AssertionError(path + ": Select list size mismatch - " + selectListA.size() + " vs " + selectListB.size());
        }
        for (int i = 0; i < selectListA.size(); i++) {
            compareSelectItem(selectListA.get(i), selectListB.get(i), path + ".select[" + i + "]");
        }
        
        // Compare WHERE
        if (a.getWhere() != null || b.getWhere() != null) {
            compare(a.getWhere(), b.getWhere(), path + ".where");
        }
    }
    
    private void compareTableSource(SQLTableSource a, SQLTableSource b, String path) {
        if (a == null && b == null) return;
        if (a == null || b == null) {
            throw new AssertionError(path + ": One table source is null");
        }
        
        if (a instanceof SQLExprTableSource && b instanceof SQLExprTableSource) {
            SQLExprTableSource tableA = (SQLExprTableSource) a;
            SQLExprTableSource tableB = (SQLExprTableSource) b;
            compare(tableA.getExpr(), tableB.getExpr(), path);
        } else {
            // Fallback for joins, subqueries, etc.
            if (!a.toString().equalsIgnoreCase(b.toString())) {
                throw new AssertionError(path + ": Table source mismatch - '" + a + "' vs '" + b + "'");
            }
        }
    }
    
    private void compareSelectItem(SQLSelectItem a, SQLSelectItem b, String path) {
        // Compare expression
        compare(a.getExpr(), b.getExpr(), path);
        // Compare alias (if present)
        String aliasA = a.getAlias();
        String aliasB = b.getAlias();
        if (aliasA != null || aliasB != null) {
            if (aliasA == null || aliasB == null) {
                // One has alias, other doesn't - this is usually fine
            } else {
                String normAliasA = ctx.stripSourceQuotes(aliasA);
                String normAliasB = ctx.stripTargetQuotes(aliasB);
                if (!normAliasA.equalsIgnoreCase(normAliasB)) {
                    throw new AssertionError(path + ".alias: Alias mismatch - '" + aliasA + "' vs '" + aliasB + "'");
                }
            }
        }
    }
    
    private void compareBetween(SQLBetweenExpr a, SQLBetweenExpr b, String path) {
        compare(a.getTestExpr(), b.getTestExpr(), path + ".test");
        compare(a.getBeginExpr(), b.getBeginExpr(), path + ".begin");
        compare(a.getEndExpr(), b.getEndExpr(), path + ".end");
    }
    
    // ========== Helper Methods ==========
    
    public void compareExprList(List<SQLExpr> a, List<SQLExpr> b, String path) {
        if (a == null && b == null) return;
        if (a == null || b == null) {
            throw new AssertionError(path + ": One list is null");
        }
        if (a.size() != b.size()) {
            throw new AssertionError(path + ": List size mismatch - " + a.size() + " vs " + b.size());
        }
        for (int i = 0; i < a.size(); i++) {
            compare(a.get(i), b.get(i), path + "[" + i + "]");
        }
    }
}
