package org.omono.converter.comparator.stmt;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import org.omono.converter.comparator.context.CompareContext;

import java.util.List;

/**
 * Abstract base class for statement comparators.
 * Provides shared helper methods for comparing common SQL elements.
 *
 * @param <T> the type of SQL statement this comparator handles
 */
public abstract class AbstractStatementComparator<T extends SQLStatement> {
    
    protected final CompareContext ctx;
    protected final String path;
    
    public AbstractStatementComparator(CompareContext ctx, String path) {
        this.ctx = ctx;
        this.path = path;
    }
    
    /**
     * Create a new instance with the given context and path.
     * Used for factory pattern in SqlAstComparator.
     */
    public abstract AbstractStatementComparator<T> withContext(CompareContext ctx, String path);
    
    /**
     * Check if this comparator supports the given statement type.
     */
    public abstract boolean supports(SQLStatement stmt);
    
    /**
     * Compare two statements. Called after supports() check passes.
     */
    @SuppressWarnings("unchecked")
    public void compareUnchecked(SQLStatement a, SQLStatement b) {
        compare((T) a, (T) b);
    }
    
    /**
     * Compare two statements of the supported type.
     */
    protected abstract void compare(T a, T b);
    
    // ========== Shared Helper Methods ==========
    
    protected void compareTableName(SQLExpr a, SQLExpr b, String subPath) {
        String nameA = extractName(a);
        String nameB = extractName(b);
        
        if (nameA == null || nameB == null) {
            throw new AssertionError(fullPath(subPath) + ": Cannot extract table name");
        }
        
        if (!ctx.isEquivalentIdentifier(nameA, nameB, false)) {
            throw new AssertionError(fullPath(subPath) + ": Table name mismatch - '" + nameA + "' vs '" + nameB + "'");
        }
    }
    
    protected String extractName(SQLExpr expr) {
        if (expr instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) expr).getName();
        }
        if (expr instanceof SQLPropertyExpr) {
            return ((SQLPropertyExpr) expr).getName();
        }
        return expr.toString();
    }
    
    protected void compareExprList(List<SQLExpr> a, List<SQLExpr> b, String subPath) {
        if (a == null && b == null) return;
        if (a == null || b == null) {
            throw new AssertionError(fullPath(subPath) + ": One list is null");
        }
        if (a.size() != b.size()) {
            throw new AssertionError(fullPath(subPath) + ": List size mismatch - " + a.size() + " vs " + b.size());
        }
        for (int i = 0; i < a.size(); i++) {
            compareExpr(a.get(i), b.get(i), subPath + "[" + i + "]");
        }
    }
    
    protected void compareString(String a, String b, String subPath) {
        if (a == null && b == null) return;
        if (a == null || b == null) {
            throw new AssertionError(fullPath(subPath) + ": String mismatch - '" + a + "' vs '" + b + "'");
        }
        String cleanA = ctx.stripSourceQuotes(a);
        String cleanB = ctx.stripTargetQuotes(b);
        if (!cleanA.equalsIgnoreCase(cleanB)) {
            throw new AssertionError(fullPath(subPath) + ": String mismatch - '" + a + "' vs '" + b + "'");
        }
    }
    
    protected void assertTypeMatch(Object a, Object b, Class<?> expected, String subPath) {
        if (!expected.isInstance(a) || !expected.isInstance(b)) {
            throw new AssertionError(fullPath(subPath) + ": Type mismatch - expected " + expected.getSimpleName()
                + " but got " + a.getClass().getSimpleName() + " and " + b.getClass().getSimpleName());
        }
    }
    
    protected void fail(String subPath, String message) {
        throw new AssertionError(fullPath(subPath) + ": " + message);
    }
    
    protected String fullPath(String subPath) {
        return path + subPath;
    }
    
    // Abstract method to be implemented by subclasses for expression comparison
    protected abstract void compareExpr(SQLExpr a, SQLExpr b, String subPath);
}
