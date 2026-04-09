package org.omono.converter.mybatis;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.omono.converter.comparator.SqlAstComparator;
import org.omono.converter.comparator.context.CompareContext;
import org.omono.converter.mybatis.rewrite.DeleteRewriteHandler;
import org.omono.converter.mybatis.rewrite.InsertRewriteHandler;
import org.omono.converter.mybatis.rewrite.SelectRewriteHandler;
import org.omono.converter.mybatis.rewrite.UpdateRewriteHandler;
import org.omono.converter.schema.ColumnDefinition;
import org.omono.converter.schema.TableMapping;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive integration test for SQL rewrite system.
 * Uses sql-comparator to verify MySQL -> PostgreSQL conversion results.
 * 
 * Test flow:
 * 1. Original MySQL SQL
 * 2. Convert using mybatistool's rewrite handlers
 * 3. Verify using sql-comparator (compare original MySQL with converted PostgreSQL)
 * 
 * MySQL -> PostgreSQL column mapping:
 * - user.user_name -> user.name
 * - user.is_active -> user.active (TINYINT -> BOOLEAN)
 * - order.order_no -> order.order_number
 */
public class SqlRewriteIntegrationTest {
    
    private TableMapping userTableMapping;
    private TableMapping orderTableMapping;
    private CompareContext userCompareContext;
    private CompareContext orderCompareContext;
    private CompareContext joinCompareContext;
    
    @Before
    public void setUp() {
        // Create table mappings
        userTableMapping = createUserTableMapping();
        orderTableMapping = createOrderTableMapping();
        
        // Create CompareContext for user table tests
        userCompareContext = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .sourceQuotes(CompareContext.MYSQL_QUOTES)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .withDefaultTypeMatchers()
            .columnMapping("user_name", "name")
            .columnMapping("is_active", "active")
            .tableMapping("user", "user")
            .build();
        
        // Create CompareContext for order table tests
        orderCompareContext = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .sourceQuotes(CompareContext.MYSQL_QUOTES)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .columnMapping("order_no", "order_number")
            .tableMapping("order", "order")
            .build();
        
        // Create CompareContext for JOIN tests
        joinCompareContext = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .sourceQuotes(CompareContext.MYSQL_QUOTES)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .withDefaultTypeMatchers()
            .columnMapping("user_name", "name")
            .columnMapping("is_active", "active")
            .columnMapping("order_no", "order_number")
            .build();
    }
    
    private TableMapping createUserTableMapping() {
        TableMapping mapping = new TableMapping("user");
        
        List<ColumnDefinition> mysqlCols = new ArrayList<>();
        mysqlCols.add(createCol("id", "BIGINT", 0));
        mysqlCols.add(createCol("user_name", "VARCHAR(100)", 1));
        mysqlCols.add(createCol("email", "VARCHAR(200)", 2));
        mysqlCols.add(createCol("status", "INT", 3));
        mysqlCols.add(createCol("created_at", "DATETIME", 4));
        mysqlCols.add(createCol("is_active", "TINYINT(1)", 5));
        mapping.setMysqlColumns(mysqlCols);
        
        List<ColumnDefinition> pgCols = new ArrayList<>();
        pgCols.add(createCol("id", "BIGINT", 0));
        pgCols.add(createCol("name", "VARCHAR(100)", 1));
        pgCols.add(createCol("email", "VARCHAR(200)", 2));
        pgCols.add(createCol("status", "INTEGER", 3));
        pgCols.add(createCol("created_at", "TIMESTAMP", 4));
        pgCols.add(createCol("active", "BOOLEAN", 5));
        mapping.setPgColumns(pgCols);
        
        mapping.addColumnMapping("user_name", "name");
        mapping.addColumnMapping("is_active", "active");
        
        return mapping;
    }
    
    private TableMapping createOrderTableMapping() {
        TableMapping mapping = new TableMapping("order");
        
        List<ColumnDefinition> mysqlCols = new ArrayList<>();
        mysqlCols.add(createCol("id", "BIGINT", 0));
        mysqlCols.add(createCol("user_id", "BIGINT", 1));
        mysqlCols.add(createCol("order_no", "VARCHAR(50)", 2));
        mysqlCols.add(createCol("amount", "DECIMAL(10,2)", 3));
        mysqlCols.add(createCol("status", "VARCHAR(20)", 4));
        mysqlCols.add(createCol("created_time", "DATETIME", 5));
        mapping.setMysqlColumns(mysqlCols);
        
        List<ColumnDefinition> pgCols = new ArrayList<>();
        pgCols.add(createCol("id", "BIGINT", 0));
        pgCols.add(createCol("user_id", "BIGINT", 1));
        pgCols.add(createCol("order_number", "VARCHAR(50)", 2));
        pgCols.add(createCol("amount", "DECIMAL(10,2)", 3));
        pgCols.add(createCol("status", "VARCHAR(20)", 4));
        pgCols.add(createCol("created_time", "TIMESTAMP", 5));
        mapping.setPgColumns(pgCols);
        
        mapping.addColumnMapping("order_no", "order_number");
        
        return mapping;
    }
    
    private ColumnDefinition createCol(String name, String type, int ordinal) {
        ColumnDefinition col = new ColumnDefinition();
        col.setName(name);
        col.setType(type);
        col.setOrdinal(ordinal);
        return col;
    }
    
    // ==================== SELECT Tests ====================
    
    @Test
    public void testSelectSimple() {
        String mysql = "SELECT id, user_name, email FROM user WHERE id = 1";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testSelectWithAlias() {
        String mysql = "SELECT u.id, u.user_name, u.email FROM user u WHERE u.id = 1";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testSelectWithJoin() {
        String mysql = "SELECT u.user_name, o.order_no, o.amount " +
                       "FROM user u JOIN `order` o ON u.id = o.user_id " +
                       "WHERE u.status = 1";
        
        Map<String, TableMapping> mappings = new HashMap<>();
        mappings.put("user", userTableMapping);
        mappings.put("order", orderTableMapping);
        mappings.put("u", userTableMapping);
        mappings.put("o", orderTableMapping);
        
        String converted = convertSelectMulti(mysql, mappings);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, joinCompareContext);
    }
    
    @Test
    public void testSelectWithWhereConditions() {
        String mysql = "SELECT * FROM user WHERE status = 1 AND is_active = 1 AND user_name = 'Alice'";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testSelectWithOrderByAndLimit() {
        String mysql = "SELECT id, user_name FROM user ORDER BY created_at DESC LIMIT 10";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testSelectWithGroupByAndHaving() {
        String mysql = "SELECT user_id, COUNT(*) as cnt FROM `order` " +
                       "GROUP BY user_id HAVING COUNT(*) > 1";
        String converted = convertSelect(mysql, orderTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, orderCompareContext);
    }
    
    @Test
    public void testSelectWithBooleanColumn() {
        String mysql = "SELECT * FROM user WHERE is_active = 1";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    // ==================== INSERT Tests ====================
    
    @Test
    public void testInsertSimple() {
        String mysql = "INSERT INTO user (id, user_name, email, status, is_active) " +
                       "VALUES (1, 'Alice', 'alice@test.com', 1, 1)";
        String converted = convertInsert(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testInsertWithReservedWordTable() {
        String mysql = "INSERT INTO `order` (id, user_id, order_no, amount) " +
                       "VALUES (1, 1, 'ORD001', 100.00)";
        String converted = convertInsert(mysql, orderTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, orderCompareContext);
    }
    
    // ==================== UPDATE Tests ====================
    
    @Test
    public void testUpdateSimple() {
        String mysql = "UPDATE user SET user_name = 'Bob', status = 2 WHERE id = 1";
        String converted = convertUpdate(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testUpdateWithBooleanColumn() {
        String mysql = "UPDATE user SET is_active = 0 WHERE id = 1";
        String converted = convertUpdate(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testUpdateWithAlias() {
        String mysql = "UPDATE user u SET u.status = 3 WHERE u.id = 1";
        String converted = convertUpdate(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testUpdateOrderTable() {
        String mysql = "UPDATE `order` SET order_no = 'ORD002', status = 'completed' WHERE id = 1";
        String converted = convertUpdate(mysql, orderTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, orderCompareContext);
    }
    
    // ==================== DELETE Tests ====================
    
    @Test
    public void testDeleteSimple() {
        String mysql = "DELETE FROM user WHERE id = 1";
        String converted = convertDelete(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testDeleteWithCondition() {
        String mysql = "DELETE FROM user WHERE status = 0 AND is_active = 0";
        String converted = convertDelete(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testDeleteFromOrder() {
        String mysql = "DELETE FROM `order` WHERE user_id = 1 AND order_no = 'ORD001'";
        String converted = convertDelete(mysql, orderTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, orderCompareContext);
    }
    
    // ==================== Complex Query Tests ====================
    
    @Test
    public void testComplexSelectWithSubquery() {
        String mysql = "SELECT * FROM user WHERE id IN " +
                       "(SELECT user_id FROM `order` WHERE amount > 100)";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, joinCompareContext);
    }
    
    @Test
    public void testSelectWithBetween() {
        String mysql = "SELECT * FROM `order` WHERE amount BETWEEN 50 AND 200";
        String converted = convertSelect(mysql, orderTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, orderCompareContext);
    }
    
    @Test
    public void testSelectWithInClause() {
        String mysql = "SELECT * FROM user WHERE status IN (1, 2, 3)";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testSelectWithLike() {
        String mysql = "SELECT * FROM user WHERE user_name LIKE '%Ali%'";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testSelectWithNotNull() {
        String mysql = "SELECT * FROM user WHERE email IS NOT NULL";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testSelectCount() {
        String mysql = "SELECT COUNT(*) FROM user WHERE is_active = 1";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testSelectDistinct() {
        String mysql = "SELECT DISTINCT user_name FROM user";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    @Test
    public void testLeftJoin() {
        String mysql = "SELECT u.user_name, o.order_no " +
                       "FROM user u LEFT JOIN `order` o ON u.id = o.user_id";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, joinCompareContext);
    }
    
    @Test
    public void testSelectWithMultipleAliases() {
        String mysql = "SELECT u.id as uid, u.user_name as uname, u.is_active as active_flag " +
                       "FROM user u WHERE u.status = 1";
        String converted = convertSelect(mysql, userTableMapping);
        
        System.out.println("MySQL:     " + mysql);
        System.out.println("Converted: " + converted);
        
        assertEquivalent(mysql, converted, userCompareContext);
    }
    
    // ==================== Helper Methods ====================
    
    private void assertEquivalent(String mysql, String converted, CompareContext ctx) {
        try {
            SqlAstComparator.assertEquivalent(mysql, converted, ctx);
        } catch (AssertionError e) {
            System.err.println("MySQL:     " + mysql);
            System.err.println("Converted: " + converted);
            throw e;
        }
    }
    
    private String convertSelect(String mysql, TableMapping mapping) {
        SelectRewriteHandler handler = new SelectRewriteHandler();
        List<SQLStatement> statements = SQLUtils.parseStatements(mysql, DbType.mysql);
        SQLStatement stmt = statements.get(0);
        
        assertTrue("Should be SELECT statement", handler.supports(stmt));
        
        SqlAnalysisResult analysis = handler.analyze(stmt);
        ConversionContext context = new ConversionContext();
        handler.convert(stmt, analysis, mapping, context);
        
        return stmt.toString();
    }
    
    private String convertSelectMulti(String mysql, Map<String, TableMapping> mappings) {
        SelectRewriteHandler handler = new SelectRewriteHandler();
        List<SQLStatement> statements = SQLUtils.parseStatements(mysql, DbType.mysql);
        SQLStatement stmt = statements.get(0);
        
        assertTrue("Should be SELECT statement", handler.supports(stmt));
        
        SqlAnalysisResult analysis = handler.analyze(stmt);
        ConversionContext context = new ConversionContext();
        handler.convertMulti(stmt, analysis, mappings, context);
        
        return stmt.toString();
    }
    
    private String convertInsert(String mysql, TableMapping mapping) {
        InsertRewriteHandler handler = new InsertRewriteHandler();
        List<SQLStatement> statements = SQLUtils.parseStatements(mysql, DbType.mysql);
        SQLStatement stmt = statements.get(0);
        
        assertTrue("Should be INSERT statement", handler.supports(stmt));
        
        SqlAnalysisResult analysis = handler.analyze(stmt);
        ConversionContext context = new ConversionContext();
        handler.convert(stmt, analysis, mapping, context);
        
        return stmt.toString();
    }
    
    private String convertUpdate(String mysql, TableMapping mapping) {
        UpdateRewriteHandler handler = new UpdateRewriteHandler();
        List<SQLStatement> statements = SQLUtils.parseStatements(mysql, DbType.mysql);
        SQLStatement stmt = statements.get(0);
        
        assertTrue("Should be UPDATE statement", handler.supports(stmt));
        
        SqlAnalysisResult analysis = handler.analyze(stmt);
        ConversionContext context = new ConversionContext();
        handler.convert(stmt, analysis, mapping, context);
        
        return stmt.toString();
    }
    
    private String convertDelete(String mysql, TableMapping mapping) {
        DeleteRewriteHandler handler = new DeleteRewriteHandler();
        List<SQLStatement> statements = SQLUtils.parseStatements(mysql, DbType.mysql);
        SQLStatement stmt = statements.get(0);
        
        assertTrue("Should be DELETE statement", handler.supports(stmt));
        
        SqlAnalysisResult analysis = handler.analyze(stmt);
        ConversionContext context = new ConversionContext();
        handler.convert(stmt, analysis, mapping, context);
        
        return stmt.toString();
    }
}