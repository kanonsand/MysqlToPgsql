package org.omono.converter.comparator;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.omono.converter.comparator.context.CompareContext;
import org.omono.converter.comparator.stmt.*;

import java.util.Arrays;
import java.util.List;

/**
 * Main entry point for SQL AST comparison.
 * Routes to specific statement comparators based on statement type.
 */
public class SqlAstComparator {
    
    private static final List<AbstractStatementComparator<?>> COMPARATORS = Arrays.asList(
        new SelectComparator(null, null),
        new InsertComparator(null, null),
        new UpdateComparator(null, null),
        new DeleteComparator(null, null)
    );
    
    /**
     * Assert that SQL from two different database dialects are structurally equivalent.
     *
     * @param sourceSql    the source SQL string
     * @param targetSql    the target SQL string
     * @param sourceDbType source database type
     * @param targetDbType target database type
     * @param ctx          the comparison context
     * @throws AssertionError if the SQL structures are not equivalent
     */
    public static void assertEquivalent(String sourceSql, String targetSql,
                                         DbType sourceDbType, DbType targetDbType,
                                         CompareContext ctx) {
        List<SQLStatement> stmtsA = SQLUtils.parseStatements(sourceSql, sourceDbType);
        List<SQLStatement> stmtsB = SQLUtils.parseStatements(targetSql, targetDbType);
        
        if (stmtsA.size() != stmtsB.size()) {
            throw new AssertionError("Statement count mismatch: " + stmtsA.size() + " vs " + stmtsB.size());
        }
        
        for (int i = 0; i < stmtsA.size(); i++) {
            compareStatement(stmtsA.get(i), stmtsB.get(i), ctx, "stmt[" + i + "]");
        }
    }
    
    /**
     * Assert that SQL from two different database dialects are structurally equivalent,
     * using a context auto-configured from the database types.
     *
     * @param sourceSql    the source SQL string
     * @param targetSql    the target SQL string
     * @param sourceDbType source database type
     * @param targetDbType target database type
     * @throws AssertionError if the SQL structures are not equivalent
     */
    public static void assertEquivalent(String sourceSql, String targetSql,
                                         DbType sourceDbType, DbType targetDbType) {
        CompareContext ctx = CompareContext.forDbTypes(sourceDbType, targetDbType);
        assertEquivalent(sourceSql, targetSql, sourceDbType, targetDbType, ctx);
    }
    
    /**
     * Assert that MySQL and PostgreSQL SQL are structurally equivalent.
     *
     * @param mysql the MySQL SQL string
     * @param pgsql the PostgreSQL SQL string
     * @param ctx   the comparison context
     * @throws AssertionError if the SQL structures are not equivalent
     */
    public static void assertEquivalent(String mysql, String pgsql, CompareContext ctx) {
        assertEquivalent(mysql, pgsql, DbType.mysql, DbType.postgresql, ctx);
    }
    
    /**
     * Assert that MySQL and PostgreSQL SQL are structurally equivalent
     * using default context (MySQL → PostgreSQL).
     */
    public static void assertEquivalent(String mysql, String pgsql) {
        assertEquivalent(mysql, pgsql, DbType.mysql, DbType.postgresql);
    }
    
    private static void compareStatement(SQLStatement a, SQLStatement b,
                                         CompareContext ctx, String path) {
        for (AbstractStatementComparator<?> comparator : COMPARATORS) {
            if (comparator.supports(a) && comparator.supports(b)) {
                comparator.withContext(ctx, path).compareUnchecked(a, b);
                return;
            }
        }
        
        // Fallback: generic string comparison for unknown statement types
        String sqlA = a.toString();
        String sqlB = b.toString();
        if (!sqlA.equalsIgnoreCase(sqlB)) {
            throw new AssertionError(path + ": SQL mismatch - '" + sqlA + "' vs '" + sqlB + "'");
        }
    }
}
